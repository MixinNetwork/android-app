package one.mixin.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.BlazeMessageService.Companion.ACTION_ACTIVITY_PAUSE

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent != null && intent.action == Intent.ACTION_BOOT_COMPLETED) {
            BlazeMessageService.startService(context, ACTION_ACTIVITY_PAUSE)
        }
    }
}
