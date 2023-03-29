package one.mixin.android.ui.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinDatabase
import one.mixin.android.util.GsonHelper
import timber.log.Timber
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException

class TransferClient(private val finishListener: (String) -> Unit) {

    private lateinit var socket: Socket
    private var quit = false
    private var startTime = System.currentTimeMillis()
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

    private var count = 0

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

    fun connectToServer(ip: String) {
        try {
            socket = Socket(ip, SERVER_PORT)
            run()
        } catch (e: UnknownHostException) {
            Timber.e(e)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun run() {
        MixinApplication.get().applicationScope.launch(Dispatchers.IO) {
            try {
                do {
                    if (inputStream.available() <= 0) delay(1000)
                    val content = protocol.read(inputStream)
                    if (content == "FINISH") {
                        finishListener("Finish Synchronized $count messages, ${System.currentTimeMillis() - startTime} ms")
                        exit()
                    } else {
                        Timber.e("sync $content")
                        // val transferData = gson.fromJson(content, TransferData::class.java)
                        // when (transferData.type) {
                        //     "message" -> {
                        //         Timber.e(
                        //             "receiver(${count++}) ${
                        //                 gson.fromJson(
                        //                     transferData.data,
                        //                     TransferMessage::class.java,
                        //                 ).messageId
                        //             }",
                        //         )
                        //     }
                        //     else -> Timber.e("No support")
                        // }
                    }
                } while (!quit)
            } catch (e: SocketException) {
                // Todo
                Timber.e(e)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun exit() {
        quit = true
        inputStream.close()
        outputStream.close()
        socket.close()
    }

    companion object {
        const val SERVER_PORT = 8888 // Todo replace dynamic port
    }
}
