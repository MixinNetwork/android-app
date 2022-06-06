package one.mixin.android.util.clear

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.notificationManager
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.ui.setting.SettingActivity.Companion.FROM_NOTIFICATION

class CleanNotification {
    companion object {
        private const val CLEAR_ID = 313388
        private const val CHANNEL_NODE = "channel_node"
        private fun getBackupNotification(context: Context, content: String?): Notification {
            val callIntent = Intent(context, SettingActivity::class.java)
            callIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            callIntent.putExtra(FROM_NOTIFICATION, true)
            val pendingCallIntent = PendingIntent.getActivity(
                context,
                0,
                callIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_NODE)
                .setSmallIcon(R.drawable.ic_msg_default)
                .setContentIntent(pendingCallIntent)
                .setOngoing(true)
                .setContentText(content ?: context.getString(R.string.deep_cleaning))
                .setContentTitle(context.getString(R.string.deep_clean))
            return builder.build()
        }

        private val notificationManager: NotificationManager by lazy {
            MixinApplication.appContext.notificationManager
        }

        fun show(content: String? = null) {
            notificationManager.notify(CLEAR_ID, getBackupNotification(MixinApplication.appContext, content))
        }

        fun cancel() {
            notificationManager.cancel(CLEAR_ID)
        }
    }
}
