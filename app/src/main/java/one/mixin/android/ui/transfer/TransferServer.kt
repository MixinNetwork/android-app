package one.mixin.android.ui.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.db.AppDao
import one.mixin.android.db.AssetDao
import one.mixin.android.db.ConversationDao
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
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.toUtcTime
import one.mixin.android.session.Session
import one.mixin.android.ui.transfer.TransferProtocol.Companion.TYPE_COMMAND
import one.mixin.android.ui.transfer.TransferProtocol.Companion.TYPE_JSON
import one.mixin.android.ui.transfer.status.TransferStatus
import one.mixin.android.ui.transfer.status.TransferStatusLiveData
import one.mixin.android.ui.transfer.vo.TransferCommand
import one.mixin.android.ui.transfer.vo.TransferCommandAction
import one.mixin.android.ui.transfer.vo.TransferData
import one.mixin.android.ui.transfer.vo.TransferDataType
import one.mixin.android.ui.transfer.vo.compatible.TransferMessage
import one.mixin.android.ui.transfer.vo.compatible.TransferMessageMention
import one.mixin.android.util.NetworkUtils
import one.mixin.android.util.SINGLE_SOCKET_THREAD
import one.mixin.android.vo.App
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ExpiredMessage
import one.mixin.android.vo.Participant
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.User
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

@ExperimentalSerializationApi
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
    val appDao: AppDao,
    val messageMentionDao: MessageMentionDao,
    val status: TransferStatusLiveData,
    private val serializationJson: Json,
) {
    val protocol = TransferProtocol(serializationJson, true)

    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null

    private var quit = false

    private var code = 0
    private var port = 0

    private var count = 0L
    private var total = 0L
    private var currentType: String? = null
        set(value) {
            if (field != value) {
                field = value
                Timber.e("Current type: $field")
            }
        }

    suspend fun startServer(
        createdSuccessCallback: (TransferCommand) -> Unit,
    ) = withContext(SINGLE_SOCKET_THREAD) {
        try {
            val serverSocket = createSocket(port = Random.nextInt(1024, 1124))
            this@TransferServer.serverSocket = serverSocket
            status.value = TransferStatus.CREATED
            code = Random.nextInt(10000)
            createdSuccessCallback(
                TransferCommand(
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
                    is TransferCommand -> {
                        if (result.action == TransferCommandAction.CONNECT.value) {
                            if (result.code == code && result.userId == Session.getAccountId()) {
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
                        } else if (result.action == TransferCommandAction.FINISH.value) {
                            RxBus.publish(DeviceTransferProgressEvent(100f))
                            status.value = TransferStatus.FINISHED
                            exit()
                        } else if (result.action == TransferCommandAction.PROGRESS.value) {
                            // Get progress from client
                            if (result.progress != null) {
                                RxBus.publish(DeviceTransferProgressEvent(result.progress))
                            }
                        } else {
                            Timber.e("Unsupported command")
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
        syncApp(outputStream)
        syncAsset(outputStream)
        syncSnapshot(outputStream)
        syncSticker(outputStream)
        syncPinMessage(outputStream)
        syncTranscriptMessage(outputStream)
        syncMessage(outputStream)
        syncMessageMention(outputStream)
        syncExpiredMessage(outputStream)
        syncMediaFile(outputStream)
        sendFinish(outputStream)
    }

    private fun <T> writeJson(
        outputStream: OutputStream,
        serializer: SerializationStrategy<T>,
        transferData: T,
        type: Byte = TYPE_JSON,
    ) {
        protocol.write(outputStream, type, serializationJson.encodeToString(serializer, transferData))
        outputStream.flush()
    }

    private fun writeCommand(
        outputStream: OutputStream,
        command: TransferCommand,
    ) {
        writeJson(outputStream, TransferCommand.serializer(), command, TYPE_COMMAND)
    }

    private fun sendStart(outputStream: OutputStream) {
        writeCommand(outputStream, TransferCommand(TransferCommandAction.START.value, total = totalCount()))
        RxBus.publish(DeviceTransferProgressEvent(0.0f))
        Timber.e("Started total: $total")
    }

    private fun totalCount(): Long {
        this.total =
            messageDao.countMediaMessages() + messageDao.countMessages() + conversationDao.countConversations() +
            expiredMessageDao.countExpiredMessages() + participantDao.countParticipants() +
            pinMessageDao.countPinMessages() + snapshotDao.countSnapshots() + stickerDao.countStickers() +
            transcriptMessageDao.countTranscriptMessages() + userDao.countUsers() + appDao.countApps() + messageMentionDao.countMessageMention()
        return total
    }

    private fun sendFinish(outputStream: OutputStream) {
        writeCommand(
            outputStream,
            TransferCommand(TransferCommandAction.FINISH.value),
        )
    }

    private fun syncConversation(outputStream: OutputStream) {
        currentType = TransferDataType.CONVERSATION.value
        var rowId = -1L
        while (!quit) {
            val list = conversationDao.getConversationsByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.CONVERSATION.value, it)
            }.forEach { conversation ->
                writeJson(outputStream, TransferData.serializer(Conversation.serializer()), conversation)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = conversationDao.getConversationRowId(list.last().conversationId) ?: return
        }
    }

    private fun syncParticipant(outputStream: OutputStream) {
        currentType = TransferDataType.PARTICIPANT.value
        var rowId = -1L
        while (!quit) {
            val list = participantDao.getParticipantsByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.PARTICIPANT.value, it)
            }.forEach { participant ->
                writeJson(outputStream, TransferData.serializer(Participant.serializer()), participant)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = participantDao.getParticipantRowId(list.last().conversationId, list.last().userId) ?: return
        }
    }

    private fun syncUser(outputStream: OutputStream) {
        currentType = TransferDataType.USER.value
        var rowId = -1L
        while (!quit) {
            val list = userDao.getUsersByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.USER.value, it)
            }.forEach { user ->
                writeJson(outputStream, TransferData.serializer(User.serializer()), user)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = userDao.getUserRowId(list.last().userId) ?: return
        }
    }

    private fun syncApp(outputStream: OutputStream) {
        currentType = TransferDataType.APP.value
        var rowId = -1L
        while (!quit) {
            val list = appDao.getAppsByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.APP.value, it)
            }.forEach { app ->
                writeJson(outputStream, TransferData.serializer(App.serializer()), app)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = appDao.getAppRowId(list.last().appId) ?: return
        }
    }

    private fun syncAsset(outputStream: OutputStream) {
        currentType = TransferDataType.ASSET.value
        var rowId = -1L
        while (!quit) {
            val list = assetDao.getAssetByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.ASSET.value, it)
            }.forEach { asset ->
                writeJson(outputStream, TransferData.serializer(Asset.serializer()), asset)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = assetDao.getAssetRowId(list.last().assetId) ?: return
        }
    }

    private fun syncSticker(outputStream: OutputStream) {
        currentType = TransferDataType.STICKER.value
        var rowId = -1L
        while (!quit) {
            val list = stickerDao.getStickersByLimitAndRowId(LIMIT, rowId)
                .map {
                    it.lastUseAt = it.lastUseAt?.let { lastUseAt ->
                        try {
                            lastUseAt.toLong().toUtcTime()
                        } catch (e: Exception) {
                            Timber.e(e)
                            lastUseAt
                        }
                    }
                    it
                }
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.STICKER.value, it)
            }.forEach { sticker ->
                writeJson(outputStream, TransferData.serializer(Sticker.serializer()), sticker)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = stickerDao.getStickerRowId(list.last().stickerId) ?: return
        }
    }

    private fun syncSnapshot(outputStream: OutputStream) {
        currentType = TransferDataType.SNAPSHOT.value
        var rowId = -1L
        while (!quit) {
            val list = snapshotDao.getSnapshotByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.SNAPSHOT.value, it)
            }.forEach { snapshot ->
                writeJson(outputStream, TransferData.serializer(Snapshot.serializer()), snapshot)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = snapshotDao.getSnapshotRowId(list.last().snapshotId) ?: return
        }
    }

    private fun syncTranscriptMessage(outputStream: OutputStream) {
        currentType = TransferDataType.TRANSCRIPT_MESSAGE.value
        var rowId = -1L
        while (!quit) {
            val list = transcriptMessageDao.getTranscriptMessageByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.TRANSCRIPT_MESSAGE.value, it)
            }.forEach { transferMessage ->
                writeJson(outputStream, TransferData.serializer(TranscriptMessage.serializer()), transferMessage)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = transcriptMessageDao.getTranscriptMessageRowId(list.last().transcriptId, list.last().messageId) ?: return
        }
    }

    private fun syncPinMessage(outputStream: OutputStream) {
        currentType = TransferDataType.PIN_MESSAGE.value
        var rowId = -1L
        while (!quit) {
            val list = pinMessageDao.getPinMessageByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.PIN_MESSAGE.value, it)
            }.forEach { pinMessage ->
                writeJson(outputStream, TransferData.serializer(PinMessage.serializer()), pinMessage)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = pinMessageDao.getPinMessageRowId(list.last().messageId) ?: return
        }
    }

    private fun syncMessage(outputStream: OutputStream) {
        currentType = TransferDataType.MESSAGE.value
        var rowId = -1L
        while (!quit) {
            val list = messageDao.getMessageByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.MESSAGE.value, it)
            }.forEach { transcriptMessage ->
                writeJson(outputStream, TransferData.serializer(TransferMessage.serializer()), transcriptMessage)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = messageDao.getMessageRowid(list.last().messageId) ?: return
        }
    }

    private fun syncMessageMention(outputStream: OutputStream) {
        currentType = TransferDataType.MESSAGE_MENTION.value
        var rowId = -1L
        while (!quit) {
            val list = messageMentionDao.getMessageMentionByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.MESSAGE_MENTION.value, it)
            }.forEach { transferMessageMention ->
                writeJson(outputStream, TransferData.serializer(TransferMessageMention.serializer()), transferMessageMention)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = messageMentionDao.getMessageMentionRowId(list.last().messageId) ?: return
        }
    }

    private fun syncExpiredMessage(outputStream: OutputStream) {
        currentType = TransferDataType.EXPIRED_MESSAGE.value
        var offset = 0
        while (!quit) {
            val list = expiredMessageDao.getExpiredMessageByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.EXPIRED_MESSAGE.value, it)
            }.forEach { expiredMessage ->
                writeJson(outputStream, TransferData.serializer(ExpiredMessage.serializer()), expiredMessage)
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
                    if (f.parentFile?.name == "Transcripts" && transcriptMessageDao.countTranscriptByMessageId(name) > 0) {
                        protocol.write(outputStream, f, name)
                        count++
                    } else if (messageDao.findMessageById(name) != null) {
                        protocol.write(outputStream, f, name)
                        count++
                    }
                }
            }
        }
    }

    fun exit() = MixinApplication.get().applicationScope.launch(SINGLE_SOCKET_THREAD) {
        try {
            quit = true
            socket?.close()
            socket = null
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Timber.e("Exit server ${e.message}")
        }
    }

    companion object {
        private const val LIMIT = 100
    }
}

private val ACCEPT_SINGLE_THREAD = Executors.newSingleThreadExecutor { r -> Thread(r, "SINGLE_DB_EXECUTOR") }.asCoroutineDispatcher()
