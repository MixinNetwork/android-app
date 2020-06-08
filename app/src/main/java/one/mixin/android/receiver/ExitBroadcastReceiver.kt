package one.mixin.android.receiver

import android.content.Context
import android.content.Intent
import dagger.android.DaggerBroadcastReceiver
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.BlazeMessageService.Companion.ACTION_TO_BACKGROUND
import one.mixin.android.ui.call.CallNotificationBuilder
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.webrtc.GroupCallService
import one.mixin.android.webrtc.VoiceCallService
import one.mixin.android.webrtc.stopService
import javax.inject.Inject

class ExitBroadcastReceiver : DaggerBroadcastReceiver() {

    @Inject
    lateinit var callState: CallStateLiveData

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent == null) return

        if (intent.action == ACTION_TO_BACKGROUND) {
            BlazeMessageService.startService(context, ACTION_TO_BACKGROUND)
        } else if (intent.action == CallNotificationBuilder.ACTION_EXIT) {
            if (callState.isGroupCall()) {
                stopService<GroupCallService>(context)
            } else {
                stopService<VoiceCallService>(context)
            }
        }
    }
}
