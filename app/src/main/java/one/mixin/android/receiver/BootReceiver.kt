package one.mixin.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import one.mixin.android.job.BlazeMessageService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED == intent.action) {
            BlazeMessageService.startService(context)
        }
    }
}