package one.mixin.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import one.mixin.android.util.TimeCache
import timber.log.Timber

class TimezoneChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            Timber.d("@@@ action: ${intent.action}")
            TimeCache.singleton.evictAll()
        }
    }
}
