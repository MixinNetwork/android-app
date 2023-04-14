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

    suspend fun connectToServer(ip: String, port: Int, commandData: TransferCommandData) =
        withContext(SINGLE_SOCKET_THREAD) {
            try {
                status.value = TransferStatus.CONNECTING
                val socket = Socket(ip, port)
                this@TransferClient.socket = socket
                status.value = TransferStatus.WAITING_FOR_VERIFICATION
                val outputStream = socket.getOutputStream()
                protocol.write(outputStream, TransferProtocol.TYPE_COMMAND, Json.encodeToString(TransferSendData(TransferDataType.COMMAND.value, commandData)))
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
                    val transferData: TransferSendData<TransferCommandData> = Json.decodeFromString(result)
                    when (transferData.type) {
                        TransferDataType.COMMAND.value -> {
                            val transferCommandData = transferData.data
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

                        else -> {
                            Timber.e("No support $result")
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
                TransferSendData(
                    TransferDataType.COMMAND.value,
                    TransferCommandData(TransferCommandAction.PROGRESS.value, progress = progress),
                ),
            )
            lastTime = System.currentTimeMillis()
        }
        RxBus.publish(DeviceTransferProgressEvent(progress))
    }

    private suspend fun processJson(content: String, outputStream: OutputStream) {
        val transferData = Json.decodeFromString<TransferSendData<JsonElement>>(content)
        when (transferData.type) {
            TransferDataType.MESSAGE.value -> {
                syncInsert {
                    val message = Json{ignoreUnknownKeys = true}.decodeFromJsonElement<Message>(transferData.data)
                    messageDao.insertIgnore(message)
                    ftsDatabase.insertOrReplaceMessageFts4(message)
                    Timber.e("Message ID: ${message.messageId}")
                }
                progress(outputStream)
            }

            TransferDataType.PARTICIPANT.value -> {
                syncInsert {
                    val participant = Json{ignoreUnknownKeys = true}.decodeFromJsonElement<Participant>(transferData.data)
                    participantDao.insertIgnore(participant)
                    Timber.e("Participant ID: ${participant.conversationId} ${participant.userId}")
                }
                progress(outputStream)
            }

            TransferDataType.USER.value -> {
                syncInsert {
                    val user = Json{ignoreUnknownKeys = true}.decodeFromJsonElement<User>(transferData.data)
                    userDao.insertIgnore(user)
                    Timber.e("User ID: ${user.userId}")
                }
                progress(outputStream)
            }

            TransferDataType.CONVERSATION.value -> {
                syncInsert {
                    val conversation = Json{ignoreUnknownKeys = true}.decodeFromJsonElement<Conversation>(transferData.data)
                    conversationDao.insertIgnore(conversation)
                    Timber.e("Conversation ID: ${conversation.conversationId}")
                }
                progress(outputStream)
            }

            TransferDataType.SNAPSHOT.value -> {
                syncInsert {
                    val snapshot = Json{ignoreUnknownKeys = true}.decodeFromJsonElement<Snapshot>(transferData.data)
                    snapshotDao.insertIgnore(snapshot)
                    Timber.e("Snapshot ID: ${snapshot.snapshotId}")
                }
                progress(outputStream)
            }

            TransferDataType.STICKER.value -> {
                syncInsert {
                    val sticker = Json{ignoreUnknownKeys = true}.decodeFromJsonElement<Sticker>(transferData.data)
                    stickerDao.insertIgnore(sticker)
                    Timber.e("Sticker ID: ${sticker.stickerId}")
                }
                progress(outputStream)
            }

            TransferDataType.ASSET.value -> {
                syncInsert {
                    val asset = Json{ignoreUnknownKeys = true}.decodeFromJsonElement<Asset>(transferData.data)
                    assetDao.insertIgnore(asset)
                    Timber.e("Asset ID: ${asset.assetId}")
                }
                progress(outputStream)
            }

            TransferDataType.PIN_MESSAGE.value -> {
                syncInsert {
                    val pinMessage = Json{ignoreUnknownKeys = true}.decodeFromJsonElement<PinMessage>(transferData.data)
                    pinMessageDao.insertIgnore(pinMessage)
                    Timber.e("PinMessage ID: ${pinMessage.messageId}")
                }
                progress(outputStream)
            }

            TransferDataType.TRANSCRIPT_MESSAGE.value -> {
                syncInsert {
                    val transcriptMessage = Json{ignoreUnknownKeys = true}.decodeFromJsonElement<TranscriptMessage>(transferData.data)
                    transcriptMessageDao.insertIgnore(transcriptMessage)
                    Timber.e("Transcript ID: ${transcriptMessage.messageId}")
                }
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
            TransferSendData(
                TransferDataType.COMMAND.value,
                TransferCommandData(TransferCommandAction.FINISH.value),
            ),
        )
    }

    private fun sendCommand(
        outputStream: OutputStream,
        transferSendData: TransferSendData<TransferCommandData>,
    ) {
        val content = Json.encodeToString(transferSendData)
        try {
            protocol.write(outputStream, TransferProtocol.TYPE_COMMAND, content)
            outputStream.flush()
        } catch (e: SocketException) {
            exit()
            status.value = TransferStatus.ERROR
        }
    }

    private suspend fun syncInsert(callback: () -> Unit) = withContext(Dispatchers.IO) {
        callback.invoke()
    }
}
