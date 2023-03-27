package one.mixin.android.ui.transfer

import one.mixin.android.MixinApplication
import one.mixin.android.util.NetworkUtils
import timber.log.Timber
import java.net.ServerSocket
import java.net.Socket

class TransferServer(private val finishListener: (String) -> Unit) {

    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null

    fun startServer(): String? {
        try {
            serverSocket = ServerSocket(TransferClient.SERVER_PORT) // todo replace port
            socket = serverSocket!!.accept()
            Timber.e("RUNNING")
            Thread(SocketHandler(socket!!, true, finishListener)).start()
        } catch (e: Exception) {
            Timber.e(e)
        }
        return NetworkUtils.getWifiIpAddress(MixinApplication.appContext)
    }
}
