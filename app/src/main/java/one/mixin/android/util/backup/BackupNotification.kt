package one.mixin.android.util.backup

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

class BackupNotification {
    companion object {
        private const val BACKUP_ID = 313389
        private const val CHANNEL_NODE = "channel_node"
        private fun getBackupNotification(context: Context, backup: Boolean = true): Notification {
            val callIntent = Intent(context, SettingActivity::class.java)
            callIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            callIntent.putExtra(FROM_NOTIFICATION, true)
            val pendingCallIntent = PendingIntent.getActivity(
                context, 0, callIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_NODE)
                .setSmallIcon(R.drawable.ic_msg_default)
                .setContentIntent(pendingCallIntent)
                .setOngoing(true)
                .setContentTitle(context.getString(if (backup) R.string.backup_notification_title else R.string.restore_in_progress))
                .setContentText(context.getString(if (backup) R.string.backup_notification_content else R.string.Restoring))

            return builder.build()
        }

        private val notificationManager: NotificationManager by lazy {
            MixinApplication.appContext.notificationManager
        }

        fun show(backup: Boolean = true) {
            notificationManager.notify(BACKUP_ID, getBackupNotification(MixinApplication.appContext, backup))
        }

        fun cancel() {
            notificationManager.cancel(BACKUP_ID)
        }
    }
}
