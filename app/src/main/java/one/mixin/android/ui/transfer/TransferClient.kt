package one.mixin.android.ui.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import one.mixin.android.ui.transfer.vo.TransferCommandAction
import one.mixin.android.ui.transfer.vo.TransferCommandData
import one.mixin.android.ui.transfer.vo.TransferData
import one.mixin.android.ui.transfer.vo.TransferDataType
import one.mixin.android.ui.transfer.vo.TransferMessage
import one.mixin.android.ui.transfer.vo.TransferSendData
import one.mixin.android.ui.transfer.vo.TransferStatus
import one.mixin.android.ui.transfer.vo.TransferStatusLiveData
import one.mixin.android.ui.transfer.vo.toMessage
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.SINGLE_SOCKET_THREAD
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.Participant
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.User
import timber.log.Timber
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Float.min
import java.net.Socket
import javax.inject.Inject

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

    private val gson by lazy {
        GsonHelper.customGson
    }

    private var count = 0L

    val protocol = TransferProtocol()

    suspend fun connectToServer(ip: String, port: Int, commandData: TransferCommandData) =
        withContext(SINGLE_SOCKET_THREAD) {
            try {
                status.value = TransferStatus.CONNECTING
                val socket = Socket(ip, port)
                this@TransferClient.socket = socket
                status.value = TransferStatus.WAITING_FOR_VERIFICATION
                val outputStream = socket.getOutputStream()
                protocol.write(outputStream, TransferProtocol.TYPE_COMMAND, gson.toJson(TransferSendData(TransferDataType.COMMAND.value, commandData)))
                outputStream.flush()
                listen(socket.inputStream, socket.outputStream)
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
            status.value = TransferStatus.SENDING
            val (type, result) = try {
                protocol.read(inputStream)
            } catch (e: EOFException) {
                Pair(null, null)
            }
            if (type == TransferProtocol.TYPE_FILE) {
                // read file
                progress(outputStream)
            } else if (type == TransferProtocol.TYPE_COMMAND || type == TransferProtocol.TYPE_JSON) {
                val content = result as String
                Timber.e("sync $content")
                val transferData = gson.fromJson(content, TransferData::class.java)
                when (transferData.type) {
                    TransferDataType.COMMAND.value -> {
                        val transferCommandData =
                            gson.fromJson(transferData.data, TransferCommandData::class.java)
                        if (transferCommandData.action == TransferCommandAction.START.value) {
                            if (transferCommandData.version > 1) {
                                Timber.e("Version does not support")
                                exit()
                                return
                            }
                            this.total = transferCommandData.total ?: 0L
                        } else if (transferCommandData.action == TransferCommandAction.PUSH.value && transferCommandData.action == TransferCommandAction.PULL.value && transferCommandData.action == TransferCommandAction.START.value) {
                            Timber.e("action ${transferCommandData.action}")
                        } else if (transferCommandData.action == TransferCommandAction.FINISH.value) {
                            status.value = TransferStatus.FINISHED
                            sendFinish(outputStream)
                            delay(100)
                            exit()
                        } else {
                            Timber.e(content)
                        }
                    }

                    TransferDataType.MESSAGE.value -> {
                        syncInsert {
                            val message =
                                gson.fromJson(transferData.data, TransferMessage::class.java)
                                    .toMessage()

                            messageDao.insertIgnore(message)
                            ftsDatabase.insertOrReplaceMessageFts4(message)
                            Timber.e("Message ID: ${message.messageId}")
                        }
                        progress(outputStream)
                    }

                    TransferDataType.PARTICIPANT.value -> {
                        syncInsert {
                            val participant =
                                gson.fromJson(transferData.data, Participant::class.java)
                            participantDao.insertIgnore(participant)
                            Timber.e("Participant ID: ${participant.conversationId} ${participant.userId}")
                        }
                        progress(outputStream)
                    }

                    TransferDataType.USER.value -> {
                        syncInsert {
                            val user = gson.fromJson(transferData.data, User::class.java)
                            userDao.insertIgnore(user)
                            Timber.e("User ID: ${user.userId}")
                        }
                        progress(outputStream)
                    }

                    TransferDataType.CONVERSATION.value -> {
                        syncInsert {
                            val conversation =
                                gson.fromJson(transferData.data, Conversation::class.java)
                            conversationDao.insertIgnore(conversation)
                            Timber.e("Conversation ID: ${conversation.conversationId}")
                        }
                        progress(outputStream)
                    }

                    TransferDataType.SNAPSHOT.value -> {
                        syncInsert {
                            val snapshot = gson.fromJson(transferData.data, Snapshot::class.java)
                            Timber.e("Snapshot ID: ${snapshot.snapshotId}")
                            progress(outputStream)
                        }
                    }

                    TransferDataType.STICKER.value -> {
                        syncInsert {
                            val sticker = gson.fromJson(transferData.data, Sticker::class.java)
                            stickerDao.insertIgnore(sticker)
                            Timber.e("Sticker ID: ${sticker.stickerId}")
                        }
                        progress(outputStream)
                    }

                    TransferDataType.ASSET.value -> {
                        syncInsert {
                            val asset = gson.fromJson(transferData.data, Asset::class.java)
                            assetDao.insertIgnore(asset)
                            Timber.e("Asset ID: ${asset.assetId}")
                        }
                        progress(outputStream)
                    }

                    TransferDataType.PIN_MESSAGE.value -> {
                        syncInsert {
                            val pinMessage =
                                gson.fromJson(transferData.data, PinMessage::class.java)
                            pinMessageDao.insertIgnore(pinMessage)
                            Timber.e("PinMessage ID: ${pinMessage.messageId}")
                        }
                        progress(outputStream)
                    }

                    TransferDataType.TRANSCRIPT_MESSAGE.value -> {
                        syncInsert {
                            val transcriptMessage =
                                gson.fromJson(transferData.data, TranscriptMessage::class.java)
                            transcriptMessageDao.insertIgnore(transcriptMessage)
                            Timber.e("Transcript ID: ${transcriptMessage.messageId}")
                        }
                        progress(outputStream)
                    }

                    else -> {
                        Timber.e("No support $content")
                    }
                }
            } else {
                // do noting
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
        val content = gson.toJson(transferSendData)
        protocol.write(outputStream, TransferProtocol.TYPE_COMMAND, content)
        outputStream.flush()
    }

    private suspend fun syncInsert(callback: () -> Unit) = withContext(Dispatchers.IO) {
        launch {
            callback.invoke()
        }
    }
}
