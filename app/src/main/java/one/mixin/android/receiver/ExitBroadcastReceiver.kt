package one.mixin.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.BlazeMessageService.Companion.ACTION_TO_BACKGROUND
import one.mixin.android.ui.call.CallNotificationBuilder
import one.mixin.android.webrtc.CallService

class ExitBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        if (intent.action == ACTION_TO_BACKGROUND) {
            BlazeMessageService.startService(context, ACTION_TO_BACKGROUND)
        } else if (intent.action == CallNotificationBuilder.ACTION_EXIT) {
            CallService.stopService(context)
        }
    }
}
