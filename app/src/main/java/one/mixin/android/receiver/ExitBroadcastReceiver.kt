package one.mixin.android.receiver

import android.content.Context
import android.content.Intent
import dagger.android.DaggerBroadcastReceiver
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.BlazeMessageService.Companion.ACTION_TO_BACKGROUND

class ExitBroadcastReceiver : DaggerBroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent == null) return

        if (intent.action == ACTION_TO_BACKGROUND) {
            BlazeMessageService.startService(context, ACTION_TO_BACKGROUND)
        }
    }
}
