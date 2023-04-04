package one.mixin.android.ui.transfer

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.api.ChecksumException
import one.mixin.android.crypto.generateAesKey
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
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.isUUID
import one.mixin.android.session.Session
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
import java.net.UnknownHostException
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
    ) =
        withContext(
            CoroutineExceptionHandler { _, exception ->
                when (exception) {
                    is UnknownHostException -> {
                    }

                    is SocketException -> {
                    }

                    is ChecksumException ->{
                    }

                    else -> {
                    }
                }
                status.value = TransferStatus.ERROR
                exit()
                Timber.e(exception)
            } + SINGLE_SOCKET_THREAD,
        ) {
            val serverSocket = createSocket(port = Random.nextInt(100))
            this@TransferServer.serverSocket = serverSocket
            status.value = TransferStatus.CREATED
            code = Random.nextInt(10000)
            createdSuccessCallback(
                TransferCommandData(
                    TransferCommandAction.PUSH.value,
                    NetworkUtils.getWifiIpAddress(MixinApplication.appContext),
                    this@TransferServer.port,
                    generateAesKey().base64RawURLEncode(), // todo
                    this@TransferServer.code,
                    userId = Session.getAccountId(),
                ),
            )
            status.value = TransferStatus.WAITING_FOR_CONNECTION
            val socket = serverSocket.accept()
            this@TransferServer.socket = socket
            status.value = TransferStatus.WAITING_FOR_VERIFICATION
            socket.soTimeout = 10000

            val remoteAddr = socket.remoteSocketAddress
            if (remoteAddr is InetSocketAddress) {
                val inetAddr = remoteAddr.address
                val ip = inetAddr.hostAddress
                Timber.e("Connected to $ip")
                run(socket.getInputStream(), socket.getOutputStream())
            } else {
                exit()
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
                val (content, _) = protocol.read(inputStream)
                if (content != null) {
                    val transferData = gson.fromJson(content, TransferData::class.java)
                    if (transferData.type == TransferDataType.COMMAND.value) {
                        Timber.e("command $content")
                        val commandData =
                            gson.fromJson(transferData.data, TransferCommandData::class.java)
                        if (commandData.action == TransferCommandAction.CONNECT.value) {
                            // Todo
                            // if (commandData.code == code && commandData.userId == Session.getAccountId()) {
                            if (commandData.code == code) {
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
                } else {
                    // do noting
                }
            } while (!quit)
        }

    fun transfer(outputStream: OutputStream) {
        status.value = TransferStatus.SENDING
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
        protocol.write(outputStream, content)
        outputStream.flush()
    }

    private fun sendStart(outputStream: OutputStream) {
        writeJson(
            outputStream,
            TransferSendData(
                TransferDataType.COMMAND.value,
                TransferCommandData(TransferCommandAction.START.value, total = totalCount()),
            ),
        )
    }

    private fun totalCount(): Long {
        this.total = messageDao.countMediaMessages() + messageDao.countMessages() + conversationDao.countConversations() +
            expiredMessageDao.countExpiredMessages() + participantDao.countParticipants() +
            pinMessageDao.countPinMessages() + snapshotDao.countSnapshots() + stickerDao.countStickers() +
            transcriptMessageDao.countTranscriptMessages() + userDao.countUsers()
        return total
    }

    private fun sendFinish(outputStream: OutputStream) {
        Timber.e("send finish")
        writeJson(
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
        var lastId = messageDao.getLastMessageRowId() ?: return
        while (!quit) {
            val messages = messageDao.findMessages(lastId, LIMIT)
            if (messages.isEmpty()) {
                return
            }
            messages.map {
                TransferSendData(TransferDataType.MESSAGE.value, it)
            }.forEach {
                Timber.e("send message: ${it.data.messageId}")
                writeJson(outputStream, it)
                count++
            }
            if (messages.size < LIMIT) {
                return
            }
            lastId = messageDao.getMessageRowid(messages.last().messageId) ?: return
        }
    }

    private fun syncExpiredMessage(outputStream: OutputStream) {
        var offset = 0
        while (!quit) {
            val list = expiredMessageDao.getExpiredMessageDaoByLimitAndOffset(LIMIT, offset)
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
                    if (messageDao.findMessageById(name) != null) {
                        protocol.write(outputStream, f, name)
                        count++
                    } else {
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
