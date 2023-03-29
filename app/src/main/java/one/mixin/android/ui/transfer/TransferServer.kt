package one.mixin.android.ui.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.getMediaPath
import one.mixin.android.ui.transfer.vo.TransferSendData
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.NetworkUtils
import timber.log.Timber
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
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
                // syncMessage()
                syncFile()
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

    private fun syncFile() {
        val context = MixinApplication.get()
        val path = context.getMediaPath()
        for (i in 1..2) {
            // 随机确定文件大小（200KB ~ 300KB）
            val size = Random.nextInt(150 * 1024) + 100 * 1024

            // 生成随机 UUID 作为文件名
            val fileName = "${UUID.randomUUID()}"

            // 创建输出流并写入数据
            val file = File(path, fileName)
            val fos = file.outputStream()
            val buffer = ByteArray(1024)
            var remaining = size
            while (remaining > 0) {
                val len = buffer.size.coerceAtMost(remaining).also { remaining -= it }
                fos.write(buffer, 0, len)
            }
            fos.close()

            protocol.write(outputStream, file, fileName)
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
