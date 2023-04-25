package one.mixin.android.ui.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import one.mixin.android.ui.transfer.vo.TransferData
import one.mixin.android.ui.transfer.vo.TransferDataType
import one.mixin.android.ui.transfer.vo.TransferMessage
import one.mixin.android.ui.transfer.vo.TransferMessageMention
import one.mixin.android.ui.transfer.vo.TransferStatus
import one.mixin.android.ui.transfer.vo.TransferStatusLiveData
import one.mixin.android.ui.transfer.vo.toMessage
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.SINGLE_SOCKET_THREAD
import one.mixin.android.util.mention.parseMentionData
import one.mixin.android.vo.App
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ExpiredMessage
import one.mixin.android.vo.MessageMention
import one.mixin.android.vo.Participant
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.User
import timber.log.Timber
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.lang.Float.min
import java.net.Socket
import java.net.SocketException
import javax.inject.Inject
import kotlin.text.Charsets.UTF_8

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
    val ftsDatabase: FtsDatabase,
    val status: TransferStatusLiveData,
) {

    private var socket: Socket? = null
    private var quit = false

    fun isAvailable() = socket != null

    private val gson by lazy {
        GsonHelper.customGson
    }

    private var count = 0L

    private var receiveOffset = 0L

    val protocol = TransferProtocol().apply {
        setTransferCallback(object :TransferProtocol.TransferCallback{
            override suspend fun onTransferWrite(dataSize: Int): Boolean {
                return true
            }

            override fun onTransferRead(dataSize: Int) {
                receiveOffset += dataSize
                // do noting
            }
        })
    }

    private val syncChannel = Channel<ByteArray>()

    private var startTime = 0L

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
                        val content = String(byteArray, UTF_8)
                        processJson(content, outputStream)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                if (status.value != TransferStatus.FINISHED) {
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
                is String -> {
                    Timber.e("sync $result")
                    val transferCommandData = gson.fromJson(result, TransferCommandData::class.java)

                    when (transferCommandData.action) {
                        TransferCommandAction.START.value -> {
                            if (transferCommandData.version != CURRENT_TRANSFER_VERSION) {
                                Timber.e("Version does not support")
                                exit()
                                return
                            }
                            startTime = System.currentTimeMillis()
                            this.total = transferCommandData.total ?: 0L
                        }

                        TransferCommandAction.PUSH.value, TransferCommandAction.PULL.value -> {
                            Timber.e("action ${transferCommandData.action}")
                        }

                        TransferCommandAction.FINISH.value -> {
                            status.value = TransferStatus.FINISHED
                            finalWork()
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
                }
                is File -> {
                    // read file
                    progress(outputStream)
                }
                else -> {
                    // do noting
                }
            }
        } while (!quit)
    }

    private var lastTime = 0L
    private suspend fun progress(outputStream: OutputStream) {
        if (total <= 0) return
        val progress = min((count++) / total.toFloat() * 100, 100f)
        if (System.currentTimeMillis() - lastTime > 200) {
            sendCommand(
                outputStream,
                TransferCommandData(TransferCommandAction.PROGRESS.value, progress = progress, offset = receiveOffset),
            )
            lastTime = System.currentTimeMillis()
            Timber.e("Client transfer ${progress}% $receiveOffset")
        }
        RxBus.publish(DeviceTransferProgressEvent(progress))
    }

    private suspend fun processJson(content: String, outputStream: OutputStream) {
        val transferData = gson.fromJson(content, TransferData::class.java)
        when (transferData.type) {
            TransferDataType.CONVERSATION.value -> {
                val conversation =
                    gson.fromJson(transferData.data, Conversation::class.java)
                conversationDao.insertIgnore(conversation)
                progress(outputStream)
            }

            TransferDataType.PARTICIPANT.value -> {
                val participant = gson.fromJson(transferData.data, Participant::class.java)
                participantDao.insertIgnore(participant)
                progress(outputStream)
            }

            TransferDataType.USER.value -> {
                val user = gson.fromJson(transferData.data, User::class.java)
                userDao.insertIgnore(user)
                progress(outputStream)
            }

            TransferDataType.APP.value -> {
                val app = gson.fromJson(transferData.data, App::class.java)
                appDao.insertIgnore(app)
                progress(outputStream)
            }

            TransferDataType.ASSET.value -> {
                val asset = gson.fromJson(transferData.data, Asset::class.java)
                assetDao.insertIgnore(asset)
                progress(outputStream)
            }

            TransferDataType.SNAPSHOT.value -> {
                val snapshot = gson.fromJson(transferData.data, Snapshot::class.java)
                snapshotDao.insertIgnore(snapshot)
                progress(outputStream)
            }

            TransferDataType.STICKER.value -> {
                val sticker = gson.fromJson(transferData.data, Sticker::class.java)
                sticker.lastUseAt?.let {
                    try {
                        sticker.lastUseAt = it.createAtToLong()?.toString()
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
                stickerDao.insertIgnore(sticker)
                progress(outputStream)
            }

            TransferDataType.PIN_MESSAGE.value -> {
                val pinMessage =
                    gson.fromJson(transferData.data, PinMessage::class.java)
                pinMessageDao.insertIgnore(pinMessage)
                progress(outputStream)
            }

            TransferDataType.TRANSCRIPT_MESSAGE.value -> {
                val transcriptMessage =
                    gson.fromJson(transferData.data, TranscriptMessage::class.java)
                transcriptMessageDao.insertIgnore(transcriptMessage)
                progress(outputStream)
            }

            TransferDataType.MESSAGE.value -> {
                val message = gson.fromJson(transferData.data, TransferMessage::class.java).toMessage()
                val rowId = messageDao.insertIgnoreReturn(message)
                if (rowId != -1L) { // If the row ID is valid (-1 indicates insertion failure)
                    ftsDatabase.insertOrReplaceMessageFts4(message)
                }
                progress(outputStream)
            }

            TransferDataType.MESSAGE_MENTION.value -> {
                val messageMention = gson.fromJson(transferData.data, TransferMessageMention::class.java).let {
                    val messageContent = messageDao.findMessageContentById(it.conversationId, it.messageId) ?: return
                    val mentionData = parseMentionData(messageContent, userDao) ?: return
                    MessageMention(it.messageId, it.conversationId, mentionData, it.hasRead)
                }
                messageMentionDao.insertIgnoreReturn(messageMention)
                progress(outputStream)
            }

            TransferDataType.EXPIRED_MESSAGE.value -> {
                val expiredMessage =
                    gson.fromJson(transferData.data, ExpiredMessage::class.java)
                expiredMessageDao.insertIgnore(expiredMessage)
                progress(outputStream)
            }

            else -> {
                Timber.e("No support $content")
            }
        }
    }

    private fun finalWork() {
        conversationDao.getAllConversationId().forEach { conversationId ->
            conversationDao.refreshLastMessageId(conversationId)
        }
        conversationExtDao.getAllConversationId().forEach { conversationId ->
            conversationExtDao.refreshCountByConversationId(conversationId)
        }
    }

    fun exit() = MixinApplication.get().applicationScope.launch(SINGLE_SOCKET_THREAD) {
        try {
            if (socket != null) {
                quit = true
                socket?.close()
                socket = null
            }
        } catch (e: Exception) {
            Timber.e("exit client ${e.message}")
        }
    }

    private suspend fun sendFinish(outputStream: OutputStream) {
        sendCommand(
            outputStream,
            TransferCommandData(TransferCommandAction.FINISH.value, offset = receiveOffset),
        )
        Timber.e(" Client transfer finish $receiveOffset")
    }

    private suspend fun sendCommand(
        outputStream: OutputStream,
        transferSendData: TransferCommandData,
    ) {
        val content = gson.toJson(transferSendData)
        try {
            protocol.write(outputStream, TransferProtocol.TYPE_COMMAND, content)
            outputStream.flush()
        } catch (e: SocketException) {
            exit()
            status.value = TransferStatus.ERROR
        }
    }
}
