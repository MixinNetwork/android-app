package one.mixin.android.ui.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.crypto.generateAesKey
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.getDeviceId
import one.mixin.android.extension.getMediaPath
import one.mixin.android.job.NotificationGenerator.userDao
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
import kotlin.random.Random

class TransferServer(private val finishListener: (String) -> Unit) {

    private lateinit var serverSocket: ServerSocket
    private lateinit var socket: Socket

    private var quit = false
    private val db by lazy {
        MixinDatabase.getDatabase(MixinApplication.appContext)
    }

    private val messageDao by lazy {
        db.messageDao()
    }

    private val assetDao by lazy {
        db.assetDao()
    }

    private val conversationDao by lazy {
        db.conversationDao()
    }

    private val participantDao by lazy {
        db.participantDao()
    }

    private val stickerDao by lazy {
        db.stickerDao()
    }

    private val snapshotDao by lazy {
        db.snapshotDao()
    }

    private val transcriptMessageDao by lazy {
        db.transcriptDao()
    }

    private val gson by lazy {
        GsonHelper.customGson
    }

    private var code = 0
        private set
    private var port = 0
        private set

    fun startServer(toDesktop: Boolean, callback: (TransferCommandData) -> Unit) {
        MixinApplication.get().applicationScope.launch(Dispatchers.IO) {
            try {
                serverSocket = createSocket(port = Random.nextInt(100))
                code = Random.nextInt(10000)
                callback(
                    TransferCommandData(
                        MixinApplication.appContext.getDeviceId(),
                        TransferCommandAction.PUSH.value,
                        1,
                        NetworkUtils.getWifiIpAddress(MixinApplication.appContext),
                        this@TransferServer.port,
                        generateAesKey().base64RawURLEncode(), // todo
                        this@TransferServer.code,
                    ),
                )
                socket = serverSocket.accept()

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
                if (inputStream.available() <= 0) delay(1000)
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
            syncConversation()
            syncParticipant()
            syncUser()
            syncAsset()
            syncSnapshot()
            syncSticker()
            syncTranscriptMessage()
            syncMessage()
            syncFile()
            // send Finish
            exit()
            finishListener("Finish")
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

    fun sendMessage(message: String) {
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
                sendMessage(gson.toJson(it))
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
                sendMessage(gson.toJson(it))
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
                sendMessage(gson.toJson(it))
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
                sendMessage(gson.toJson(it))
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
                sendMessage(gson.toJson(it))
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
                sendMessage(gson.toJson(it))
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
                sendMessage(gson.toJson(it))
                Timber.e("send transcript ${it.data.transcriptId}")
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
                sendMessage(gson.toJson(it))
            }
            if (messages.size < LIMIT) {
                return
            }
            lastId = messageDao.getMessageRowid(messages.last().messageId) ?: return
        }
    }

    private fun syncFile() {
        val context = MixinApplication.get()
        val folder = context.getMediaPath() ?: return
        folder.walkTopDown().forEach { f ->
            if (f.isFile && f.length() > 0) {
                protocol.write(outputStream, f, f.nameWithoutExtension)
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
