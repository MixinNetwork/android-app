package one.mixin.android.ui.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.db.AssetDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ExpiredMessageDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.PinMessageDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.TranscriptMessageDao
import one.mixin.android.db.UserDao
import one.mixin.android.event.DeviceTransferProgressEvent
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.fts.insertOrReplaceMessageFts4
import one.mixin.android.ui.transfer.vo.CURRENT_TRANSFER_VERSION
import one.mixin.android.ui.transfer.vo.TransferCommandAction
import one.mixin.android.ui.transfer.vo.TransferCommandData
import one.mixin.android.ui.transfer.vo.TransferDataType
import one.mixin.android.ui.transfer.vo.TransferSendData
import one.mixin.android.ui.transfer.vo.TransferStatus
import one.mixin.android.ui.transfer.vo.TransferStatusLiveData
import one.mixin.android.util.SINGLE_SOCKET_THREAD
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ExpiredMessage
import one.mixin.android.vo.Message
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
    val expiredMessageDao: ExpiredMessageDao,
    val messageDao: MessageDao,
    val participantDao: ParticipantDao,
    val pinMessageDao: PinMessageDao,
    val snapshotDao: SnapshotDao,
    val stickerDao: StickerDao,
    val transcriptMessageDao: TranscriptMessageDao,
    val userDao: UserDao,
    val ftsDatabase: FtsDatabase,
    val status: TransferStatusLiveData,
) {

    private var socket: Socket? = null
    private var quit = false

    fun isAvailable() = socket != null

    private var count = 0L

    val protocol = TransferProtocol()

    private val syncChannel = Channel<ByteArray>()

    private val json by lazy {
        Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }
    }

    private var startTime = 0L

    suspend fun connectToServer(ip: String, port: Int, commandData: TransferCommandData) =
        withContext(SINGLE_SOCKET_THREAD) {
            try {
                status.value = TransferStatus.CONNECTING
                val socket = Socket(ip, port)
                this@TransferClient.socket = socket
                status.value = TransferStatus.WAITING_FOR_VERIFICATION
                val outputStream = socket.getOutputStream()
                sendCommand(outputStream, commandData)
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
                    val transferCommandData: TransferCommandData = json.decodeFromString(result)
                    when (transferCommandData.action) {
                        TransferCommandAction.START.value -> {
                            if (transferCommandData.version != CURRENT_TRANSFER_VERSION) {
                                Timber.e("Version does not support")
                                exit()
                                return
                            }
                            this.total = transferCommandData.total ?: 0L
                        }

                        TransferCommandAction.PUSH.value, TransferCommandAction.PULL.value -> {
                            Timber.e("action ${transferCommandData.action}")
                        }

                        TransferCommandAction.FINISH.value -> {
                            status.value = TransferStatus.FINISHED
                            sendFinish(outputStream)
                            delay(100)
                            exit()
                        }

                        else -> {
                            Timber.e(result)
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
    private fun progress(outputStream: OutputStream) {
        if (total <= 0) return
        val progress = min((count++) / total.toFloat() * 100, 100f)
        if (System.currentTimeMillis() - lastTime > 200) {
            sendCommand(
                outputStream,
                TransferCommandData(TransferCommandAction.PROGRESS.value, progress = progress),
            )
            lastTime = System.currentTimeMillis()
        }
        RxBus.publish(DeviceTransferProgressEvent(progress))
    }

    private suspend fun processJson(content: String, outputStream: OutputStream) {
        val transferData = json.decodeFromString<TransferSendData<JsonElement>>(content)
        when (transferData.type) {
            TransferDataType.CONVERSATION.value -> {
                    val conversation = json.decodeFromJsonElement<Conversation>(transferData.data)
                    conversationDao.insertIgnore(conversation)
                    Timber.e("Conversation ID: ${conversation.conversationId}")
                progress(outputStream)
            }

            TransferDataType.PARTICIPANT.value -> {
                    val participant = json.decodeFromJsonElement<Participant>(transferData.data)
                    participantDao.insertIgnore(participant)
                    Timber.e("Participant ID: ${participant.conversationId} ${participant.userId}")
                progress(outputStream)
            }

            TransferDataType.USER.value -> {
                    val user = json.decodeFromJsonElement<User>(transferData.data)
                    userDao.insertIgnore(user)
                    Timber.e("User ID: ${user.userId}")
                progress(outputStream)
            }

            TransferDataType.ASSET.value -> {
                    val asset = json.decodeFromJsonElement<Asset>(transferData.data)
                    assetDao.insertIgnore(asset)
                    Timber.e("Asset ID: ${asset.assetId}")
                progress(outputStream)
            }

            TransferDataType.SNAPSHOT.value -> {
                    val snapshot = json.decodeFromJsonElement<Snapshot>(transferData.data)
                    snapshotDao.insertIgnore(snapshot)
                    Timber.e("Snapshot ID: ${snapshot.snapshotId}")
                progress(outputStream)
            }

            TransferDataType.STICKER.value -> {
                    val sticker = json.decodeFromJsonElement<Sticker>(transferData.data)
                    stickerDao.insertIgnore(sticker)
                    Timber.e("Sticker ID: ${sticker.stickerId}")
                progress(outputStream)
            }

            TransferDataType.PIN_MESSAGE.value -> {
                    val pinMessage = json.decodeFromJsonElement<PinMessage>(transferData.data)
                    pinMessageDao.insertIgnore(pinMessage)
                    Timber.e("PinMessage ID: ${pinMessage.messageId}")
                progress(outputStream)
            }

            TransferDataType.TRANSCRIPT_MESSAGE.value -> {
                    val transcriptMessage = json.decodeFromJsonElement<TranscriptMessage>(transferData.data)
                    transcriptMessageDao.insertIgnore(transcriptMessage)
                    Timber.e("Transcript ID: ${transcriptMessage.messageId}")
                progress(outputStream)
            }

            TransferDataType.MESSAGE.value -> {
                    val message = json.decodeFromJsonElement<Message>(transferData.data)
                    messageDao.insertIgnore(message)
                    ftsDatabase.insertOrReplaceMessageFts4(message)
                    Timber.e("Message ID: ${message.messageId}")
                progress(outputStream)
            }

            TransferDataType.EXPIRED_MESSAGE.name -> {
                    val expiredMessage =
                        json.decodeFromJsonElement<ExpiredMessage>(transferData.data)
                    expiredMessageDao.insertIgnore(expiredMessage)
                    Timber.e("ExpiredMessage ID: ${expiredMessage.messageId}")
                progress(outputStream)
            }

            else -> {
                Timber.e("No support $content")
            }
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
        val content = json.encodeToString(transferSendData)
        try {
            protocol.write(outputStream, TransferProtocol.TYPE_COMMAND, content)
            outputStream.flush()
        } catch (e: SocketException) {
            exit()
            status.value = TransferStatus.ERROR
        }
    }

}
