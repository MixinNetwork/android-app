package one.mixin.android.ui.transfer

import one.mixin.android.MixinApp
import one.mixin.android.MixinApplication
import one.mixin.android.util.NetworkUtils
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.UnknownHostException

class TransferClient {
    fun connectToServer(ip: String) {
        try {
            val socket = Socket(ip, SERVER_PORT)
            Thread(SocketHandler(socket,false)).start()
        } catch (e: UnknownHostException) {
            Timber.e(e)
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    companion object {
        const val SERVER_PORT = 8888 // Todo replace dynamic port
    }
}