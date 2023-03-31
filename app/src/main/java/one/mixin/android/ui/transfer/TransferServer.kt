package one.mixin.android.ui.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
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
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.isUUID
import one.mixin.android.ui.transfer.vo.TransferCommandAction
import one.mixin.android.ui.transfer.vo.TransferCommandData
import one.mixin.android.ui.transfer.vo.TransferData
import one.mixin.android.ui.transfer.vo.TransferDataType
import one.mixin.android.ui.transfer.vo.TransferSendData
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.NetworkUtils
import timber.log.Timber
import java.net.BindException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
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
) {

    private lateinit var serverSocket: ServerSocket
    private lateinit var socket: Socket

    private var quit = false

    private val gson by lazy {
        GsonHelper.customGson
    }

    private var code = 0
    private var port = 0

    fun startServer(toDesktop: Boolean, callback: (TransferCommandData) -> Unit) {
        MixinApplication.get().applicationScope.launch(Dispatchers.IO) {
            try {
                serverSocket = createSocket(port = Random.nextInt(100))
                code = Random.nextInt(10000)
                callback(
                    TransferCommandData(
                        TransferCommandAction.PUSH.value,
                        NetworkUtils.getWifiIpAddress(MixinApplication.appContext),
                        this@TransferServer.port,
                        generateAesKey().base64RawURLEncode(), // todo
                        this@TransferServer.code,
                    ),
                )
                socket = serverSocket.accept()
                socket.soTimeout = 10000

                val remoteAddr = socket.remoteSocketAddress
                if (remoteAddr is InetSocketAddress) {
                    val inetAddr = remoteAddr.address
                    val ip = inetAddr.hostAddress
                    if (toDesktop) {
                        run()
                    } else {
                        run()
                        Timber.e("Connected to $ip")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
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

    fun run() {
        MixinApplication.get().applicationScope.launch(Dispatchers.IO) {
            do {
                if (inputStream.available() <= 0) {
                    delay(300)
                    continue
                }
                val content = protocol.read(inputStream)
                try {
                    val transferData = gson.fromJson(content, TransferData::class.java)
                    if (transferData.type == TransferDataType.COMMAND.value) {
                        val commandData =
                            gson.fromJson(transferData.data, TransferCommandData::class.java)
                        if (commandData.code == code) {
                            Timber.e("Verification passed, start transmission")
                            transfer()
                        } else {
                            Timber.e("Validation failed, close")
                            exit()
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    exit()
                }
            } while (!quit)
        }
    }

    fun transfer() {
        try {
            sendStart()
            syncConversation()
            syncParticipant()
            syncUser()
            syncAsset()
            syncSnapshot()
            syncSticker()
            syncPinMessage()
            syncTranscriptMessage()
            syncMessage()
            syncExpiredMessage()
            syncMediaFile()
            sendClose()
            exit()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private val inputStream by lazy {
        socket.getInputStream()
    }

    private val outputStream by lazy {
        socket.getOutputStream()
    }

    private fun sendCommand(transferSendData: TransferSendData<TransferCommandData>) {
        sendJsonContent(gson.toJson(transferSendData))
    }

    private fun sendStart() {
        sendCommand(TransferSendData(TransferDataType.COMMAND.value, TransferCommandData(TransferCommandAction.START.value, total = totalCount())))
    }

    private fun totalCount(): Long {
        return messageDao.countMediaMessages() + messageDao.countMessages() + conversationDao.countConversations() +
            expiredMessageDao.countExpiredMessages() + participantDao.countParticipants() +
            pinMessageDao.countPinMessages() + snapshotDao.countSnapshots() + stickerDao.countStickers() +
            transcriptMessageDao.countTranscriptMessages() + userDao.countUsers()
    }

    private fun sendClose() {
        sendCommand(TransferSendData(TransferDataType.COMMAND.value, TransferCommandData(TransferCommandAction.CLOSE.value)))
    }

    private fun sendJsonContent(message: String) {
        protocol.write(outputStream, message)
        outputStream.flush()
    }

    private fun syncConversation() {
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
                sendJsonContent(gson.toJson(it))
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncParticipant() {
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
                sendJsonContent(gson.toJson(it))
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncUser() {
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
                sendJsonContent(gson.toJson(it))
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncAsset() {
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
                sendJsonContent(gson.toJson(it))
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncSticker() {
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
                sendJsonContent(gson.toJson(it))
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncSnapshot() {
        var offset = 0
        while (!quit) {
            val list = snapshotDao.getSnapshotByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.SNAPSHOT.value, it)
            }.forEach {
                sendJsonContent(gson.toJson(it))
                Timber.e("send snapshot ${it.data.snapshotId}")
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncTranscriptMessage() {
        var offset = 0
        while (!quit) {
            val list = transcriptMessageDao.getTranscriptMessageByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.TRANSCRIPT_MESSAGE.value, it)
            }.forEach {
                sendJsonContent(gson.toJson(it))
                Timber.e("send transcript ${it.data.transcriptId}")
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncPinMessage() {
        var offset = 0
        while (!quit) {
            val list = pinMessageDao.getPinMessageByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.PIN_MESSAGE.value, it)
            }.forEach {
                sendJsonContent(gson.toJson(it))
                Timber.e("send pin message: ${it.data.messageId}")
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncMessage() {
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
                sendJsonContent(gson.toJson(it))
            }
            if (messages.size < LIMIT) {
                return
            }
            lastId = messageDao.getMessageRowid(messages.last().messageId) ?: return
        }
    }

    private fun syncExpiredMessage() {
        var offset = 0
        while (!quit) {
            val list = expiredMessageDao.getExpiredMessageDaoByLimitAndOffset(LIMIT, offset)
            if (list.isEmpty()) {
                return
            }
            list.map {
                TransferSendData(TransferDataType.EXPIRED_MESSAGE.value, it)
            }.forEach {
                sendJsonContent(gson.toJson(it))
                Timber.e("send pin message: ${it.data.messageId}")
            }
            if (list.size < LIMIT) {
                return
            }
            offset += LIMIT
        }
    }

    private fun syncMediaFile() {
        val context = MixinApplication.get()
        val folder = context.getMediaPath() ?: return
        folder.walkTopDown().forEach { f ->
            if (f.isFile && f.length() > 0) {
                val name = f.nameWithoutExtension
                if (name.isUUID()) {
                    protocol.write(outputStream, f, name) }
            }
        }
    }

    val protocol = TransferProtocol()

    fun exit() {
        quit = true
        inputStream.close()
        outputStream.close()
        socket.close()
    }

    companion object {
        private const val LIMIT = 100
    }
}
