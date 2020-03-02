package one.mixin.android.job

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.birbit.android.jobqueue.Params
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.supportsNougat
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.ChannelManager
import one.mixin.android.util.ChannelManager.Companion.CHANNEL_MESSAGE
import one.mixin.android.util.ChannelManager.Companion.CHANNEL_VERSION
import one.mixin.android.util.ChannelManager.Companion.getChannelId
import one.mixin.android.util.mention.rendMentionContent
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.SnapshotType
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import one.mixin.android.vo.isRepresentativeMessage
import org.jetbrains.anko.notificationManager

class NotificationJob(val message: Message, private val userMap: Map<String, String>? = null, private val force: Boolean = false) : BaseJob(Params(PRIORITY_UI_HIGH).requireNetwork().groupBy("notification_group")) {

    companion object {
        private const val serialVersionUID = 1L
        const val KEY_REPLY = "key_reply"
        const val CONVERSATION_ID = "conversation_id"
        const val IS_PLAIN = "is_plain"
    }

    override fun onRun() {
        ChannelManager.updateChannelSound(MixinApplication.appContext)
        notifyMessage(message)
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder

    private val notificationManager: NotificationManager by lazy {
        MixinApplication.appContext.notificationManager
    }

    @SuppressLint("NewApi")
    private fun notifyMessage(message: Message) {
        val context = MixinApplication.appContext
        val user = syncUser(message.userId) ?: return
        if (user.relationship == UserRelationship.BLOCKING.name) {
            return
        }
        val conversation = conversationDao.getConversationItem(message.conversationId) ?: return
        if (conversation.category == null) {
            return
        }
        if (!force && conversation.isMute()) {
            return
        }
        val mainIntent = MainActivity.getSingleIntent(context)
        val conversationIntent = ConversationActivity
            .putIntent(context, message.conversationId)

        notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (conversation.isGroup()) {
                NotificationCompat.Builder(context, getChannelId(true, CHANNEL_VERSION))
            } else {
                NotificationCompat.Builder(context, getChannelId(false, CHANNEL_VERSION))
            }
        } else {
            NotificationCompat.Builder(context, CHANNEL_MESSAGE)
        }

        notificationBuilder.setContentIntent(
            PendingIntent.getActivities(context, message.id.hashCode(),
                arrayOf(mainIntent, conversationIntent), PendingIntent.FLAG_UPDATE_CURRENT))
        supportsNougat {
            val remoteInput = RemoteInput.Builder(KEY_REPLY)
                .setLabel(context.getString(R.string.notification_reply))
                .build()
            val sendIntent = Intent(context, SendService::class.java)
            sendIntent.putExtra(CONVERSATION_ID, message.conversationId)
            sendIntent.putExtra(IS_PLAIN, user.isBot() || message.isRepresentativeMessage(conversation))
            val pendingIntent = PendingIntent.getService(
                context, message.conversationId.hashCode(), sendIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val action = NotificationCompat.Action.Builder(R.mipmap.ic_launcher,
                context.getString(R.string.notification_reply), pendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build()
            notificationBuilder.addAction(action)
            val readAction = NotificationCompat.Action.Builder(R.mipmap.ic_launcher,
                context.getString(R.string.notification_mark), pendingIntent)
                .build()
            notificationBuilder.addAction(readAction)
        }

        when (message.category) {
            MessageCategory.SIGNAL_TEXT.name, MessageCategory.PLAIN_TEXT.name -> {
                if (conversation.isGroup() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_text_message, user.fullName))
                    notificationBuilder.setContentText("${user.fullName} : ${rendMentionContent(message.content, userMap)}")
                } else {
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_text_message))
                    notificationBuilder.setContentText(rendMentionContent(message.content, userMap))
                }
            }
            MessageCategory.SIGNAL_IMAGE.name, MessageCategory.PLAIN_IMAGE.name -> {
                if (conversation.isGroup() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_image_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    notificationBuilder.setContentText(
                        context.getString(R.string.alert_key_group_image_message, user.fullName))
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_image_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_image_message))
                }
            }
            MessageCategory.SIGNAL_VIDEO.name, MessageCategory.PLAIN_VIDEO.name -> {
                if (conversation.isGroup() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_video_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    notificationBuilder.setContentText(
                        context.getString(R.string.alert_key_group_video_message, user.fullName))
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_video_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_video_message))
                }
            }
            MessageCategory.SIGNAL_LIVE.name, MessageCategory.PLAIN_LIVE.name -> {
                if (conversation.isGroup() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_live_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    notificationBuilder.setContentText(
                        context.getString(R.string.alert_key_group_live_message, user.fullName))
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_live_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_live_message))
                }
            }
            MessageCategory.SIGNAL_DATA.name, MessageCategory.PLAIN_DATA.name -> {
                if (conversation.isGroup() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_data_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    notificationBuilder.setContentText(
                        context.getString(R.string.alert_key_group_data_message, user.fullName))
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_data_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_data_message))
                }
            }
            MessageCategory.SIGNAL_AUDIO.name, MessageCategory.PLAIN_AUDIO.name -> {
                if (conversation.isGroup() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_audio_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    notificationBuilder.setContentText(
                        context.getString(R.string.alert_key_group_audio_message, user.fullName))
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_audio_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_audio_message))
                }
            }
            MessageCategory.SIGNAL_STICKER.name, MessageCategory.PLAIN_STICKER.name -> {
                if (conversation.isGroup() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_sticker_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    notificationBuilder.setContentText(
                        context.getString(R.string.alert_key_group_sticker_message, user.fullName))
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_sticker_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_sticker_message))
                }
            }
            MessageCategory.SIGNAL_CONTACT.name, MessageCategory.PLAIN_CONTACT.name -> {
                if (conversation.isGroup() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_contact_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    notificationBuilder.setContentText(
                        context.getString(R.string.alert_key_group_contact_message, user.fullName))
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_contact_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_contact_message))
                }
            }
            MessageCategory.SIGNAL_POST.name, MessageCategory.PLAIN_POST.name -> {
                if (conversation.isGroup() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_post_message, user.fullName))
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    notificationBuilder.setContentText("${user.fullName}: ${rendMentionContent(message.content, userMap)}")
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_post_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText("${user.fullName}: ${rendMentionContent(message.content, userMap)}")
                }
            }
            MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name -> {
                if (message.action == SnapshotType.transfer.name) {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_transfer_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_transfer_message))
                }
            }
            MessageCategory.APP_BUTTON_GROUP.name, MessageCategory.APP_CARD.name -> {
                if (conversation.isGroup() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_text_message, user.fullName))
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_text_message))
                } else {
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_text_message))
                    notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_text_message))
                }
            }
            MessageCategory.SYSTEM_CONVERSATION.name -> {
                notificationBuilder.setContentTitle(context.getString(R.string.app_name))
            }
            MessageCategory.WEBRTC_AUDIO_OFFER.name -> {
                notificationBuilder.setContentTitle(user.fullName)
                notificationBuilder.setContentText(context.getString(R.string.alert_key_contact_audio_call_message))
            }
            else -> {
                // No support
                return
            }
        }
        notificationBuilder.setSmallIcon(R.drawable.ic_msg_default)
        notificationBuilder.color = ContextCompat.getColor(context, R.color.gray_light)
        notificationBuilder.setWhen(System.currentTimeMillis())

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setSound(Uri.parse("android.resource://" + context.packageName + "/" + R.raw.mixin))
        }
        notificationBuilder.setAutoCancel(true)
        notificationBuilder.priority = NotificationCompat.PRIORITY_HIGH
        user.notNullWithElse({
            context.mainThread {
                val height = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
                val width = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

                Glide.with(context)
                    .asBitmap()
                    .load(it.avatarUrl)
                    .apply(RequestOptions().fitCenter().circleCrop())
                    .listener(object : RequestListener<Bitmap> {
                        override fun onResourceReady(
                            resource: Bitmap?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            notificationBuilder.setLargeIcon(resource)
                            notificationManager.notify(message.conversationId.hashCode(), notificationBuilder.build())
                            return false
                        }

                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            isFirstResource: Boolean
                        ):
                            Boolean {
                            notificationBuilder.setLargeIcon(
                                BitmapFactory.decodeResource(context.resources, R.drawable.default_avatar))
                            notificationManager.notify(message.conversationId.hashCode(), notificationBuilder.build())
                            return false
                        }
                    }).submit(width, height)
            }
        }, {
            notificationManager.notify(message.conversationId.hashCode(), notificationBuilder.build())
        })
    }

    private fun syncUser(userId: String): User? {
        val u = userDao.findUser(userId)
        if (u == null) {
            val response = userService.getUsers(arrayListOf(userId)).execute().body()
            if (response != null && response.isSuccess) {
                response.data?.let { data ->
                    for (user in data) {
                        runBlocking { userRepo.upsert(user) }
                    }
                }
            }
        }
        return u
    }

    private fun syncContactUser(conversationId: String): User? {
        return userDao.findContactByConversationId(conversationId)
    }
}
