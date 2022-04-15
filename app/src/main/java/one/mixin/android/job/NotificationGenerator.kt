package one.mixin.android.job

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ
import androidx.core.app.NotificationCompat.Action.SEMANTIC_ACTION_REPLY
import androidx.core.app.NotificationCompat.CATEGORY_MESSAGE
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.notificationManager
import one.mixin.android.extension.supportsNougat
import one.mixin.android.extension.supportsR
import one.mixin.android.ui.conversation.BubbleActivity
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.ChannelManager
import one.mixin.android.util.ChannelManager.Companion.GROUP
import one.mixin.android.util.ChannelManager.Companion.MESSAGES
import one.mixin.android.util.ChannelManager.Companion.SILENCE
import one.mixin.android.util.mention.rendMentionContent
import one.mixin.android.util.updateShortcuts
import one.mixin.android.vo.App
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.SnapshotType
import one.mixin.android.vo.UserRelationship
import one.mixin.android.vo.getEncryptedCategory
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isData
import one.mixin.android.vo.isGroupConversation
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isLocation
import one.mixin.android.vo.isPost
import one.mixin.android.vo.isRepresentativeMessage
import one.mixin.android.vo.isSticker
import one.mixin.android.vo.isText
import one.mixin.android.vo.isTranscript
import one.mixin.android.vo.isVideo

const val KEY_REPLY = "key_reply"
const val CONVERSATION_ID = "conversation_id"
const val ENCRYPTED_CATEGORY = "encrypted_category"

object NotificationGenerator : Injector() {

    private val notificationManager: NotificationManager by lazy {
        MixinApplication.appContext.notificationManager
    }

    fun generate(
        lifecycleScope: CoroutineScope,
        message: Message,
        userMap: Map<String, String>? = null,
        force: Boolean = false,
        isSilent: Boolean = false
    ) = lifecycleScope.launch(Dispatchers.IO) {
        ChannelManager.updateChannelSound(MixinApplication.appContext)

        val context = MixinApplication.appContext
        val user = syncUser(message.userId) ?: return@launch
        if (user.relationship == UserRelationship.BLOCKING.name) {
            return@launch
        }
        val conversation = conversationDao.getConversationItem(message.conversationId) ?: return@launch
        if (conversation.category == null) {
            return@launch
        }
        if (!force && conversation.isMute()) {
            return@launch
        }
        val mainIntent = MainActivity.getSingleIntent(context)
        val conversationIntent = ConversationActivity
            .putIntent(context, message.conversationId)

        val channelId: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isSilent) {
                ChannelManager.getChannelId(SILENCE)
            } else if (conversation.isGroupConversation()) {
                ChannelManager.getChannelId(GROUP)
            } else {
                ChannelManager.getChannelId(MESSAGES)
            }
        } else {
            ChannelManager.CHANNEL_MESSAGE
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
        notificationBuilder.setContentIntent(
            PendingIntent.getActivities(
                context,
                message.id.hashCode(),
                arrayOf(mainIntent, conversationIntent),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
        ).setCategory(CATEGORY_MESSAGE)
        supportsNougat {
            val remoteInput = RemoteInput.Builder(KEY_REPLY)
                .setLabel(context.getString(R.string.Reply))
                .build()
            val sendIntent = Intent(context, SendService::class.java)
            sendIntent.putExtra(CONVERSATION_ID, message.conversationId)
            var app: App? = null
            var isBot = user.isBot()
            if (user.isBot()) {
                app = appDao.findAppById(requireNotNull(user.appId))
            } else if (message.isRepresentativeMessage(conversation)) {
                val representativeUser = syncUser(conversation.ownerId)
                if (representativeUser == null) {
                    isBot = false
                } else {
                    app = appDao.findAppById(requireNotNull(representativeUser.appId))
                    isBot = representativeUser.isBot()
                }
            }
            val encryptCategory = getEncryptedCategory(isBot, app)
            sendIntent.putExtra(ENCRYPTED_CATEGORY, encryptCategory.ordinal)
            val pendingIntent = PendingIntent.getService(
                context,
                message.conversationId.hashCode(),
                sendIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            val action = NotificationCompat.Action.Builder(
                R.mipmap.ic_launcher,
                context.getString(R.string.Reply),
                pendingIntent
            )
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .setSemanticAction(SEMANTIC_ACTION_REPLY)
                .setShowsUserInterface(false)
                .build()
            notificationBuilder.addAction(action)
            val readAction = NotificationCompat.Action.Builder(
                R.mipmap.ic_launcher,
                context.getString(R.string.notification_mark),
                pendingIntent
            ).setSemanticAction(SEMANTIC_ACTION_MARK_AS_READ)
                .setShowsUserInterface(false)
                .build()
            notificationBuilder.addAction(readAction)
        }

        var contentText: String? = null
        when {
            message.isText() -> {
                if (conversation.isGroupConversation() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_text_message, user.fullName)
                    )
                    contentText = "${user.fullName} : ${rendMentionContent(message.content, userMap)}"
                } else {
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_text_message))
                    contentText = rendMentionContent(message.content, userMap)
                }
            }
            message.isImage() -> {
                if (conversation.isGroupConversation() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_image_message, user.fullName)
                    )
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    contentText = context.getString(R.string.alert_key_group_image_message, user.fullName)
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_image_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    contentText = context.getString(R.string.alert_key_contact_image_message)
                }
            }
            message.isVideo() -> {
                if (conversation.isGroupConversation() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_video_message, user.fullName)
                    )
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    contentText = context.getString(R.string.alert_key_group_video_message, user.fullName)
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_video_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    contentText = context.getString(R.string.alert_key_contact_video_message)
                }
            }
            message.isLive() -> {
                if (conversation.isGroupConversation() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_live_message, user.fullName)
                    )
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    contentText = context.getString(R.string.alert_key_group_live_message, user.fullName)
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_live_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    contentText = context.getString(R.string.alert_key_contact_live_message)
                }
            }
            message.isData() -> {
                if (conversation.isGroupConversation() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_data_message, user.fullName)
                    )
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    contentText = context.getString(R.string.alert_key_group_data_message, user.fullName)
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_data_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    contentText = context.getString(R.string.alert_key_contact_data_message)
                }
            }
            message.isAudio() -> {
                if (conversation.isGroupConversation() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_audio_message, user.fullName)
                    )
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    contentText = context.getString(R.string.alert_key_group_audio_message, user.fullName)
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_audio_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    contentText = context.getString(R.string.alert_key_contact_audio_message)
                }
            }
            message.isSticker() -> {
                if (conversation.isGroupConversation() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_sticker_message, user.fullName)
                    )
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    contentText = context.getString(R.string.alert_key_group_sticker_message, user.fullName)
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_sticker_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    contentText = context.getString(R.string.alert_key_contact_sticker_message)
                }
            }
            message.isContact() -> {
                if (conversation.isGroupConversation() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_contact_message, user.fullName)
                    )
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    contentText = context.getString(R.string.alert_key_group_contact_message, user.fullName)
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_contact_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    contentText = context.getString(R.string.alert_key_contact_contact_message)
                }
            }
            message.isLocation() -> {
                if (conversation.isGroupConversation() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_location_message, user.fullName)
                    )
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    contentText = context.getString(R.string.alert_key_group_location_message, user.fullName)
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_location_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    contentText = context.getString(R.string.alert_key_contact_location_message)
                }
            }
            message.isPost() -> {
                if (conversation.isGroupConversation() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_post_message, user.fullName)
                    )
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    contentText = "${user.fullName}: ${rendMentionContent(message.content, userMap)}"
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_post_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    contentText = "${user.fullName}: ${rendMentionContent(message.content, userMap)}"
                }
            }
            message.isTranscript() -> {
                if (conversation.isGroupConversation() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_transcript_message, user.fullName)
                    )
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    contentText = context.getString(R.string.alert_key_group_transcript_message, user.fullName)
                } else {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_transcript_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    contentText = context.getString(R.string.alert_key_contact_transcript_message)
                }
            }
            message.type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name -> {
                if (message.action == SnapshotType.transfer.name) {
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_transfer_message))
                    notificationBuilder.setContentTitle(user.fullName)
                    contentText = context.getString(R.string.alert_key_contact_transfer_message)
                }
            }
            message.type == MessageCategory.APP_BUTTON_GROUP.name ||
                message.type == MessageCategory.APP_CARD.name -> {
                if (conversation.isGroupConversation() || message.isRepresentativeMessage(conversation)) {
                    notificationBuilder.setContentTitle(conversation.getConversationName())
                    notificationBuilder.setTicker(
                        context.getString(R.string.alert_key_group_text_message, user.fullName)
                    )
                    contentText = context.getString(R.string.alert_key_contact_text_message)
                } else {
                    notificationBuilder.setContentTitle(user.fullName)
                    notificationBuilder.setTicker(context.getString(R.string.alert_key_contact_text_message))
                    contentText = context.getString(R.string.alert_key_contact_text_message)
                }
            }
            message.type == MessageCategory.SYSTEM_CONVERSATION.name -> {
                notificationBuilder.setContentTitle(context.getString(R.string.app_name))
            }
            message.type == MessageCategory.WEBRTC_AUDIO_OFFER.name -> {
                notificationBuilder.setContentTitle(user.fullName)
                contentText = context.getString(R.string.alert_key_contact_audio_call_message)
            }
            else -> {
                // No support
                return@launch
            }
        }
        notificationBuilder.setContentText(contentText)
        notificationBuilder.setSmallIcon(R.drawable.ic_msg_default)
        notificationBuilder.color = ContextCompat.getColor(context, R.color.colorLightBlue)
        notificationBuilder.setWhen(System.currentTimeMillis())

        if (!isSilent && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setSound(Uri.parse("android.resource://" + context.packageName + "/" + R.raw.mixin))
        }
        notificationBuilder.setAutoCancel(true)
        notificationBuilder.priority = if (isSilent) {
            NotificationCompat.PRIORITY_MIN
        } else {
            NotificationCompat.PRIORITY_HIGH
        }

        var person: Person? = null
        supportsR({
            person = Person.Builder()
                .setName(conversation.getConversationName())
                .setImportant(true)
                .build()

            val messagingStyle = NotificationCompat.MessagingStyle(requireNotNull(person)).also { style ->
                style.addMessage(NotificationCompat.MessagingStyle.Message(contentText, System.currentTimeMillis(), person))
                style.isGroupConversation = false
            }

            notificationBuilder.setShortcutId(conversation.conversationId)
                .addPerson(person)
                .setLocusId(LocusIdCompat(conversation.conversationId))
                .setStyle(messagingStyle)
        })

        val canBubble = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && notificationManager.areBubblesAllowed()
        if (canBubble) {
            loadBitmap(context, conversation.iconUrl()) { bitmap ->
                val resource = bitmap ?: BitmapFactory.decodeResource(context.resources, R.drawable.ic_group_place_holder)
                notificationBuilder.setLargeIcon(resource)
                buildBubble(context, conversation, notificationBuilder, message, resource, person)
                notificationManager.notify(message.conversationId.hashCode(), notificationBuilder.build())
            }
        } else {
            user.notNullWithElse(
                {
                    loadBitmap(context, it.avatarUrl) { bitmap ->
                        val resource = bitmap ?: BitmapFactory.decodeResource(context.resources, R.drawable.default_avatar)
                        notificationBuilder.setLargeIcon(resource)
                        notificationManager.notify(message.conversationId.hashCode(), notificationBuilder.build())
                    }
                },
                {
                    notificationManager.notify(message.conversationId.hashCode(), notificationBuilder.build())
                }
            )
        }
    }

    private fun buildBubble(context: Context, conversation: ConversationItem, notificationBuilder: NotificationCompat.Builder, message: Message, bitmap: Bitmap, person: Person? = null) {
        supportsR({
            val icon = IconCompat.createWithBitmap(bitmap)
            val shortcut = ShortcutInfoCompat.Builder(context, conversation.conversationId)
                .setIntent(Intent(Intent.ACTION_DEFAULT))
                .setLongLived(true)
                .setIcon(icon)
                .setShortLabel(conversation.getConversationName()).apply {
                    person?.let { setPerson(it) }
                }
                .build()
            updateShortcuts(mutableListOf(shortcut))
            notificationBuilder.setShortcutInfo(shortcut)

            val userId = when {
                conversation.isGroupConversation() -> null
                message.isRepresentativeMessage(conversation) -> conversation.ownerId
                else -> message.userId
            }
            val target = BubbleActivity.putIntent(context, conversation.conversationId, userId)
            val bubbleIntent = PendingIntent.getActivity(
                context, conversation.conversationId.hashCode(), target,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(bubbleIntent, icon)
                .setDesiredHeight(640.dp)
                .setSuppressNotification(MixinApplication.conversationId == conversation.conversationId)
                .setAutoExpandBubble(false)
                .build()
            notificationBuilder.bubbleMetadata = bubbleMetadata
        })
    }

    private fun loadBitmap(context: Context, url: String?, onComplete: (Bitmap?) -> Unit) {
        context.mainThread {
            val height =
                context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
            val width =
                context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

            Glide.with(context)
                .asBitmap()
                .load(url)
                .apply(RequestOptions().fitCenter().circleCrop())
                .listener(
                    object : RequestListener<Bitmap> {
                        override fun onResourceReady(
                            resource: Bitmap,
                            model: Any?,
                            target: Target<Bitmap>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            onComplete(resource)
                            return false
                        }

                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            onComplete(null)
                            return false
                        }
                    }
                ).submit(width, height)
        }
    }
}
