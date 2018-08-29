package one.mixin.android.ui.call

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import one.mixin.android.R
import one.mixin.android.webrtc.CallService

class CallNotificationBuilder {

    companion object {
        const val CHANNEL_NODE = "channel_node"
        const val WEBRTC_NOTIFICATION = 313388
        const val ACTION_EXIT = "action_exit"

        const val TYPE_INCOMING_RINGING = 1
        const val TYPE_OUTGOING_RINGING = 2
        const val TYPE_ESTABLISHED = 3
        const val TYPE_INCOMING_CONNECTING = 4

        fun getCallInProgressNotification(context: Context, type: Int): Notification {
            val callIntent = Intent(context, CallActivity::class.java)
            callIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingCallIntent = PendingIntent.getActivity(context, 0, callIntent, 0)

            val builder = NotificationCompat.Builder(context, CHANNEL_NODE)
                .setSmallIcon(R.drawable.ic_close_black_24dp)
                .setContentIntent(pendingCallIntent)
                .setOngoing(true)
                .setContentTitle("test")

            if (type == TYPE_INCOMING_CONNECTING) {
                builder.setContentText(context.getString(R.string.CallNotificationBuilder_connecting))
                builder.priority = NotificationCompat.PRIORITY_MIN
            } else if (type == TYPE_INCOMING_RINGING) {
                builder.setContentText(context.getString(R.string.NotificationBarManager__incoming_signal_call))
                builder.addAction(getServiceNotificationAction(context, CallService.ACTION_CALL_DECLINE, R.drawable.ic_close_black_24dp, R.string
                    .NotificationBarManager__deny_call))
//                builder.addAction(getActivityNotificationAction(context, CallActivity.ACTION_ANSWER, R.drawable.ic_close_black_24dp, R.string.NotificationBarManager__answer_call))
            } else if (type == TYPE_OUTGOING_RINGING) {
                builder.setContentText(context.getString(R.string.NotificationBarManager__establishing_signal_call))
                builder.addAction(getServiceNotificationAction(context, CallService.ACTION_CALL_CANCEL, R.drawable.ic_close_black_24dp, R.string.NotificationBarManager__cancel_call))
            } else {
                builder.setContentText(context.getString(R.string.NotificationBarManager_signal_call_in_progress))
                builder.addAction(getServiceNotificationAction(context, CallService.ACTION_CALL_CANCEL, R.drawable.ic_close_black_24dp, R.string.NotificationBarManager__end_call))
            }

            return builder.build()
        }

        private fun getServiceNotificationAction(context: Context, action: String, iconResId: Int, titleResId: Int): NotificationCompat.Action {
            val intent = Intent(context, CallService::class.java)
            intent.action = action

            val pendingIntent = PendingIntent.getService(context, 0, intent, 0)

            return NotificationCompat.Action(iconResId, context.getString(titleResId), pendingIntent)
        }

        private fun getActivityNotificationAction(context: Context, action: String,
            @DrawableRes iconResId: Int, @StringRes titleResId: Int): NotificationCompat.Action {
            val intent = Intent(context, CallActivity::class.java)
            intent.action = action

            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

            return NotificationCompat.Action(iconResId, context.getString(titleResId), pendingIntent)
        }
    }
}
