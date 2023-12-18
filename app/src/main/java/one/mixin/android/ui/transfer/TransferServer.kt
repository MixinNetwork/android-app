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
import one.mixin.android.db.SafeSnapshotDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.TokenDao
import one.mixin.android.db.TranscriptMessageDao
import one.mixin.android.db.UserDao
import one.mixin.android.event.DeviceTransferProgressEvent
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.getTimeMonthsAgo
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
import one.mixin.android.ui.transfer.vo.compatible.isAttachment
import one.mixin.android.ui.transfer.vo.compatible.isTranscript
import one.mixin.android.ui.transfer.vo.compatible.markAttachmentAsPending
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
import one.mixin.android.vo.markAttachmentAsPending
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.Token
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
class TransferServer
    @Inject
    internal constructor(
        val assetDao: AssetDao,
        val tokenDao: TokenDao,
        val conversationDao: ConversationDao,
        val expiredMessageDao: ExpiredMessageDao,
        val messageDao: MessageDao,
        val participantDao: ParticipantDao,
        val pinMessageDao: PinMessageDao,
        val snapshotDao: SnapshotDao,
        val safeSnapshotDao: SafeSnapshotDao,
        val stickerDao: StickerDao,
        val transcriptMessageDao: TranscriptMessageDao,
        val userDao: UserDao,
        val appDao: AppDao,
        val messageMentionDao: MessageMentionDao,
        val status: TransferStatusLiveData,
        private val serializationJson: Json,
    ) {
        val protocol by lazy { TransferProtocol(serializationJson, secretBytes, true) }

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
        private var currentId: String? = null
            set(value) {
                if (field != value) {
                    field = value
                    Timber.e("Current id: $field")
                }
            }

        private val secretBytes by lazy {
            TransferCipher.generateKey()
        }

        suspend fun restartServer(createdSuccessCallback: (TransferCommand) -> Unit) =
            withContext(SINGLE_SOCKET_THREAD) {
                try {
                    quit = true
                    socket?.close()
                    socket = null
                    serverSocket?.close()
                    serverSocket = null
                } catch (e: Exception) {
                    Timber.e(e)
                }
                quit = false
                startServer(conversationIds, monthsAgo, createdSuccessCallback)
            }

        private var conversationIds: Collection<String>? = null
        private var monthsAgo: Int? = null
            private set(value) {
                field = value
                monthsAgoTimestamp =
                    if (value == null) {
                        null
                    } else {
                        getTimeMonthsAgo(value).toString()
                    }
            }
        private var monthsAgoTimestamp: String? = null

        suspend fun startServer(
            conversationIds: Collection<String>?,
            monthsAgo: Int?,
            createdSuccessCallback: (TransferCommand) -> Unit,
        ) = withContext(SINGLE_SOCKET_THREAD) {
            try {
                NetworkUtils.printWifiInfo(MixinApplication.appContext)
                val serverSocket = createSocket(port = Random.nextInt(1024, 1124))
                this@TransferServer.serverSocket = serverSocket
                status.value = TransferStatus.CREATED
                code = Random.nextInt(10000)
                this@TransferServer.conversationIds = conversationIds
                this@TransferServer.monthsAgo = monthsAgo
                createdSuccessCallback(
                    TransferCommand(
                        TransferCommandAction.PUSH.value,
                        NetworkUtils.getWifiIpAddress(MixinApplication.appContext),
                        this@TransferServer.port,
                        secretBytes.base64Encode(),
                        this@TransferServer.code,
                        userId = Session.getAccountId(),
                    ),
                )
                status.value = TransferStatus.WAITING_FOR_CONNECTION
                val socket =
                    withContext(ACCEPT_SINGLE_THREAD) {
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
                NetworkUtils.printWifiInfo(MixinApplication.appContext)
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

        private suspend fun run(
            inputStream: InputStream,
            outputStream: OutputStream,
        ) =
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

        private fun transfer(
            outputStream: OutputStream,
            type: String?,
            primaryId: String?,
            assistanceId: String?,
        ) {
            status.value = TransferStatus.SYNCING
            val transferDataType = transferDataTypeFromValue(type)
            sendStart(outputStream, transferDataType, primaryId, assistanceId)
            syncConversation(outputStream, transferDataType, primaryId)
            syncParticipant(outputStream, transferDataType, primaryId, assistanceId)
            syncUser(outputStream, transferDataType, primaryId)
            syncApp(outputStream, transferDataType, primaryId)
            syncAsset(outputStream, transferDataType, primaryId)
            syncToken(outputStream, transferDataType, primaryId)
            syncSnapshot(outputStream, transferDataType, primaryId)
            syncSafeSnapshot(outputStream, transferDataType, primaryId)
            syncSticker(outputStream, transferDataType, primaryId)
            syncPinMessage(outputStream, transferDataType, primaryId)
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

        private fun sendStart(
            outputStream: OutputStream,
            type: TransferDataType?,
            primaryId: String?,
            assistanceId: String?,
        ) {
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
                val list =
                    if (conversationIds.isNullOrEmpty()) {
                        conversationDao.getConversationsByLimitAndRowId(LIMIT, rowId)
                    } else {
                        conversationDao.getConversationsByLimitAndRowId(LIMIT, rowId, conversationIds!!)
                    }
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
                currentId = list.last().conversationId
                rowId = conversationDao.getConversationRowId(list.last().conversationId) ?: return
            }
        }

        private fun syncParticipant(
            outputStream: OutputStream,
            transferDataType: TransferDataType?,
            primaryId: String?,
            assistanceId: String?,
        ) {
            val collection = conversationIds
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
                val list =
                    if (collection.isNullOrEmpty()) {
                        participantDao.getParticipantsByLimitAndRowId(LIMIT, rowId)
                    } else {
                        participantDao.getParticipantsByLimitAndRowId(LIMIT, rowId, collection)
                    }
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
                currentId = list.last().userId
                rowId = participantDao.getParticipantRowId(list.last().conversationId, list.last().userId) ?: return
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
                currentId = list.last().userId
                rowId = userDao.getUserRowId(list.last().userId) ?: return
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
                currentId = list.last().appId
                rowId = appDao.getAppRowId(list.last().appId) ?: return
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
                currentId = list.last().assetId
                rowId = assetDao.getAssetRowId(list.last().assetId) ?: return
            }
        }

        private fun syncToken(
            outputStream: OutputStream,
            transferDataType: TransferDataType?,
            primaryId: String?,
        ) {
            var rowId = -1L
            if (transferDataType != null) {
                if (transferDataType.ordinal > TransferDataType.TOKEN.ordinal) {
                    // skip
                    return
                } else if (transferDataType == TransferDataType.TOKEN && primaryId != null) {
                    rowId = tokenDao.getTokenRowId(primaryId) ?: -1
                }
            }
            currentType = TransferDataType.TOKEN.value
            while (!quit) {
                val list = tokenDao.getTokenByLimitAndRowId(LIMIT, rowId)
                if (list.isEmpty()) {
                    return
                }
                list.map {
                    TransferData(TransferDataType.TOKEN.value, it)
                }.forEach { token ->
                    writeJson(outputStream, TransferData.serializer(Token.serializer()), token)
                    count++
                }
                if (list.size < LIMIT) {
                    return
                }
                currentId = list.last().assetId
                rowId = tokenDao.getTokenRowId(list.last().assetId) ?: return
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
                val list =
                    stickerDao.getStickersByLimitAndRowId(LIMIT, rowId)
                        .map {
                            it.lastUseAt =
                                it.lastUseAt?.let { lastUseAt ->
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
                currentId = list.last().stickerId
                rowId = stickerDao.getStickerRowId(list.last().stickerId) ?: return
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
                currentId = list.last().snapshotId
                rowId = snapshotDao.getSnapshotRowId(list.last().snapshotId) ?: return
            }
        }

        private fun syncSafeSnapshot(
            outputStream: OutputStream,
            transferDataType: TransferDataType?,
            primaryId: String?,
        ) {
            var rowId = -1L
            if (transferDataType != null) {
                if (transferDataType.ordinal > TransferDataType.SAFE_SNAPSHOT.ordinal) {
                    // skip
                    return
                } else if (transferDataType == TransferDataType.SAFE_SNAPSHOT && primaryId != null) {
                    rowId = safeSnapshotDao.getSnapshotRowId(primaryId) ?: -1
                }
            }
            currentType = TransferDataType.SAFE_SNAPSHOT.value
            while (!quit) {
                val list = safeSnapshotDao.getSnapshotByLimitAndRowId(LIMIT, rowId)
                if (list.isEmpty()) {
                    return
                }
                list.map {
                    TransferData(TransferDataType.SAFE_SNAPSHOT.value, it)
                }.forEach { snapshot ->
                    writeJson(outputStream, TransferData.serializer(SafeSnapshot.serializer()), snapshot)
                    count++
                }
                if (list.size < LIMIT) {
                    return
                }
                currentId = list.last().snapshotId
                rowId = safeSnapshotDao.getSnapshotRowId(list.last().snapshotId) ?: return
            }
        }

        // Sync data by messageId
        private fun syncTranscriptMessage(
            outputStream: OutputStream,
            transcriptId: String,
        ) {
            val list = transcriptMessageDao.getTranscript(transcriptId)
            if (list.isEmpty()) {
                return
            }
            list.map {
                it.markAttachmentAsPending()
            }.forEach { transcriptMessage ->
                writeJson(
                    outputStream,
                    TransferData.serializer(TranscriptMessage.serializer()),
                    TransferData(TransferDataType.TRANSCRIPT_MESSAGE.value, transcriptMessage),
                )
                syncTranscriptMediaFile(outputStream, transcriptMessage)
                count++
            }
        }

        private fun syncPinMessage(
            outputStream: OutputStream,
            transferDataType: TransferDataType?,
            primaryId: String?,
        ) {
            var rowId = -1L
            val collection = conversationIds
            val timestamp = monthsAgoTimestamp
            if (transferDataType != null) {
                if (transferDataType.ordinal > TransferDataType.PIN_MESSAGE.ordinal) {
                    // skip
                    return
                } else if (transferDataType == TransferDataType.PIN_MESSAGE && primaryId != null) {
                    rowId = pinMessageDao.getPinMessageRowId(primaryId) ?: -1
                }
            }
            if (timestamp != null) {
                val startId = pinMessageDao.getMessageRowidByCreateAt(timestamp)
                if (startId == null) {
                    // skip message
                    return
                } else {
                    rowId = startId
                }
            }
            currentType = TransferDataType.PIN_MESSAGE.value
            while (!quit) {
                val list =
                    if (collection.isNullOrEmpty()) {
                        pinMessageDao.getPinMessageByLimitAndRowId(LIMIT, rowId)
                    } else {
                        pinMessageDao.getPinMessageByLimitAndRowId(LIMIT, rowId, collection)
                    }
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
                currentId = list.last().messageId
                rowId = pinMessageDao.getPinMessageRowId(list.last().messageId) ?: return
            }
        }

        private fun syncMessage(
            outputStream: OutputStream,
            transferDataType: TransferDataType?,
            primaryId: String?,
        ) {
            var rowId = -1L
            val collection = conversationIds
            val timestamp = monthsAgoTimestamp
            val tooMuchId = (collection?.size ?: 0) > STRIDE_FOR_TRANSFER
            if (transferDataType != null) {
                if (transferDataType.ordinal > TransferDataType.MESSAGE.ordinal) {
                    // skip
                    return
                } else if (transferDataType == TransferDataType.MESSAGE && primaryId != null) {
                    rowId = messageDao.getMessageRowid(primaryId) ?: -1
                }
            }
            if (timestamp != null) {
                val startId = messageDao.getMessageRowidByCreateAt(timestamp)
                if (startId == null) {
                    // skip message
                    return
                } else {
                    rowId = startId
                }
            }
            currentType = TransferDataType.MESSAGE.value
            while (!quit) {
                val list =
                    if (timestamp != null && !collection.isNullOrEmpty() && !tooMuchId) {
                        messageDao.getMessageByLimitAndRowId(LIMIT, rowId, collection, timestamp)
                    } else if (timestamp != null && !collection.isNullOrEmpty() && tooMuchId) {
                        messageDao.getMessageByLimitAndRowId(LIMIT, rowId, timestamp)
                    } else if (timestamp != null) {
                        messageDao.getMessageByLimitAndRowId(LIMIT, rowId, timestamp)
                    } else if (!collection.isNullOrEmpty() && !tooMuchId) {
                        messageDao.getMessageByLimitAndRowId(LIMIT, rowId, collection)
                    } else {
                        messageDao.getMessageByLimitAndRowId(LIMIT, rowId)
                    }
                if (list.isEmpty()) {
                    return
                }
                if (tooMuchId && !collection.isNullOrEmpty()) {
                    list.filter { collection.contains(it.conversationId) }
                } else {
                    list
                }.map {
                    it.markAttachmentAsPending()
                }.forEach { transferMessage ->
                    if (transferMessage.isTranscript()) {
                        syncTranscriptMessage(outputStream, transferMessage.messageId)
                    }
                    writeJson(
                        outputStream,
                        TransferData.serializer(TransferMessage.serializer()),
                        TransferData(TransferDataType.MESSAGE.value, transferMessage),
                    )
                    syncMessageMediaFile(outputStream, transferMessage)
                    count++
                }
                if (list.size < LIMIT) {
                    return
                }
                currentId = list.last().messageId
                val lastRowId = messageDao.getMessageRowid(currentId!!) ?: return
                rowId = lastRowId + 1
            }
        }

        private fun syncMessageMention(
            outputStream: OutputStream,
            transferDataType: TransferDataType?,
            primaryId: String?,
        ) {
            var rowId = -1L
            val collection = conversationIds
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
                val list =
                    if (collection.isNullOrEmpty()) {
                        messageMentionDao.getMessageMentionByLimitAndRowId(LIMIT, rowId)
                    } else {
                        messageMentionDao.getMessageMentionByLimitAndRowId(LIMIT, rowId, collection)
                    }
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
                currentId = list.last().messageId
                rowId = messageMentionDao.getMessageMentionRowId(list.last().messageId) ?: return
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
                currentId = list.last().messageId
                rowId = expiredMessageDao.getExpiredMessageRowId(list.last().messageId) ?: return
            }
        }

        private fun syncMessageMediaFile(
            outputStream: OutputStream,
            message: TransferMessage,
        ) {
            if (!message.isAttachment()) return
            val context = MixinApplication.get()
            val mediaMessage = message.toMessage()
            mediaMessage.absolutePath(context)?.let { path ->
                val f =
                    try {
                        Uri.parse(path).toFile()
                    } catch (e: Exception) {
                        null
                    } ?: return
                Timber.e("Sync ${f.absolutePath}")
                if (f.isFile && f.exists() && f.length() > 0) {
                    protocol.write(outputStream, f, message.messageId)
                    count++
                }
            }
        }

        private fun syncTranscriptMediaFile(
            outputStream: OutputStream,
            message: TranscriptMessage,
        ) {
            val context = MixinApplication.get()
            if (!message.isAttachment()) return
            message.absolutePath(context)?.let { path ->
                val f =
                    try {
                        Uri.parse(path).toFile()
                    } catch (e: Exception) {
                        null
                    } ?: return
                Timber.e("Sync ${f.absolutePath}")
                if (f.isFile && f.exists() && f.length() > 0) {
                    protocol.write(outputStream, f, message.messageId)
                    count++
                }
            }
        }

        fun exit() =
            MixinApplication.get().applicationScope.launch(SINGLE_SOCKET_THREAD) {
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
            this.total = totalConversationCount(transferDataType, primaryId) + totalParticipantCount(transferDataType, primaryId, assistanceId) + totalUserCount(transferDataType, primaryId) +
                totalAppCount(transferDataType, primaryId) + totalAssetCount(transferDataType, primaryId) + totalTokenCount(transferDataType, primaryId) +
                totalSnapshotCount(transferDataType, primaryId) + totalSafeSnapshotCount(transferDataType, primaryId) + totalStickerCount(transferDataType, primaryId) +
                totalStickerCount(transferDataType, primaryId) + totalPinMessageCount(transferDataType, primaryId) + totalMessageCount(transferDataType, primaryId) +
                totalMessageMentionCount(transferDataType, primaryId) + totalExpiredMessageCount(transferDataType, primaryId)
            return total
        }

        private fun totalConversationCount(
            transferDataType: TransferDataType?,
            primaryId: String?,
        ): Long {
            val collection = conversationIds
            if (!collection.isNullOrEmpty()) return collection.size.toLong()
            return if (transferDataType == null || primaryId == null) {
                conversationDao.countConversations()
            } else if (transferDataType == TransferDataType.CONVERSATION) {
                val rowId = conversationDao.getConversationRowId(primaryId) ?: -1L
                conversationDao.countConversations(rowId)
            } else {
                0L
            }
        }

        private fun totalSnapshotCount(
            transferDataType: TransferDataType?,
            primaryId: String?,
        ): Long {
            return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.SNAPSHOT.ordinal) {
                snapshotDao.countSnapshots()
            } else if (transferDataType == TransferDataType.SNAPSHOT) {
                val rowId = snapshotDao.getSnapshotRowId(primaryId) ?: -1L
                snapshotDao.countSnapshots(rowId)
            } else {
                0L
            }
        }

        private fun totalSafeSnapshotCount(
            transferDataType: TransferDataType?,
            primaryId: String?,
        ): Long {
            return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.SAFE_SNAPSHOT.ordinal) {
                safeSnapshotDao.countSnapshots()
            } else if (transferDataType == TransferDataType.SAFE_SNAPSHOT) {
                val rowId = safeSnapshotDao.getSnapshotRowId(primaryId) ?: -1L
                safeSnapshotDao.countSnapshots(rowId)
            } else {
                0L
            }
        }

        private fun totalStickerCount(
            transferDataType: TransferDataType?,
            primaryId: String?,
        ): Long {
            return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.STICKER.ordinal) {
                stickerDao.countStickers()
            } else if (transferDataType == TransferDataType.STICKER) {
                val rowId = stickerDao.getStickerRowId(primaryId) ?: -1L
                stickerDao.countStickers(rowId)
            } else {
                0L
            }
        }

        private fun totalUserCount(
            transferDataType: TransferDataType?,
            primaryId: String?,
        ): Long {
            return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.USER.ordinal) {
                userDao.countUsers()
            } else if (transferDataType == TransferDataType.USER) {
                val rowId = userDao.getUserRowId(primaryId) ?: -1L
                userDao.countUsers(rowId)
            } else {
                0L
            }
        }

        private fun totalAppCount(
            transferDataType: TransferDataType?,
            primaryId: String?,
        ): Long {
            return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.APP.ordinal) {
                appDao.countApps()
            } else if (transferDataType == TransferDataType.APP) {
                val rowId = appDao.getAppRowId(primaryId) ?: -1L
                appDao.countApps(rowId)
            } else {
                0L
            }
        }

        private fun totalAssetCount(
            transferDataType: TransferDataType?,
            primaryId: String?,
        ): Long {
            return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.ASSET.ordinal) {
                assetDao.countAssets()
            } else if (transferDataType == TransferDataType.ASSET) {
                val rowId = tokenDao.getTokenRowId(primaryId) ?: -1L
                assetDao.countAssets(rowId)
            } else {
                0L
            }
        }

        private fun totalTokenCount(
            transferDataType: TransferDataType?,
            primaryId: String?,
        ): Long {
            return if (transferDataType == null || primaryId == null || transferDataType.ordinal < TransferDataType.TOKEN.ordinal) {
                tokenDao.countTokens()
            } else if (transferDataType == TransferDataType.TOKEN) {
                val rowId = tokenDao.getTokenRowId(primaryId) ?: -1L
                tokenDao.countTokens(rowId)
            } else {
                0L
            }
        }

        private fun totalMessageMentionCount(
            transferDataType: TransferDataType?,
            primaryId: String?,
        ): Long {
            val collection = conversationIds
            return if (transferDataType == null || transferDataType.ordinal < TransferDataType.MESSAGE_MENTION.ordinal) {
                if (collection.isNullOrEmpty()) {
                    messageMentionDao.countMessageMention()
                } else {
                    messageMentionDao.countMessageMention(collection)
                }
            } else if (transferDataType == TransferDataType.MESSAGE_MENTION && primaryId != null) {
                val rowId = messageMentionDao.getMessageMentionRowId(primaryId) ?: -1L
                if (collection.isNullOrEmpty()) {
                    messageMentionDao.countMessageMention(rowId)
                } else {
                    messageMentionDao.countMessageMention(rowId, collection)
                }
            } else {
                0L
            }
        }

        private fun totalPinMessageCount(
            transferDataType: TransferDataType?,
            primaryId: String?,
        ): Long {
            val collection = conversationIds
            val timestamp = monthsAgoTimestamp
            var rowId = -1L
            if (timestamp != null) {
                val startId = pinMessageDao.getMessageRowidByCreateAt(timestamp) ?: return 0
                rowId = startId
            } else if (transferDataType == TransferDataType.PIN_MESSAGE && primaryId != null) {
                rowId = pinMessageDao.getPinMessageRowId(primaryId) ?: -1L
            }
            return if (rowId == -1L) {
                if (collection.isNullOrEmpty()) {
                    pinMessageDao.countPinMessages()
                } else {
                    pinMessageDao.countPinMessages(collection)
                }
            } else {
                if (collection.isNullOrEmpty()) {
                    pinMessageDao.countPinMessages(rowId)
                } else {
                    pinMessageDao.countPinMessages(rowId, collection)
                }
            }
        }

        private fun totalExpiredMessageCount(
            transferDataType: TransferDataType?,
            primaryId: String?,
        ): Long {
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
            val collection = conversationIds
            return if (transferDataType == null || primaryId == null || assistanceId == null || transferDataType.ordinal < TransferDataType.PARTICIPANT.ordinal) {
                if (collection.isNullOrEmpty()) {
                    participantDao.countParticipants()
                } else {
                    participantDao.countParticipants(collection)
                }
            } else if (transferDataType == TransferDataType.PARTICIPANT) {
                val rowId = participantDao.getParticipantRowId(primaryId, assistanceId) ?: -1L
                if (collection.isNullOrEmpty()) {
                    participantDao.countParticipants(rowId)
                } else {
                    participantDao.countParticipants(rowId, collection)
                }
            } else {
                0L
            }
        }

        private fun totalMessageCount(
            transferDataType: TransferDataType?,
            primaryId: String?,
        ): Long {
            val collection = conversationIds
            val timestamp = monthsAgoTimestamp
            var rowId = -1L
            if (timestamp != null) {
                val startId = messageDao.getMessageRowidByCreateAt(timestamp) ?: return 0
                rowId = startId
            } else if (transferDataType == TransferDataType.MESSAGE && primaryId != null) {
                rowId = messageDao.getMessageRowid(primaryId) ?: -1L
            }
            return if (rowId == -1L) {
                if (collection.isNullOrEmpty()) {
                    messageDao.countMediaMessages() + messageDao.countMessages() + transcriptMessageDao.countTranscriptMessages()
                } else {
                    messageDao.countMediaMessages(collection) + messageDao.countMessages(collection) + transcriptMessageDao.countTranscriptMessages(collection)
                }
            } else {
                if (!collection.isNullOrEmpty() && timestamp != null) {
                    collection.chunked(STRIDE_FOR_TRANSFER).sumOf { ids ->
                        messageDao.countMediaMessages(rowId, ids, timestamp) + messageDao.countMessages(rowId, ids, timestamp) + transcriptMessageDao.countTranscriptMessages(rowId, ids, timestamp)
                    }
                } else if (!collection.isNullOrEmpty()) {
                    collection.chunked(STRIDE_FOR_TRANSFER).sumOf { ids ->
                        messageDao.countMediaMessages(rowId, ids) + messageDao.countMessages(rowId, ids) + transcriptMessageDao.countTranscriptMessages(rowId, ids)
                    }
                } else if (timestamp != null) {
                    messageDao.countMediaMessages(rowId, timestamp) + messageDao.countMessages(rowId, timestamp) + transcriptMessageDao.countTranscriptMessages(rowId, timestamp)
                } else {
                    messageDao.countMediaMessages(rowId) + messageDao.countMessages(rowId) + transcriptMessageDao.countTranscriptMessages(rowId)
                }
            }
        }

        companion object {
            private const val LIMIT = 100
            private const val STRIDE_FOR_TRANSFER = 900
        }
    }

private val ACCEPT_SINGLE_THREAD =
    Executors.newSingleThreadExecutor { r -> Thread(r, "SINGLE_DB_EXECUTOR") }
        .asCoroutineDispatcher()
