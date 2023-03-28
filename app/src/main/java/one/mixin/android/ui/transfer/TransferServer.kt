package one.mixin.android.ui.transfer

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinDatabase
import one.mixin.android.ui.transfer.vo.TransferData
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
        val messageDao = MixinDatabase.getDatabase(MixinApplication.appContext).messageDao()
        var lastId = messageDao.getLastMessageRowId()
        MixinApplication.get().applicationScope.launch(Dispatchers.IO) {
            try {
                do {
                    lastId ?: return@launch
                    val messages = messageDao.findMessages(lastId!!, 100)
                    if (messages.isEmpty()) {
                        sendMessage("FINISH")
                        exit()
                        return@launch
                    }
                    Timber.e("$lastId size:${messages.size}")
                    messages.map {
                        val s = gson.toJson(it)
                        TransferData("message", JsonParser.parseString(s).asJsonObject)
                    }.forEach {
                        sendMessage(gson.toJson(it))
                    }
                    lastId = messageDao.getMessageRowid(messages.last().messageId)
                    // sendMessage("hello")
                    // sendMessage("world")
                    // sendMessage("你好")
                    // sendMessage("你好，world")
                    // sendMessage("FINISH")
                    quit = true
                } while (!quit)
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

    fun sendMessage(message: String) {
        protocol.write(outputStream, message)
        outputStream.flush()
    }

    val protocol = TransferProtocol()

    fun exit() {
        quit = true
        inputStream.close()
        outputStream.close()
        socket.close()
    }
}
