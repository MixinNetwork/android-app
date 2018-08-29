package one.mixin.android.webrtc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import one.mixin.android.webrtc.CallService

class ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_SCREEN_OFF == intent.action) {
            Intent(context, CallService::class.java).apply {
                action = CallService.ACTION_SCREEN_OFF
            }.run { context.startService(intent) }
        }
    }
}