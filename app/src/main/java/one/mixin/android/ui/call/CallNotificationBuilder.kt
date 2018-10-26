package one.mixin.android.ui.call

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import one.mixin.android.R
import one.mixin.android.vo.CallState
import one.mixin.android.vo.MessageStatus
import one.mixin.android.webrtc.CallService

class CallNotificationBuilder {

    companion object {
        private const val CHANNEL_NODE = "channel_node"
        const val WEBRTC_NOTIFICATION = 313388
        const val ACTION_EXIT = "action_exit"

        fun getCallNotification(context: Context, state: CallState, name: String?): Notification {
            val callIntent = Intent(context, CallActivity::class.java)
            callIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingCallIntent = PendingIntent.getActivity(context, 0, callIntent, 0)

            val builder = NotificationCompat.Builder(context, CHANNEL_NODE)
                .setSmallIcon(R.drawable.ic_close_black_24dp)
                .setContentIntent(pendingCallIntent)
                .setOngoing(true)
                .setContentTitle(name)

            when (state.callInfo.callState) {
                CallService.CallState.STATE_DIALING -> {
                    if (state.callInfo.dialingStatus != MessageStatus.READ) {
                        builder.setContentText(context.getString(R.string.call_notification_outgoing))
                    } else {
                        builder.setContentText(context.getString(R.string.call_notification_ringing))
                    }
                    builder.addAction(getAction(context, CallService.ACTION_CALL_CANCEL, R.drawable.ic_close_black_24dp, R.string
                        .call_notification_action_cancel) {
                        it.putExtra(CallService.EXTRA_FROM_NOTIFICATION, true)
                    })
                }
                CallService.CallState.STATE_RINGING -> {
                    builder.setContentText(context.getString(R.string.call_notification_incoming_voice))
                    builder.addAction(getAction(context, CallService.ACTION_CALL_ANSWER, R.drawable.ic_close_black_24dp, R.string
                        .call_notification_action_answer))
                    builder.addAction(getAction(context, CallService.ACTION_CALL_DECLINE, R.drawable.ic_close_black_24dp, R.string
                        .call_notification_action_decline))
                }
                CallService.CallState.STATE_CONNECTED -> {
                    builder.setContentText(context.getString(R.string.call_notification_connected))
                    builder.addAction(getAction(context, CallService.ACTION_CALL_LOCAL_END, R.drawable.ic_close_black_24dp, R.string
                        .call_notification_action_hang_up) {
                        it.putExtra(CallService.EXTRA_FROM_NOTIFICATION, true)
                    })
                }
                else -> {
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
            val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            return NotificationCompat.Action(iconResId, context.getString(titleResId), pendingIntent)
        }
    }
}
