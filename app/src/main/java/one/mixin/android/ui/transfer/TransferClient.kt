package one.mixin.android.ui.transfer

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.db.AppDao
import one.mixin.android.db.AssetDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ConversationExtDao
import one.mixin.android.db.ExpiredMessageDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MessageMentionDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.PinMessageDao
import one.mixin.android.db.RemoteMessageStatusDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.TranscriptMessageDao
import one.mixin.android.db.UserDao
import one.mixin.android.event.DeviceTransferProgressEvent
import one.mixin.android.extension.createAtToLong
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.fts.insertOrReplaceMessageFts4
import one.mixin.android.ui.transfer.vo.CURRENT_TRANSFER_VERSION
import one.mixin.android.ui.transfer.vo.TransferCommandAction
import one.mixin.android.ui.transfer.vo.TransferCommandData
import one.mixin.android.ui.transfer.vo.TransferDataType
import one.mixin.android.ui.transfer.vo.TransferMessageMention
import one.mixin.android.ui.transfer.vo.TransferSendData
import one.mixin.android.ui.transfer.vo.TransferStatus
import one.mixin.android.ui.transfer.vo.TransferStatusLiveData
import one.mixin.android.util.SINGLE_SOCKET_THREAD
import one.mixin.android.util.mention.parseMentionData
import one.mixin.android.vo.App
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ExpiredMessage
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageMention
import one.mixin.android.vo.Participant
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.User
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Float.min
import java.net.Socket
import java.net.SocketException
import javax.inject.Inject

class TransferClient @Inject internal constructor(
    val assetDao: AssetDao,
    val conversationDao: ConversationDao,
    val conversationExtDao: ConversationExtDao,
    val expiredMessageDao: ExpiredMessageDao,
    val messageDao: MessageDao,
    val participantDao: ParticipantDao,
    val pinMessageDao: PinMessageDao,
    val snapshotDao: SnapshotDao,
    val stickerDao: StickerDao,
    val transcriptMessageDao: TranscriptMessageDao,
    val userDao: UserDao,
    val appDao: AppDao,
    val messageMentionDao: MessageMentionDao,
    val remoteMessageStatusDao: RemoteMessageStatusDao,
    val ftsDatabase: FtsDatabase,
    val status: TransferStatusLiveData,
    val gson: Gson,
    private val serializationJson: Json,
) {

    private var socket: Socket? = null
    private var quit = false
    private var count = 0L
    private var startTime = 0L

    val protocol = TransferProtocol()

    private val syncChannel = Channel<ByteArray>()

    suspend fun connectToServer(ip: String, port: Int, commandData: TransferCommandData) =
        withContext(SINGLE_SOCKET_THREAD) {
            try {
                status.value = TransferStatus.CONNECTING
                val socket = Socket(ip, port)
                this@TransferClient.socket = socket
                status.value = TransferStatus.WAITING_FOR_VERIFICATION
                val outputStream = socket.getOutputStream()
                protocol.write(outputStream, TransferProtocol.TYPE_COMMAND, gson.toJson(commandData))
                outputStream.flush()
                launch(Dispatchers.IO) { listen(socket.inputStream, socket.outputStream) }
                launch(Dispatchers.IO) {
                    for (byteArray in syncChannel) {
                        processJson(byteArray)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                if (status.value != TransferStatus.FINISHED && status.value != TransferStatus.ERROR) {
                    status.value = TransferStatus.ERROR
                }
                exit()
            }
        }

    private var total = 0L

    private suspend fun listen(inputStream: InputStream, outputStream: OutputStream) {
        do {
            status.value = TransferStatus.SYNCING
            val result = try {
                protocol.read(inputStream)
            } catch (e: EOFException) {
                null
            }
            when (result) {
                is TransferCommandData -> {
                    when (result.action) {
                        TransferCommandAction.START.value -> {
                            if (result.version != CURRENT_TRANSFER_VERSION) {
                                Timber.e("Version does not support")
                                exit()
                                return
                            }
                            startTime = System.currentTimeMillis()
                            this.total = result.total ?: 0L
                        }

                        TransferCommandAction.PUSH.value, TransferCommandAction.PULL.value -> {
                            Timber.e("action ${result.action}")
                        }

                        TransferCommandAction.FINISH.value -> {
                            status.value = TransferStatus.FINISHED
                            sendFinish(outputStream)
                            delay(100)
                            exit()
                            Timber.e("It takes a total of ${System.currentTimeMillis() - startTime} milliseconds to synchronize ${this.total} data")
                        }

                        else -> {
                        }
                    }
                }
                is ByteArray -> {
                    syncChannel.send(result)
                    progress(outputStream)
                }
                else -> {
                    // read file
                    progress(outputStream)
                }
            }
        } while (!quit)
    }

    private var lastTime = 0L
    private val runtime by lazy {
        Runtime.getRuntime()
    }

    private var lastProgress = 0f
    private fun progress(outputStream: OutputStream) {
        if (quit || total <= 0 || status.value == TransferStatus.ERROR) return
        val progress = min((count++) / total.toFloat() * 100, 100f)
        if (lastProgress != progress && System.currentTimeMillis() - lastTime > 300) {
            sendCommand(
                outputStream,
                TransferCommandData(TransferCommandAction.PROGRESS.value, progress = progress),
            )
            lastProgress = progress
            lastTime = System.currentTimeMillis()
            RxBus.publish(DeviceTransferProgressEvent(progress))
            Timber.e("Device transfer $progress")
        }
    }

    private val mutableList:MutableList<Message> = mutableListOf()

    private fun processJson(byteArray: ByteArray) {
        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        val transferData = serializationJson.decodeFromStream<TransferSendData<JsonElement>>(byteArrayInputStream)
        when (transferData.type) {
            TransferDataType.CONVERSATION.value -> {
                val conversation = serializationJson.decodeFromJsonElement<Conversation>(transferData.data)
                conversationDao.insertIgnore(conversation)
            }

            TransferDataType.PARTICIPANT.value -> {
                val participant = serializationJson.decodeFromJsonElement<Participant>(transferData.data)
                participantDao.insertIgnore(participant)
            }

            TransferDataType.USER.value -> {
                val user = serializationJson.decodeFromJsonElement<User>(transferData.data)
                userDao.insertIgnore(user)
            }

            TransferDataType.APP.value -> {
                val app = serializationJson.decodeFromJsonElement<App>(transferData.data)
                appDao.insertIgnore(app)
            }

            TransferDataType.ASSET.value -> {
                val asset = serializationJson.decodeFromJsonElement<Asset>(transferData.data)
                assetDao.insertIgnore(asset)
            }

            TransferDataType.SNAPSHOT.value -> {
                val snapshot = serializationJson.decodeFromJsonElement<Snapshot>(transferData.data)
                snapshotDao.insertIgnore(snapshot)
            }

            TransferDataType.STICKER.value -> {
                val sticker = serializationJson.decodeFromJsonElement<Sticker>(transferData.data)
                sticker.lastUseAt?.let {
                    try {
                        sticker.lastUseAt = it.createAtToLong().toString()
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
                stickerDao.insertIgnore(sticker)
            }

            TransferDataType.PIN_MESSAGE.value -> {
                val pinMessage = serializationJson.decodeFromJsonElement<PinMessage>(transferData.data)
                pinMessageDao.insertIgnore(pinMessage)
            }

            TransferDataType.TRANSCRIPT_MESSAGE.value -> {
                val transcriptMessage = serializationJson.decodeFromJsonElement<TranscriptMessage>(transferData.data)
                transcriptMessageDao.insertIgnore(transcriptMessage)
            }

            TransferDataType.MESSAGE.value -> {
                val message = serializationJson.decodeFromJsonElement<Message>(transferData.data)
                if (messageDao.findMessageIdById(message.messageId) == null) {
                    mutableList.add(message)
                    if (mutableList.size >= 1000) {
                        messageDao.insertList(mutableList)
                        mutableList.clear()
                    }
                    ftsDatabase.insertOrReplaceMessageFts4(message)
                }
            }

            TransferDataType.MESSAGE_MENTION.value -> {
                if (mutableList.isNotEmpty()) {
                    messageDao.insertList(mutableList)
                    mutableList.clear()
                }
                val messageMention =
                    serializationJson.decodeFromJsonElement<TransferMessageMention>(transferData.data).let {
                        val messageContent =
                            messageDao.findMessageContentById(it.conversationId, it.messageId)
                                ?: return
                        val mentionData = parseMentionData(messageContent, userDao) ?: return
                        MessageMention(it.messageId, it.conversationId, mentionData, it.hasRead)
                    }
                messageMentionDao.insertIgnoreReturn(messageMention)
            }

            TransferDataType.EXPIRED_MESSAGE.value -> {
                if (mutableList.isNotEmpty()) {
                    messageDao.insertList(mutableList)
                    mutableList.clear()
                }
                val expiredMessage =
                    serializationJson.decodeFromJsonElement<ExpiredMessage>(transferData.data)
                expiredMessageDao.insertIgnore(expiredMessage)
            }

            else -> {
                Timber.e("No support ${transferData.type}")
            }
        }
        byteArrayInputStream.close()
    }

    private fun finalWork() {
        if (mutableList.isNotEmpty()) {
            messageDao.insertList(mutableList)
        }
        conversationDao.getAllConversationId().forEach { conversationId ->
            conversationDao.refreshLastMessageId(conversationId)
            remoteMessageStatusDao.updateConversationUnseen(conversationId)
        }
        conversationExtDao.getAllConversationId().forEach { conversationId ->
            conversationExtDao.refreshCountByConversationId(conversationId)
        }
        mutableList.clear()
    }

    fun exit() = MixinApplication.get().applicationScope.launch(SINGLE_SOCKET_THREAD) {
        try {
            if (socket != null) {
                quit = true
                socket?.close()
                socket = null
            }
            finalWork()
        } catch (e: Exception) {
            Timber.e("Exit client ${e.message}")
        }
    }

    private fun sendFinish(outputStream: OutputStream) {
        sendCommand(
            outputStream,
            TransferCommandData(TransferCommandAction.FINISH.value),
        )
    }

    private fun sendCommand(
        outputStream: OutputStream,
        transferSendData: TransferCommandData,
    ) {
        val content = gson.toJson(transferSendData)
        try {
            protocol.write(outputStream, TransferProtocol.TYPE_COMMAND, content)
            outputStream.flush()
        } catch (e: SocketException) {
            exit()
            if (status.value != TransferStatus.FINISHED && status.value != TransferStatus.ERROR) {
                status.value = TransferStatus.ERROR
            }
        }
    }
}
