package one.mixin.android.ui.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinDatabase
import one.mixin.android.ui.transfer.vo.Data
import one.mixin.android.ui.transfer.vo.TransferData
import one.mixin.android.util.GsonHelper
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class SocketHandler(
    private val socket: Socket,
    private val isServer: Boolean,
    private val finishListener: (String) -> Unit,
) : Runnable {

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

    override fun run() {
        if (isServer) {
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
                            TransferData("message", Data(it))
                        }.forEach {
                            sendMessage(GsonHelper.customGson.toJson(it))
                        }
                        lastId = messageDao.getMessageRowid(messages.last().messageId)
                    } while (!quit)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        } else {
            MixinApplication.get().applicationScope.launch(Dispatchers.IO) {
                try {
                    do {
                        val message = bufferedReader.readLine()
                        if (message == null || message == "FINISH") {
                            finishListener("Finish Synchronized $count messages, ${System.currentTimeMillis() - startTime} ms")
                            exit()
                        }
                        Timber.e("${count++}" + message)
                    } while (!quit)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    private var count = 0

    private val inputStream by lazy {
        socket.getInputStream()
    }

    private val outputStream by lazy {
        socket.getOutputStream()
    }

    private val bufferedReader by lazy {
        BufferedReader(InputStreamReader(inputStream))
    }

    private val outputStreamWriter by lazy {
        OutputStreamWriter(outputStream)
    }

    fun sendMessage(message: String) {
        outputStreamWriter.write("$message\n")
        outputStreamWriter.flush()
    }

    fun exit() {
        quit = true
        inputStream.close()
        outputStream.close()
        socket.close()
    }
}
