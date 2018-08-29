package one.mixin.android.webrtc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.getSystemService
import java.lang.reflect.InvocationTargetException

/**
 * Listens for incoming PSTN calls and rejects them if a RedPhone call is already in progress.
 *
 * Unstable use of reflection employed to gain access to ITelephony.
 *
 */
class IncomingCallReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "IncomingCallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Checking incoming call...")

        if (intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) == null) {
            Log.w(TAG, "Telephony event does not contain number...")
            return
        }

        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE) != TelephonyManager.EXTRA_STATE_RINGING) {
            Log.w(TAG, "Telephony event is not state ringing...")
            return
        }
        val callListener = InCallListener(context, Handler())
        //TODO
    }

    private class InCallListener internal constructor(context: Context, handler: Handler) : ResultReceiver(handler) {

        private val context: Context = context.applicationContext

        override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
            if (resultCode == 1) {
                Log.i(TAG, "Attempting to deny incoming PSTN call.")
                val tm = context.getSystemService<TelephonyManager>()
                try {
                    val getTelephony = tm!!.javaClass.getDeclaredMethod("getITelephony")
                    getTelephony.isAccessible = true
                    val telephonyService = getTelephony.invoke(tm)
                    val endCall = telephonyService.javaClass.getDeclaredMethod("endCall")
                    endCall.invoke(telephonyService)
                    Log.i(TAG, "Denied Incoming Call.")
                } catch (e: NoSuchMethodException) {
                    Log.w(TAG, "Unable to access ITelephony API", e)
                } catch (e: IllegalAccessException) {
                    Log.w(TAG, "Unable to access ITelephony API", e)
                } catch (e: InvocationTargetException) {
                    Log.w(TAG, "Unable to access ITelephony API", e)
                }
            }
        }
    }
}