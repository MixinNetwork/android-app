package one.mixin.android.ui.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinDatabase
import one.mixin.android.ui.transfer.vo.TransferSendData
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.NetworkUtils
import timber.log.Timber
import java.net.ServerSocket
import java.net.Socket

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

    private val conversationDao by lazy {
        db.conversationDao()
    }

    private val gson by lazy {
        GsonHelper.customGson
    }

    fun startServer(): String? {
        MixinApplication.get().applicationScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(TransferClient.SERVER_PORT) // todo replace port
                socket = serverSocket.accept()
                Timber.e("RUNNING")
                run()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        return NetworkUtils.getWifiIpAddress(MixinApplication.appContext)
    }

    fun run() {
        MixinApplication.get().applicationScope.launch(Dispatchers.IO) {
            try {
                syncConversation()
                syncUser()
                syncAsset()
                syncSnapshot()
                syncSticker()
                syncMessage()
                sendMessage("FINISH")
                exit()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private val inputStream by lazy {
        socket.getInputStream()
    }

    private val outputStream by lazy {
        socket.getOutputStream()
    }

    private var count = 0
    fun sendMessage(message: String) {
        protocol.write(outputStream, message)
        outputStream.flush()
        Timber.e("send(${count++}) $message")
    }

    private fun syncConversation() {
    }

    private fun syncUser() {
    }

    private fun syncAsset() {
    }

    private fun syncSticker() {
    }

    private fun syncSnapshot() {
    }

    private fun syncMessage() {
        var lastId = messageDao.getLastMessageRowId() ?: return
        while (!quit) {
            val messages = messageDao.findMessages(lastId, 100)
            if (messages.isEmpty()) {
                return
            }
            messages.map {
                TransferSendData("message", it)
            }.forEach {
                sendMessage(gson.toJson(it))
            }
            if (messages.size < 100) {
                return
            }
            lastId = messageDao.getMessageRowid(messages.last().messageId) ?: return
        }
    }

    val protocol = TransferProtocol()

    fun exit() {
        quit = true
        inputStream.close()
        outputStream.close()
        socket.close()
    }
}
