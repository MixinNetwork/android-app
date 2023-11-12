package one.mixin.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import one.mixin.android.extension.getParcelableCompat
import timber.log.Timber

class SMSReceiver : BroadcastReceiver() {
    companion object {
        var smsListener: SMSListener? = null
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION != intent?.action) return
        val extras = intent.extras ?: return
        val status = extras.getParcelableCompat(SmsRetriever.EXTRA_STATUS, Status::class.java) ?: return

        when (status.statusCode) {
            CommonStatusCodes.SUCCESS -> {
                val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE) ?: return
                smsListener?.onSuccess(message)
            }
            CommonStatusCodes.TIMEOUT -> {
                val msg = "SMSReceiver Waiting for SMS timed out (5 minutes)"
                smsListener?.onError(msg)
                Timber.d(msg)
            }
        }
    }
}

interface SMSListener {
    fun onSuccess(message: String)
    fun onError(message: String)
}
