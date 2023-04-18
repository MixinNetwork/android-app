package one.mixin.android.ui.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
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
import one.mixin.android.extension.createAtToLong
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.toUtcTime
import one.mixin.android.session.Session
import one.mixin.android.ui.transfer.TransferProtocol.Companion.TYPE_COMMAND
import one.mixin.android.ui.transfer.TransferProtocol.Companion.TYPE_JSON
import one.mixin.android.ui.transfer.vo.TransferCommandAction
import one.mixin.android.ui.transfer.vo.TransferCommandData
import one.mixin.android.ui.transfer.vo.TransferData
import one.mixin.android.ui.transfer.vo.TransferDataType
import one.mixin.android.ui.transfer.vo.TransferSendData
import one.mixin.android.ui.transfer.vo.TransferStatus
import one.mixin.android.ui.transfer.vo.TransferStatusLiveData
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.NetworkUtils
import one.mixin.android.util.SINGLE_SOCKET_THREAD
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.net.BindException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.random.Random

class TransferServer @Inject internal constructor(
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
    val status: TransferStatusLiveData,
) {

    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null

    private var quit = false

    private val gson by lazy {
        GsonHelper.customGson
    }

    private var code = 0
    private var port = 0

    private var count = 0L
    private var total = 0L

    suspend fun startServer(
        createdSuccessCallback: (TransferCommandData) -> Unit,
    ) = withContext(SINGLE_SOCKET_THREAD) {
        try {
            val serverSocket = createSocket(port = Random.nextInt(1024, 1124))
            this@TransferServer.serverSocket = serverSocket
            status.value = TransferStatus.CREATED
            code = Random.nextInt(10000)
            createdSuccessCallback(
                TransferCommandData(
                    TransferCommandAction.PUSH.value,
                    NetworkUtils.getWifiIpAddress(MixinApplication.appContext),
                    this@TransferServer.port,
                    null,
                    this@TransferServer.code,
                    userId = Session.getAccountId(),
                ),
            )
            status.value = TransferStatus.WAITING_FOR_CONNECTION
            val socket = withContext(ACCEPT_SINGLE_THREAD) {
                serverSocket.accept()
            }
            this@TransferServer.socket = socket
            status.value = TransferStatus.WAITING_FOR_VERIFICATION

            val remoteAddr = socket.remoteSocketAddress
            if (remoteAddr is InetSocketAddress) {
                val inetAddr = remoteAddr.address
                val ip = inetAddr.hostAddress
                Timber.e("Connected to $ip")
                run(socket.getInputStream(), socket.getOutputStream())
            } else {
                exit()
            }
        } catch (e: Exception) {
            if (status.value != TransferStatus.FINISHED && !(status.value == TransferStatus.INITIALIZING && e is SocketException)) {
                status.value = TransferStatus.ERROR
            }
            exit()
            Timber.e(e)
        }
    }

    private fun createSocket(port: Int): ServerSocket {
        var newPort = port
        while (!isPortAvailable(newPort)) {
            newPort++
            if (newPort >= 65535) {
                throw RuntimeException("No available port found")
            }
        }
        this.port = newPort
        return ServerSocket(newPort)
    }

    private fun isPortAvailable(port: Int): Boolean {
        if (port in 1024..49151) { // Exclude common ports
            try {
                ServerSocket(port).use { return true }
            } catch (e: BindException) {
                return false
            }
        } else {
            return false
        }
    }

    private suspend fun run(inputStream: InputStream, outputStream: OutputStream) =
        withContext(Dispatchers.IO) {
            do {
                when (val result = protocol.read(inputStream)) {
                    is String -> {
                        val transferData = gson.fromJson(result, TransferData::class.java)
                        if (transferData.type == TransferDataType.COMMAND.value) {
                            Timber.e("command $result")
                            val commandData =
                                gson.fromJson(transferData.data, TransferCommandData::class.java)
                            if (commandData.action == TransferCommandAction.CONNECT.value) {
                                if (commandData.code == code && commandData.userId == Session.getAccountId()) {
                                    Timber.e("Verification passed, start transmission")
                                    status.value = TransferStatus.VERIFICATION_COMPLETED
                                    launch {
                                        transfer(outputStream)
                                    }
                                } else {
                                    Timber.e("Validation failed, close")
                                    status.value = TransferStatus.ERROR
                                    exit()
                                }
                            } else if (commandData.action == TransferCommandAction.FINISH.value) {
                                RxBus.publish(DeviceTransferProgressEvent(100f))
                                status.value = TransferStatus.FINISHED
                                exit()
                            } else if (commandData.action == TransferCommandAction.PROGRESS.value) {
                                // Get progress from client
                                if (commandData.progress != null) {
                                    RxBus.publish(DeviceTransferProgressEvent(commandData.progress))
                                }
                            } else {
                                Timber.e("Unsupported command")
                            }
                        }
                    }
                    else -> {
                        // File ByteArray null
                        // do noting
                    }
                }
            } while (!quit)
        }

    fun transfer(outputStream: OutputStream) {
        status.value = TransferStatus.SYNCING
        sendStart(outputStream)
        syncConversation(outputStream)
        syncParticipant(outputStream)
        syncUser(outputStream)
        syncAsset(outputStream)
        syncSnapshot(outputStream)
        syncSticker(outputStream)
        syncPinMessage(outputStream)
        syncTranscriptMessage(outputStream)
        syncMessage(outputStream)
        syncExpiredMessage(outputStream)
        syncMediaFile(outputStream)
        sendFinish(outputStream)
    }

    private fun writeJson(
        outputStream: OutputStream,
        transferData: Any,
    ) {
        val content = gson.toJson(transferData)
        protocol.write(outputStream, TYPE_JSON, content)
        outputStream.flush()
    }

    private fun writeCommand(
        outputStream: OutputStream,
        transferData: TransferSendData<TransferCommandData>,
    ) {
        val content = gson.toJson(transferData)
        protocol.write(outputStream, TYPE_COMMAND, content)
        outputStream.flush()
    }

    private fun sendStart(outputStream: OutputStream) {
        writeCommand(
            outputStream,
            TransferSendData(
                TransferDataType.COMMAND.value,
                TransferCommandData(TransferCommandAction.START.value, total = totalCount()),
            ),
        )
        RxBus.publish(DeviceTransferProgressEvent(0.0f))
    }

    private fun totalCount(): Long {
        this.total =
            messageDao.countMediaMessages() + messageDao.countMessages() + conversationDao.countConversations() +
            expiredMessageDao.countExpiredMessages() + participantDao.countParticipants() +
            pinMessageDao.countPinMessages() + snapshotDao.countSnapshots() + stickerDao.countStickers() +
            transcriptMessageDao.countTranscriptMessages() + userDao.countUsers()
        return total
    }

    private fun sendFinish(outputStream: OutputStream) {
        Timber.e("send finish")
        writeCommand(
            outputStream,
            TransferSendData(
                TransferDataType.COMMAND.value,
                TransferCommandData(TransferCommandAction.FINISH.value),
            ),
        )
    }

    private fun syncConversation(outputStream: OutputStream) {
        var offset = 0
        while (!quit) {
            val list = conversationDao.getConversationsByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.CONVERSATION.value, it)
            }.forEach {
                Timber.e("send conversation ${it.data.conversationId}")
                writeJson(outputStream, it)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncParticipant(outputStream: OutputStream) {
        var offset = 0
        while (!quit) {
            val list = participantDao.getParticipantsByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.PARTICIPANT.value, it)
            }.forEach {
                Timber.e("send Participant ${it.data.conversationId} ${it.data.userId}")
                writeJson(outputStream, it)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncUser(outputStream: OutputStream) {
        var offset = 0
        while (!quit) {
            val list = userDao.getUsersByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.USER.value, it)
            }.forEach {
                Timber.e("send user ${it.data.userId}")
                writeJson(outputStream, it)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncAsset(outputStream: OutputStream) {
        var offset = 0
        while (!quit) {
            val list = assetDao.getAssetByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.ASSET.value, it)
            }.forEach {
                Timber.e("send asset ${it.data.assetId}")
                writeJson(outputStream, it)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncSticker(outputStream: OutputStream) {
        var offset = 0
        while (!quit) {
            val list = stickerDao.getStickersByLimitAndOffset(LIMIT, offset)
                .map {
                    it.lastUseAt = it.lastUseAt?.let { lastUseAt ->
                        try {
                            lastUseAt.toLong().toUtcTime()
                        } catch (e: Exception) {
                            Timber.e(e)
                            lastUseAt
                        }
                    } ?: it.lastUseAt
                    it
                }
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.STICKER.value, it)
            }.forEach {
                Timber.e("send sticker ${it.data.stickerId}")
                writeJson(outputStream, it)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncSnapshot(outputStream: OutputStream) {
        var offset = 0
        while (!quit) {
            val list = snapshotDao.getSnapshotByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.SNAPSHOT.value, it)
            }.forEach {
                writeJson(outputStream, it)
                Timber.e("send snapshot ${it.data.snapshotId}")
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncTranscriptMessage(outputStream: OutputStream) {
        var offset = 0
        while (!quit) {
            val list = transcriptMessageDao.getTranscriptMessageByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.TRANSCRIPT_MESSAGE.value, it)
            }.forEach {
                writeJson(outputStream, it)
                Timber.e("send transcript ${it.data.transcriptId}")
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncPinMessage(outputStream: OutputStream) {
        var offset = 0
        while (!quit) {
            val list = pinMessageDao.getPinMessageByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.PIN_MESSAGE.value, it)
            }.forEach {
                writeJson(outputStream, it)
                count++

                Timber.e("send pin message: ${it.data.messageId}")
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncMessage(outputStream: OutputStream) {
        var offset = 0
        while (!quit) {
            val list = messageDao.getMessageByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.MESSAGE.value, it)
            }.forEach {
                writeJson(outputStream, it)
                Timber.e("send message: ${it.data.messageId}")
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncExpiredMessage(outputStream: OutputStream) {
        var offset = 0
        while (!quit) {
            val list = expiredMessageDao.getExpiredMessageByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.EXPIRED_MESSAGE.value, it)
            }.forEach {
                writeJson(outputStream, it)
                Timber.e("send pin message: ${it.data.messageId}")
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncMediaFile(outputStream: OutputStream) {
        val context = MixinApplication.get()
        val folder = context.getMediaPath() ?: return
        folder.walkTopDown().forEach { f ->
            if (f.isFile && f.length() > 0) {
                val name = f.nameWithoutExtension
                if (name.isUUID()) {
                    if (f.parentFile.name == "Transcripts" && transcriptMessageDao.countTranscriptByMessageId(name) > 0) {
                        protocol.write(outputStream, f, name)
                        count++
                    } else if (messageDao.findMessageById(name) != null) {
                        protocol.write(outputStream, f, name)
                        count++
                    } else {
                        // skip and delete not held file
                        f.delete()
                    }
                }
            }
        }
    }

    val protocol = TransferProtocol()

    fun exit() = MixinApplication.get().applicationScope.launch(SINGLE_SOCKET_THREAD) {
        try {
            quit = true
            socket?.close()
            socket = null
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Timber.e("exit server ${e.message}")
        }
    }

    companion object {
        private const val LIMIT = 100
    }
}

private val ACCEPT_SINGLE_THREAD = Executors.newSingleThreadExecutor { r -> Thread(r, "SINGLE_DB_EXECUTOR") }.asCoroutineDispatcher()
