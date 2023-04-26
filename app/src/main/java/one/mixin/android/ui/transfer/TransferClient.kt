package one.mixin.android.ui.transfer

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.transfer.vo.CURRENT_TRANSFER_VERSION
import one.mixin.android.ui.transfer.vo.TransferCommandAction
import one.mixin.android.ui.transfer.vo.TransferCommandData
import one.mixin.android.ui.transfer.vo.TransferStatus
import one.mixin.android.ui.transfer.vo.TransferStatusLiveData
import one.mixin.android.util.SINGLE_SOCKET_THREAD
import one.mixin.android.util.SINGLE_TRANSFER_PROGRESS_THREAD
import timber.log.Timber
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Float.min
import java.net.Socket
import java.net.SocketException
import javax.inject.Inject

class TransferClient @Inject internal constructor(
    val status: TransferStatusLiveData,
    private val serializationJson: Json,
    val context: Application,
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
    val jobManager: MixinJobManager,
) {

    private var socket: Socket? = null
    private var quit = false

    private lateinit var flashMan: FlashMan

    private var count = 0L

    private var receiveOffset = 0L

    val protocol = TransferProtocol(false).apply {
        setTransferCallback(object : TransferProtocol.TransferCallback {
            override suspend fun onTransferWrite(dataSize: Int) {
            }

            override fun onTransferRead(dataSize: Int) {
                receiveOffset += dataSize
                MixinApplication.get().applicationScope.launch(Dispatchers.IO) {
                    socket?.getOutputStream()?.let {
                        launch(SINGLE_TRANSFER_PROGRESS_THREAD) {
                            progress(it)
                        }
                    }
                }
                // do noting
            }
        })
    }

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
                launch {
                    listen(socket.inputStream, socket.outputStream)
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
                protocol.read(
                    inputStream,
                    if (::flashMan.isInitialized) {
                        flashMan
                    } else {
                        null
                    },
                )
            } catch (e: EOFException) {
                null
            }
            when (result) {
                is String -> {
                    val transferCommandData: TransferCommandData = serializationJson.decodeFromString(result)
                    when (transferCommandData.action) {
                        TransferCommandAction.START.value -> {
                            if (transferCommandData.version != CURRENT_TRANSFER_VERSION) {
                                Timber.e("Version does not support")
                                exit()
                                return
                            }
                            flashMan = FlashMan(transferCommandData.deviceId, context, assetDao, conversationDao, conversationExtDao, expiredMessageDao, messageDao, participantDao, pinMessageDao, snapshotDao, stickerDao, transcriptMessageDao, userDao, appDao, messageMentionDao, ftsDatabase, jobManager, serializationJson)
                            this.total = transferCommandData.total ?: 0L
                        }

                        TransferCommandAction.PUSH.value, TransferCommandAction.PULL.value -> {
                            Timber.e("action ${transferCommandData.action}")
                        }

                        TransferCommandAction.FINISH.value -> {
                            status.value = TransferStatus.PARSING
                            sendFinish(outputStream)
                            flashMan.finish(status)
                            delay(100)
                            exit()
                        }

                        else -> {
                            Timber.e(result)
                        }
                    }
                }

                is ByteArray -> {
                    count++
                    flashMan.writeBytes(result)
                }

                else -> {
                    count++
                    // do noting
                }
            }
        } while (!quit)
    }

    private var lastTime = 0L
    private suspend fun progress(outputStream: OutputStream) {
        if (total <= 0) return
        val progress = min(count / total.toFloat() * 100, 100f)
        if (System.currentTimeMillis() - lastTime > 200) {
            sendCommand(
                outputStream,
                TransferCommandData(TransferCommandAction.PROGRESS.value, progress = progress, offset = receiveOffset),
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
        val content = serializationJson.encodeToString(transferSendData)
        try {
            protocol.write(outputStream, TransferProtocol.TYPE_COMMAND, content)
            outputStream.flush()
        } catch (e: SocketException) {
            exit()
            status.value = TransferStatus.ERROR
        }
    }
}
