package one.mixin.android.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putInt
import one.mixin.android.extension.supportsOreo
import org.jetbrains.anko.notificationManager
import timber.log.Timber

class ChannelManager {

    companion object {
        const val CHANNEL_GROUP = "channel_group"
        const val CHANNEL_MESSAGE = "channel_message"
        private const val CHANNEL_GROUP_VERSION = "channel_group_version"

        private const val CHANNEL_MESSAGE_VERSION = "channel_message_version"
        private const val CHANNEL_UPDATED = "channel_updated"

        fun create(context: Context) {
            supportsOreo {
                val messageChannel =
                    NotificationChannel(
                        getChannelId(context, false),
                        context.getString(R.string.notification_message),
                        NotificationManager.IMPORTANCE_HIGH
                    )

                messageChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                val uri =
                    Uri.parse("android.resource://${context.packageName}/${R.raw.mixin}").toString()
                messageChannel.setSound(
                    Uri.parse(uri),
                    AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .build()
                )
                val groupChannel = copyChannel(messageChannel, getChannelId(context, true))
                groupChannel.name = context.getString(R.string.notification_group)
                context.notificationManager.createNotificationChannels(
                    listOf(messageChannel, groupChannel)
                )
            }
        }

        @Synchronized
        fun updateSound(context: Context) {
            supportsOreo {
                if (!context.defaultSharedPreferences.getBoolean(CHANNEL_UPDATED, false)) {
                    deleteChannel(context, CHANNEL_GROUP)
                    deleteChannel(context, CHANNEL_MESSAGE)
                    create(context)
                    context.defaultSharedPreferences.putBoolean(CHANNEL_UPDATED, true)
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun deleteChannel(context: Context, channelId: String) {
            val existingChannel =
                context.notificationManager.getNotificationChannel(channelId) ?: return
            try {
                context.notificationManager.deleteNotificationChannel(existingChannel.id)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        fun getChannelId(context: Context, isGroup: Boolean): String {
            return if (isGroup) {
                "$CHANNEL_GROUP${context.defaultSharedPreferences.getInt(CHANNEL_GROUP_VERSION, 0)}"
            } else {
                "$CHANNEL_MESSAGE${context.defaultSharedPreferences.getInt(
                    CHANNEL_MESSAGE_VERSION,
                    0
                )}"
            }
        }

        @Synchronized
        fun updateChannelSound(context: Context, uri: Uri, isGroup: Boolean): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val existingChannel =
                    context.notificationManager.getNotificationChannel(
                        getChannelId(
                            context,
                            isGroup
                        )
                    ) ?: return false
                try {
                    context.notificationManager.deleteNotificationChannel(existingChannel.id)
                } catch (e: Exception) {
                    Timber.e(e)
                }
                val oldVersion = context.defaultSharedPreferences.getInt(
                    if (isGroup) {
                        CHANNEL_GROUP_VERSION
                    } else {
                        CHANNEL_MESSAGE_VERSION
                    }, 0
                )
                val newChannel = copyChannel(
                    existingChannel,
                    "${if (isGroup) {
                        CHANNEL_GROUP
                    } else {
                        CHANNEL_MESSAGE
                    }}${oldVersion + 1}"
                )

                context.defaultSharedPreferences.putInt(
                    if (isGroup) {
                        CHANNEL_GROUP_VERSION
                    } else {
                        CHANNEL_MESSAGE_VERSION
                    }, oldVersion + 1
                )
                newChannel.setSound(
                    uri,
                    AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .build()
                )
                context.notificationManager.createNotificationChannel(newChannel)
                return true
            } else {
                return false
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun copyChannel(original: NotificationChannel, id: String): NotificationChannel {
            val copy = NotificationChannel(id, original.name, original.importance)

            copy.group = original.group
            copy.setSound(original.sound, original.audioAttributes)
            copy.setBypassDnd(original.canBypassDnd())
            copy.enableVibration(original.shouldVibrate())
            copy.vibrationPattern = original.vibrationPattern
            copy.lockscreenVisibility = original.lockscreenVisibility
            copy.setShowBadge(original.canShowBadge())
            copy.lightColor = original.lightColor
            copy.enableLights(original.shouldShowLights())

            return copy
        }
    }
}
