package one.mixin.android.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putInt
import one.mixin.android.extension.supportsOreo
import one.mixin.android.extension.supportsQ
import org.jetbrains.anko.notificationManager
import timber.log.Timber

class ChannelManager {

    companion object {
        private const val CHANNEL_GROUP = "channel_group"
        const val CHANNEL_MESSAGE = "channel_message"
        private const val CHANNEL_CURRENT_VERSION = "channel_current_version"
        private const val CHANNEL_CURRENT_USER_VERSION = "channel_current_user_version"
        private const val CHANNEL_VERSION = 0

        fun create(context: Context, channelVersion: Int) {
            supportsOreo {
                val messageChannel =
                    NotificationChannel(
                        getChannelId(false),
                        context.getString(R.string.notification_message),
                        NotificationManager.IMPORTANCE_HIGH
                    )

                val uri =
                    Uri.parse("android.resource://${context.packageName}/${R.raw.mixin}").toString()
                messageChannel.setSound(
                    Uri.parse(uri),
                    AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .build()
                )
                messageChannel.enableVibration(true)
                messageChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                val groupChannel = copyChannel(messageChannel, getChannelId(true))
                groupChannel.name = context.getString(R.string.notification_group)

                supportsQ {
                    groupChannel.setAllowBubbles(true)
                    messageChannel.setAllowBubbles(true)
                }
                context.notificationManager.createNotificationChannels(
                    listOf(messageChannel, groupChannel)
                )
            }
        }

        @Synchronized
        fun updateChannelSound(context: Context) {
            supportsOreo {
                val channelUpdatedWithVersion = "$CHANNEL_CURRENT_VERSION$CHANNEL_VERSION"
                // first check current version channel is already updated
                if (context.defaultSharedPreferences.getBoolean(channelUpdatedWithVersion, false)) {
                    return
                }

                // then delete all old CHANNEL_GROUP and CHANNEL_MESSAGE channels
                deleteChannels(context)

                // finally create new channel and update SP
                create(context, CHANNEL_VERSION)
                context.defaultSharedPreferences.putBoolean(channelUpdatedWithVersion, true)
            }
        }

        fun resetChannelSound(context: Context) {
            supportsOreo {
                val currentUserVersion = context.defaultSharedPreferences.getInt(CHANNEL_CURRENT_USER_VERSION, 0)
                deleteChannels(context)
                context.defaultSharedPreferences.putInt(CHANNEL_CURRENT_USER_VERSION, currentUserVersion + 1)
                create(context, CHANNEL_VERSION)
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun deleteChannels(context: Context) {
            val existingChannels =
                context.notificationManager.notificationChannels ?: return
            try {
                existingChannels.forEach {
                    if (it.id.startsWith(CHANNEL_GROUP) || it.id.startsWith(CHANNEL_MESSAGE)) {
                        context.notificationManager.deleteNotificationChannel(it.id)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        fun getChannelId(isGroup: Boolean): String {
            val currentUserVersion = MixinApplication.appContext.defaultSharedPreferences.getInt(CHANNEL_CURRENT_USER_VERSION, 0)
            return if (isGroup) {
                "${CHANNEL_GROUP}_$CHANNEL_VERSION.$currentUserVersion"
            } else {
                "${CHANNEL_MESSAGE}_$CHANNEL_VERSION.$currentUserVersion"
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
