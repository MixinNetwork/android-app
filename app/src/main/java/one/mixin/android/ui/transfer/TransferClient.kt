package one.mixin.android.ui.transfer

import timber.log.Timber
import java.io.IOException
import java.net.Socket
import java.net.UnknownHostException

class TransferClient(private val finishListener: (String) -> Unit) {
    fun connectToServer(ip: String) {
        try {
            val socket = Socket(ip, SERVER_PORT)
            Thread(SocketHandler(socket, false, finishListener)).start()
        } catch (e: UnknownHostException) {
            Timber.e(e)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    companion object {
        const val SERVER_PORT = 8888 // Todo replace dynamic port
    }
}
