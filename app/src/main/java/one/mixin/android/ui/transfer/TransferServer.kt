package one.mixin.android.ui.transfer

import android.net.Uri
import androidx.core.net.toFile
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
import one.mixin.android.ui.transfer.vo.compatible.toMessage
import one.mixin.android.ui.transfer.vo.transferDataTypeFromValue
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
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.isAttachment
import timber.log.Timber
import java.io.File
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
    private var currentId: Long? = null
        set(value) {
            if (field != value) {
                field = value
                Timber.e("Current id: $field")
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
                                    transfer(
                                        outputStream,
                                        result.type,
                                        result.primaryId,
                                        result.assistanceId,
                                    )
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

    private fun transfer(outputStream: OutputStream, type: String?, primaryId: String?, assistanceId: String?) {
        status.value = TransferStatus.SYNCING
        val transferDataType = transferDataTypeFromValue(type)
        sendStart(outputStream, transferDataType, primaryId, assistanceId)
        syncConversation(outputStream, transferDataType, primaryId)
        syncParticipant(outputStream, transferDataType, primaryId, assistanceId)
        syncUser(outputStream, transferDataType, primaryId)
        syncApp(outputStream, transferDataType, primaryId)
        syncAsset(outputStream, transferDataType, primaryId)
        syncSnapshot(outputStream, transferDataType, primaryId)
        syncSticker(outputStream, transferDataType, primaryId)
        syncPinMessage(outputStream, transferDataType, primaryId)
        syncTranscriptMessage(outputStream, transferDataType, primaryId, assistanceId)
        syncMessage(outputStream, transferDataType, primaryId)
        syncMessageMention(outputStream, transferDataType, primaryId)
        syncExpiredMessage(outputStream, transferDataType, primaryId)
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

    private fun sendStart(outputStream: OutputStream, type: TransferDataType?, primaryId: String?, assistanceId: String?) {
        writeCommand(outputStream, TransferCommand(TransferCommandAction.START.value, total = totalCount(type, primaryId, assistanceId)))
        RxBus.publish(DeviceTransferProgressEvent(0f))
        Timber.e("Started total: $total")
    }

    private fun sendFinish(outputStream: OutputStream) {
        writeCommand(
            outputStream,
            TransferCommand(TransferCommandAction.FINISH.value),
        )
    }

    private fun syncConversation(
        outputStream: OutputStream,
        transferDataType: TransferDataType?,
        primaryId: String?,
    ) {
        var rowId = -1L
        if (transferDataType != null) {
            if (transferDataType.ordinal > TransferDataType.CONVERSATION.ordinal) {
                // skip
                return
            } else if (transferDataType == TransferDataType.CONVERSATION && primaryId != null) {
                rowId = conversationDao.getConversationRowId(primaryId) ?: -1
            }
        }
        currentType = TransferDataType.CONVERSATION.value
        while (!quit) {
            val list = conversationDao.getConversationsByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.CONVERSATION.value, it)
            }.forEach { conversation ->
                writeJson(
                    outputStream,
                    TransferData.serializer(Conversation.serializer()),
                    conversation,
                )
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = conversationDao.getConversationRowId(list.last().conversationId) ?: return
            currentId = rowId
        }
    }

    private fun syncParticipant(
        outputStream: OutputStream,
        transferDataType: TransferDataType?,
        primaryId: String?,
        assistanceId: String?,
    ) {
        var rowId = -1L
        if (transferDataType != null) {
            if (transferDataType.ordinal > TransferDataType.PARTICIPANT.ordinal) {
                // skip
                return
            } else if (transferDataType == TransferDataType.PARTICIPANT && primaryId != null && assistanceId != null) {
                rowId = participantDao.getParticipantRowId(primaryId, assistanceId) ?: -1
            }
        }
        currentType = TransferDataType.PARTICIPANT.value

        while (!quit) {
            val list = participantDao.getParticipantsByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.PARTICIPANT.value, it)
            }.forEach { participant ->
                writeJson(
                    outputStream,
                    TransferData.serializer(Participant.serializer()),
                    participant,
                )
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId =
                participantDao.getParticipantRowId(list.last().conversationId, list.last().userId)
                    ?: return
            currentId = rowId
        }
    }

    private fun syncUser(
        outputStream: OutputStream,
        transferDataType: TransferDataType?,
        primaryId: String?,
    ) {
        var rowId = -1L
        if (transferDataType != null) {
            if (transferDataType.ordinal > TransferDataType.USER.ordinal) {
                // skip
                return
            } else if (transferDataType == TransferDataType.USER && primaryId != null) {
                rowId = userDao.getUserRowId(primaryId) ?: -1
            }
        }
        currentType = TransferDataType.USER.value
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
            currentId = rowId
        }
    }

    private fun syncApp(
        outputStream: OutputStream,
        transferDataType: TransferDataType?,
        primaryId: String?,
    ) {
        var rowId = -1L
        if (transferDataType != null) {
            if (transferDataType.ordinal > TransferDataType.APP.ordinal) {
                // skip
                return
            } else if (transferDataType == TransferDataType.APP && primaryId != null) {
                rowId = appDao.getAppRowId(primaryId) ?: -1
            }
        }
        currentType = TransferDataType.APP.value
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
            currentId = rowId
        }
    }

    private fun syncAsset(
        outputStream: OutputStream,
        transferDataType: TransferDataType?,
        primaryId: String?,
    ) {
        var rowId = -1L
        if (transferDataType != null) {
            if (transferDataType.ordinal > TransferDataType.ASSET.ordinal) {
                // skip
                return
            } else if (transferDataType == TransferDataType.ASSET && primaryId != null) {
                rowId = assetDao.getAssetRowId(primaryId) ?: -1
            }
        }
        currentType = TransferDataType.ASSET.value
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
            currentId = rowId
        }
    }

    private fun syncSticker(
        outputStream: OutputStream,
        transferDataType: TransferDataType?,
        primaryId: String?,
    ) {
        var rowId = -1L
        if (transferDataType != null) {
            if (transferDataType.ordinal > TransferDataType.STICKER.ordinal) {
                // skip
                return
            } else if (transferDataType == TransferDataType.STICKER && primaryId != null) {
                rowId = stickerDao.getStickerRowId(primaryId) ?: -1
            }
        }
        currentType = TransferDataType.STICKER.value
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
            currentId = rowId
        }
    }

    private fun syncSnapshot(
        outputStream: OutputStream,
        transferDataType: TransferDataType?,
        primaryId: String?,
    ) {
        var rowId = -1L
        if (transferDataType != null) {
            if (transferDataType.ordinal > TransferDataType.SNAPSHOT.ordinal) {
                // skip
                return
            } else if (transferDataType == TransferDataType.SNAPSHOT && primaryId != null) {
                rowId = snapshotDao.getSnapshotRowId(primaryId) ?: -1
            }
        }
        currentType = TransferDataType.SNAPSHOT.value
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
            currentId = rowId
        }
    }

    private fun syncTranscriptMessage(
        outputStream: OutputStream,
        transferDataType: TransferDataType?,
        primaryId: String?,
        assistanceId: String?,
    ) {
        var rowId = -1L
        if (transferDataType != null) {
            if (transferDataType.ordinal > TransferDataType.TRANSCRIPT_MESSAGE.ordinal) {
                // skip
                return
            } else if (transferDataType == TransferDataType.TRANSCRIPT_MESSAGE && primaryId != null && assistanceId != null) {
                rowId = transcriptMessageDao.getTranscriptMessageRowId(primaryId, assistanceId) ?: -1
            }
        }
        currentType = TransferDataType.TRANSCRIPT_MESSAGE.value
        while (!quit) {
            val list = transcriptMessageDao.getTranscriptMessageByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.forEach { transcriptMessage ->
                writeJson(
                    outputStream,
                    TransferData.serializer(TranscriptMessage.serializer()),
                    TransferData(TransferDataType.TRANSCRIPT_MESSAGE.value, transcriptMessage),
                )
                syncMediaFile(outputStream, transcriptMessage)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = transcriptMessageDao.getTranscriptMessageRowId(
                list.last().transcriptId,
                list.last().messageId,
            ) ?: return
            currentId = rowId
        }
    }

    private fun syncPinMessage(
        outputStream: OutputStream,
        transferDataType: TransferDataType?,
        primaryId: String?,
    ) {
        var rowId = -1L
        if (transferDataType != null) {
            if (transferDataType.ordinal > TransferDataType.PIN_MESSAGE.ordinal) {
                // skip
                return
            } else if (transferDataType == TransferDataType.PIN_MESSAGE && primaryId != null) {
                rowId = pinMessageDao.getPinMessageRowId(primaryId) ?: -1
            }
        }
        currentType = TransferDataType.PIN_MESSAGE.value
        while (!quit) {
            val list = pinMessageDao.getPinMessageByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.PIN_MESSAGE.value, it)
            }.forEach { pinMessage ->
                writeJson(
                    outputStream,
                    TransferData.serializer(PinMessage.serializer()),
                    pinMessage,
                )
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = pinMessageDao.getPinMessageRowId(list.last().messageId) ?: return
            currentId = rowId
        }
    }

    private fun syncMessage(
        outputStream: OutputStream,
        transferDataType: TransferDataType?,
        primaryId: String?,
    ) {
        var rowId = -1L
        if (transferDataType != null) {
            if (transferDataType.ordinal > TransferDataType.MESSAGE.ordinal) {
                // skip
                return
            } else if (transferDataType == TransferDataType.MESSAGE && primaryId != null) {
                rowId = messageDao.getMessageRowid(primaryId) ?: -1
            }
        }
        currentType = TransferDataType.MESSAGE.value
        while (!quit) {
            val list = messageDao.getMessageByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.forEach { transcriptMessage ->
                writeJson(
                    outputStream,
                    TransferData.serializer(TransferMessage.serializer()),
                    TransferData(TransferDataType.MESSAGE.value, transcriptMessage),
                )
                syncMediaFile(outputStream, transcriptMessage)
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = messageDao.getMessageRowid(list.last().messageId) ?: return
            currentId = rowId
        }
    }

    private fun syncMessageMention(
        outputStream: OutputStream,
        transferDataType: TransferDataType?,
        primaryId: String?,
    ) {
        var rowId = -1L
        if (transferDataType != null) {
            if (transferDataType.ordinal > TransferDataType.MESSAGE_MENTION.ordinal) {
                // skip
                return
            } else if (transferDataType == TransferDataType.MESSAGE_MENTION && primaryId != null) {
                rowId = messageMentionDao.getMessageMentionRowId(primaryId) ?: -1
            }
        }
        currentType = TransferDataType.MESSAGE_MENTION.value
        while (!quit) {
            val list = messageMentionDao.getMessageMentionByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.MESSAGE_MENTION.value, it)
            }.forEach { transferMessageMention ->
                writeJson(
                    outputStream,
                    TransferData.serializer(TransferMessageMention.serializer()),
                    transferMessageMention,
                )
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = messageMentionDao.getMessageMentionRowId(list.last().messageId) ?: return
            currentId = rowId
        }
    }

    private fun syncExpiredMessage(
        outputStream: OutputStream,
        transferDataType: TransferDataType?,
        primaryId: String?,
    ) {
        var rowId = -1L
        if (transferDataType != null) {
            if (transferDataType.ordinal > TransferDataType.EXPIRED_MESSAGE.ordinal) {
                // skip
                return
            } else if (transferDataType == TransferDataType.EXPIRED_MESSAGE && primaryId != null) {
                rowId = expiredMessageDao.getExpiredMessageRowId(primaryId) ?: -1
            }
        }
        currentType = TransferDataType.EXPIRED_MESSAGE.value
        while (!quit) {
            val list = expiredMessageDao.getExpiredMessageByLimitAndRowId(LIMIT, rowId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferData(TransferDataType.EXPIRED_MESSAGE.value, it)
            }.forEach { expiredMessage ->
                writeJson(
                    outputStream,
                    TransferData.serializer(ExpiredMessage.serializer()),
                    expiredMessage,
                )
                count++
            }
            if (list.size < LIMIT) {
                return
            }
            rowId = expiredMessageDao.getExpiredMessageRowId(list.last().messageId) ?: return
            currentId = rowId
        }
    }

    private fun syncMediaFile(outputStream: OutputStream, message: TransferMessage) {
        val context = MixinApplication.get()
        val mediaMessage = message.toMessage()
        mediaMessage.absolutePath(context)?.let { path ->
            val f = try {
                Uri.parse(path).toFile()
            } catch (e: Exception) {
                null
            } ?: return
            Timber.e("Sync ${f.absolutePath}")
            if (f.exists() && f.length() > 0) {
                protocol.write(outputStream, f, message.messageId)
                count++
            }
        }
    }

    private fun syncMediaFile(outputStream: OutputStream, message: TranscriptMessage) {
        val context = MixinApplication.get()
        if (!message.isAttachment()) return
        message.absolutePath(context)?.let { path ->
            val f = try {
                Uri.parse(path).toFile()
            } catch (e: Exception) {
                null
            } ?: return
            Timber.e("Sync ${f.absolutePath}")
            if (f.exists() && f.length() > 0) {
                protocol.write(outputStream, f, message.messageId)
                count++
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

    private fun totalCount(
        transferDataType: TransferDataType?,
        primaryId: String?,
        assistanceId: String?,
    ): Long {
        this.total = totalConversationCount(transferDataType, primaryId) +
            totalParticipantCount(transferDataType, primaryId, assistanceId) + totalUserCount(
                transferDataType,
                primaryId,
            ) + totalAppCount(transferDataType, primaryId) + totalAssetCount(transferDataType, primaryId) +
            totalSnapshotCount(transferDataType, primaryId) + totalStickerCount(
                transferDataType,
                primaryId,
            ) + totalPinMessageCount(transferDataType, primaryId) + totalTranscriptMessageCount(
                transferDataType,
                primaryId,
                assistanceId,
            ) + totalMessageCount(transferDataType, primaryId) + totalMessageMentionCount(transferDataType, primaryId) + totalExpiredMessageCount(
                transferDataType,
                primaryId,
            )
        return total
    }

    private fun totalConversationCount(transferDataType: TransferDataType?, primaryId: String?): Long {
        return if (transferDataType == null || primaryId == null) {
            conversationDao.countConversations()
        } else if (transferDataType == TransferDataType.CONVERSATION) {
            val rowId = conversationDao.getConversationRowId(primaryId) ?: -1L
            conversationDao.countConversations(rowId)
        } else {
            0L
        }
    }

    private fun totalSnapshotCount(transferDataType: TransferDataType?, primaryId: String?): Long {
        return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.SNAPSHOT.ordinal) {
            snapshotDao.countSnapshots()
        } else if (transferDataType == TransferDataType.SNAPSHOT) {
            val rowId = snapshotDao.getSnapshotRowId(primaryId) ?: -1L
            snapshotDao.countSnapshots(rowId)
        } else {
            0L
        }
    }

    private fun totalStickerCount(transferDataType: TransferDataType?, primaryId: String?): Long {
        return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.STICKER.ordinal) {
            stickerDao.countStickers()
        } else if (transferDataType == TransferDataType.STICKER) {
            val rowId = stickerDao.getStickerRowId(primaryId) ?: -1L
            stickerDao.countStickers(rowId)
        } else {
            0L
        }
    }

    private fun totalTranscriptMessageCount(transferDataType: TransferDataType?, primaryId: String?, assistanceId: String?): Long {
        return if (transferDataType == null || primaryId == null || assistanceId == null || transferDataType.ordinal < TransferDataType.TRANSCRIPT_MESSAGE.ordinal) {
            transcriptMessageDao.countTranscriptMessages()
        } else if (transferDataType == TransferDataType.TRANSCRIPT_MESSAGE) {
            val rowId = transcriptMessageDao.getTranscriptMessageRowId(primaryId, assistanceId) ?: -1L
            transcriptMessageDao.countTranscriptMessages(rowId)
        } else {
            0L
        }
    }

    private fun totalUserCount(transferDataType: TransferDataType?, primaryId: String?): Long {
        return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.USER.ordinal) {
            userDao.countUsers()
        } else if (transferDataType == TransferDataType.USER) {
            val rowId = userDao.getUserRowId(primaryId) ?: -1L
            userDao.countUsers(rowId)
        } else {
            0L
        }
    }

    private fun totalAppCount(transferDataType: TransferDataType?, primaryId: String?): Long {
        return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.APP.ordinal) {
            appDao.countApps()
        } else if (transferDataType == TransferDataType.APP) {
            val rowId = appDao.getAppRowId(primaryId) ?: -1L
            appDao.countApps(rowId)
        } else {
            0L
        }
    } private fun totalAssetCount(transferDataType: TransferDataType?, primaryId: String?): Long {
        return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.ASSET.ordinal) {
            assetDao.countAssets()
        } else if (transferDataType == TransferDataType.ASSET) {
            val rowId = assetDao.getAssetRowId(primaryId) ?: -1L
            assetDao.countAssets(rowId)
        } else {
            0L
        }
    }

    private fun totalMessageMentionCount(transferDataType: TransferDataType?, primaryId: String?): Long {
        return if (transferDataType == null || transferDataType.ordinal < TransferDataType.MESSAGE_MENTION.ordinal) {
            messageMentionDao.countMessageMention()
        } else if (transferDataType == TransferDataType.MESSAGE_MENTION && primaryId != null) {
            val rowId = messageMentionDao.getMessageMentionRowId(primaryId) ?: -1L
            messageMentionDao.countMessageMention(rowId)
        } else {
            0L
        }
    }

    private fun totalPinMessageCount(transferDataType: TransferDataType?, primaryId: String?): Long {
        return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.PIN_MESSAGE.ordinal) {
            pinMessageDao.countPinMessages()
        } else if (transferDataType == TransferDataType.PIN_MESSAGE) {
            val rowId = pinMessageDao.getPinMessageRowId(primaryId) ?: -1L
            pinMessageDao.countPinMessages(rowId)
        } else {
            0L
        }
    }

    private fun totalExpiredMessageCount(transferDataType: TransferDataType?, primaryId: String?): Long {
        return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.EXPIRED_MESSAGE.ordinal) {
            expiredMessageDao.countExpiredMessages()
        } else if (transferDataType == TransferDataType.EXPIRED_MESSAGE) {
            val rowId = expiredMessageDao.getExpiredMessageRowId(primaryId) ?: -1L
            expiredMessageDao.countExpiredMessages(rowId)
        } else {
            0L
        }
    }

    private fun totalParticipantCount(
        transferDataType: TransferDataType?,
        primaryId: String?,
        assistanceId: String?,
    ): Long {
        return if (transferDataType == null || primaryId == null || assistanceId == null || transferDataType.ordinal < TransferDataType.PARTICIPANT.ordinal) {
            participantDao.countParticipants()
        } else if (transferDataType == TransferDataType.PARTICIPANT) {
            val rowId = participantDao.getParticipantRowId(primaryId, assistanceId) ?: -1L
            participantDao.countParticipants(rowId)
        } else {
            0L
        }
    }

    private fun totalMessageCount(transferDataType: TransferDataType?, primaryId: String?): Long {
        return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.MESSAGE.ordinal) {
            messageDao.countMediaMessages() + messageDao.countMessages()
        } else if (transferDataType == TransferDataType.MESSAGE) {
            val rowId = messageDao.getMessageRowid(primaryId) ?: -1L
            messageDao.countMediaMessages(rowId) + messageDao.countMessages(rowId)
        } else {
            0L
        }
    }

    companion object {
        private const val LIMIT = 100
    }
}

private val ACCEPT_SINGLE_THREAD =
    Executors.newSingleThreadExecutor { r -> Thread(r, "SINGLE_DB_EXECUTOR") }
        .asCoroutineDispatcher()
