package one.mixin.android.ui.call

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import one.mixin.android.R
import one.mixin.android.vo.CallState
import one.mixin.android.vo.User
import one.mixin.android.webrtc.CallService

class CallNotificationBuilder {

    companion object {
        private const val CHANNEL_NODE = "channel_node"
        const val WEBRTC_NOTIFICATION = 313388
        const val ACTION_EXIT = "action_exit"

        fun getCallNotification(context: Context, state: CallState, user: User?): Notification? {
            if (state.callInfo.callState == CallService.CallState.STATE_IDLE) return null

            val callIntent = Intent(context, CallActivity::class.java)
            callIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            user?.let {
                callIntent.putExtra(CallActivity.ARGS_ANSWER, it)
            }
            val pendingCallIntent = PendingIntent.getActivity(context, 0, callIntent, FLAG_UPDATE_CURRENT)

            val builder = NotificationCompat.Builder(context, CHANNEL_NODE)
                .setSmallIcon(R.drawable.ic_msg_default)
                .setContentIntent(pendingCallIntent)
                .setOngoing(true)
                .setContentTitle(user?.fullName)

            when (state.callInfo.callState) {
                CallService.CallState.STATE_DIALING -> {
                    builder.setContentText(context.getString(R.string.call_notification_outgoing))
                    builder.addAction(getAction(context, CallService.ACTION_CALL_CANCEL, R.drawable.ic_close_black, R.string
                        .call_notification_action_cancel) {
                        it.putExtra(CallService.EXTRA_TO_IDLE, true)
                    })
                }
                CallService.CallState.STATE_RINGING -> {
                    builder.setContentText(context.getString(R.string.call_notification_incoming_voice))
                    builder.addAction(getAction(context, CallService.ACTION_CALL_ANSWER, R.drawable.ic_close_black, R.string
                        .call_notification_action_answer))
                    builder.addAction(getAction(context, CallService.ACTION_CALL_DECLINE, R.drawable.ic_close_black, R.string
                        .call_notification_action_decline))
                }
                CallService.CallState.STATE_CONNECTED -> {
                    builder.setContentText(context.getString(R.string.call_notification_connected))
                    builder.addAction(getAction(context, CallService.ACTION_CALL_LOCAL_END, R.drawable.ic_close_black, R.string
                        .call_notification_action_hang_up) {
                        it.putExtra(CallService.EXTRA_TO_IDLE, true)
                    })
                }
                else -> {
                    builder.setContentText(context.getString(R.string.call_connecting))
                    val action = if (state.isInitiator) CallService.ACTION_CALL_CANCEL else CallService.ACTION_CALL_DECLINE
                    builder.addAction(getAction(context, action, R.drawable.ic_close_black, R.string
                        .call_notification_action_hang_up) {
                        it.putExtra(CallService.EXTRA_TO_IDLE, true)
                    })
                }
            }
            return builder.build()
        }

        private fun getAction(
            context: Context,
            action: String,
            iconResId: Int,
            titleResId: Int,
            putExtra: ((intent: Intent) -> Unit)? = null
        ): NotificationCompat.Action {
            val intent = Intent(context, CallService::class.java)
            intent.action = action
            putExtra?.invoke(intent)
            val pendingIntent = PendingIntent.getService(context, 0, intent, FLAG_UPDATE_CURRENT)
            return NotificationCompat.Action(iconResId, context.getString(titleResId), pendingIntent)
        }
    }
}
