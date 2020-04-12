package one.mixin.android.job

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Build.VERSION
import android.os.PowerManager
import com.birbit.android.jobqueue.network.NetworkEventProvider
import com.birbit.android.jobqueue.network.NetworkUtil
import one.mixin.android.vo.LinkState

class JobNetworkUtil(val context: Context, private val linkState: LinkState) : NetworkUtil, NetworkEventProvider {
    private var listener: NetworkEventProvider.Listener? = null

    init {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            listenForIdle(context)
        }
        listenNetworkViaConnectivityManager(context)

        linkState.observeForever { dispatchNetworkChange(context) }
    }

    private fun listenNetworkViaConnectivityManager(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                dispatchNetworkChange(context)
            }
        })
    }

    @TargetApi(23)
    private fun listenForIdle(context: Context) {
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                dispatchNetworkChange(context)
            }
        }, IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED))
    }

    internal fun dispatchNetworkChange(context: Context) {
        if (listener == null) { // shall not be but just be safe
            return
        }
        // http://developer.android.com/reference/android/net/ConnectivityManager.html#EXTRA_NETWORK_INFO
        // Since NetworkInfo can vary based on UID, applications should always obtain network information
        // through getActiveNetworkInfo() or getAllNetworkInfo().
        listener!!.onNetworkChange(getNetworkStatus(context))
    }

    override fun getNetworkStatus(context: Context): Int {
        if (isDozing(context)) {
            return NetworkUtil.DISCONNECTED
        }
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return NetworkUtil.DISCONNECTED
        val networkCapabilities = cm.getNetworkCapabilities(network) ?: return NetworkUtil.DISCONNECTED
        if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return NetworkUtil.DISCONNECTED
        }
        if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            if (LinkState.isOnline(linkState.state)) {
                return NetworkUtil.WEB_SOCKET
            }
            val unMetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            return if (unMetered) {
                NetworkUtil.UNMETERED
            } else {
                NetworkUtil.METERED
            }
        } else {
            return NetworkUtil.DISCONNECTED
        }
    }

    /**
     * Returns true if the device is in Doze/Idle mode. Should be called before checking the network connection because
     * the ConnectionManager may report the device is connected when it isn't during Idle mode.
     */
    @TargetApi(23)
    private fun isDozing(context: Context): Boolean {
        return if (VERSION.SDK_INT >= 23) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isDeviceIdleMode && !powerManager.isIgnoringBatteryOptimizations(
                context.packageName
            )
        } else {
            false
        }
    }

    override fun setListener(listener: NetworkEventProvider.Listener) {
        this.listener = listener
    }
}
