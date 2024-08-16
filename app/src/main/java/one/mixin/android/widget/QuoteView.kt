package one.mixin.android.widget

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.core.widget.TextViewCompat
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.ViewQuoteBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.generateConversationPath
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.renderMessage
import one.mixin.android.extension.round
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.mention.MentionRenderCache
import one.mixin.android.vo.ActionButtonData
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.membershipIcon
import timber.log.Timber
import java.io.File

class QuoteView constructor(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs) {
        private val binding = ViewQuoteBinding.inflate(LayoutInflater.from(context), this)

        init {
            round(4.dp)
        }

        private var iconSize = context.dpToPx(10f)

        fun changeSize(size: Float) {
            binding.replyNameTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
            binding.replyContentTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 2f)
            (binding.replyAvatar.layoutParams as LayoutParams).apply {
                this.matchConstraintMaxWidth = context.dpToPx(26 + size)
            }
            iconSize = context.dpToPx(size - 4f)
        }

        fun bind(quoteMessageItem: QuoteMessageItem?) {
            if (quoteMessageItem == null) {
                setBackgroundColor(context.getColor(R.color.colorAccent))
                background.alpha = 0x0D
                binding.startView.setBackgroundColor(context.getColor(R.color.colorAccent))
                binding.replyNameTv.visibility = View.GONE
                binding.replyIv.visibility = View.GONE
                binding.replyAvatar.visibility = View.GONE
                binding.replyContentTv.setText(R.string.Message_not_found)
                binding.replyContentTv.setTypeface(null, Typeface.ITALIC)
                setIcon(R.drawable.ic_type_recall)
                return
            }
            binding.replyContentTv.setTypeface(null, Typeface.NORMAL)
            binding.replyNameTv.visibility = View.VISIBLE
            binding.replyNameTv.text = quoteMessageItem.userFullName
            binding.replyNameTv.setTextColor(BaseViewHolder.getColorById(quoteMessageItem.userId))
            binding.replyNameTv.setCompoundDrawables(null, null, getMembershipBadge(quoteMessageItem), null)
            setBackgroundColor(BaseViewHolder.getColorById(quoteMessageItem.userId))
            background.alpha = 0x0D
            binding.startView.setBackgroundColor(BaseViewHolder.getColorById(quoteMessageItem.userId))
            when {
                quoteMessageItem.type.endsWith("_TEXT") -> {
                    if (quoteMessageItem.mentions != null) {
                        binding.replyContentTv.renderMessage(
                            quoteMessageItem.content,
                            MentionRenderCache.singleton.getMentionRenderContext(quoteMessageItem.mentions),
                        )
                    } else {
                        binding.replyContentTv.text = quoteMessageItem.content
                    }
                    binding.replyIv.visibility = View.GONE
                    binding.replyAvatar.visibility = View.GONE
                    (binding.replyContentTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                    (binding.replyNameTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                    setIcon()
                }
                quoteMessageItem.type == MessageCategory.MESSAGE_RECALL.name -> {
                    binding.replyContentTv.setText(
                        if (quoteMessageItem.userId == Session.getAccountId()) {
                            R.string.You_deleted_this_message
                        } else {
                            R.string.This_message_was_deleted
                        },
                    )
                    binding.replyIv.visibility = View.GONE
                    binding.replyAvatar.visibility = View.GONE
                    (binding.replyContentTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                    (binding.replyNameTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                    setIcon(R.drawable.ic_type_recall)
                }
                quoteMessageItem.type.endsWith("_IMAGE") -> {
                    binding.replyIv.loadImage(
                        absolutePath(quoteMessageItem.mediaUrl, quoteMessageItem.type, quoteMessageItem.conversationId),
                        R.drawable.image_holder,
                    )
                    binding.replyContentTv.setText(R.string.Photo)
                    setIcon(R.drawable.ic_type_pic)
                    binding.replyIv.visibility = View.VISIBLE
                    binding.replyAvatar.visibility = View.GONE
                    (binding.replyContentTv.layoutParams as LayoutParams).marginEnd =
                        16.dp
                    (binding.replyNameTv.layoutParams as LayoutParams).marginEnd =
                        16.dp
                }
                quoteMessageItem.type.endsWith("_VIDEO") -> {
                    binding.replyIv.loadImage(
                        absolutePath(quoteMessageItem.mediaUrl, quoteMessageItem.type, quoteMessageItem.conversationId),
                        R.drawable.image_holder,
                    )
                    binding.replyContentTv.setText(R.string.Video)
                    setIcon(R.drawable.ic_type_video)
                    binding.replyIv.visibility = View.VISIBLE
                    binding.replyAvatar.visibility = View.GONE
                    (binding.replyContentTv.layoutParams as LayoutParams).marginEnd =
                        16.dp
                    (binding.replyNameTv.layoutParams as LayoutParams).marginEnd =
                        16.dp
                }
                quoteMessageItem.type.endsWith("_LIVE") -> {
                    binding.replyIv.loadImage(
                        quoteMessageItem.thumbUrl,
                        R.drawable.image_holder,
                    )
                    binding.replyContentTv.setText(R.string.Live)
                    setIcon(R.drawable.ic_type_live)
                    binding.replyIv.visibility = View.VISIBLE
                    binding.replyAvatar.visibility = View.GONE
                    (binding.replyContentTv.layoutParams as LayoutParams).marginEnd =
                        16.dp
                    (binding.replyNameTv.layoutParams as LayoutParams).marginEnd =
                        16.dp
                }
                quoteMessageItem.type.endsWith("_DATA") -> {
                    val mediaName = quoteMessageItem.mediaName
                    if (mediaName != null) {
                        binding.replyContentTv.text = mediaName
                    } else {
                        binding.replyContentTv.setText(R.string.File)
                    }
                    setIcon(R.drawable.ic_type_file)
                    binding.replyIv.visibility = View.GONE
                    binding.replyAvatar.visibility = View.GONE
                    (binding.replyContentTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                    (binding.replyNameTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                }
                quoteMessageItem.type.endsWith("_POST") -> {
                    binding.replyContentTv.setText(R.string.Post)
                    setIcon(R.drawable.ic_type_file)
                    binding.replyIv.visibility = View.GONE
                    binding.replyAvatar.visibility = View.GONE
                    (binding.replyContentTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                    (binding.replyNameTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                }
                quoteMessageItem.type.endsWith("_TRANSCRIPT") -> {
                    binding.replyContentTv.setText(R.string.Transcript)
                    setIcon(R.drawable.ic_type_transcript)
                    binding.replyIv.visibility = View.GONE
                    binding.replyAvatar.visibility = View.GONE
                    (binding.replyContentTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                    (binding.replyNameTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                }
                quoteMessageItem.type.endsWith("_LOCATION") -> {
                    binding.replyContentTv.setText(R.string.Location)
                    setIcon(R.drawable.ic_type_location)
                    binding.replyIv.visibility = View.GONE
                    binding.replyAvatar.visibility = View.GONE
                    (binding.replyContentTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                    (binding.replyNameTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                }
                quoteMessageItem.type.endsWith("_AUDIO") -> {
                    val mediaDuration = quoteMessageItem.mediaDuration
                    if (mediaDuration != null) {
                        binding.replyContentTv.text = mediaDuration.toLong().formatMillis()
                    } else {
                        binding.replyContentTv.setText(R.string.Audio)
                    }
                    setIcon(R.drawable.ic_type_audio)
                    binding.replyIv.visibility = View.GONE
                    binding.replyAvatar.visibility = View.GONE
                    (binding.replyContentTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                    (binding.replyNameTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                }
                quoteMessageItem.type.endsWith("_STICKER") -> {
                    binding.replyContentTv.setText(R.string.Sticker)
                    setIcon(R.drawable.ic_type_stiker)
                    binding.replyIv.loadImage(
                        quoteMessageItem.assetUrl,
                        R.drawable.image_holder,
                    )
                    binding.replyIv.visibility = View.VISIBLE
                    binding.replyAvatar.visibility = View.GONE
                    (binding.replyContentTv.layoutParams as LayoutParams).marginEnd =
                        16.dp
                    (binding.replyNameTv.layoutParams as LayoutParams).marginEnd =
                        16.dp
                }
                quoteMessageItem.type.endsWith("_CONTACT") -> {
                    binding.replyContentTv.text = quoteMessageItem.sharedUserIdentityNumber
                    setIcon(R.drawable.ic_type_contact)
                    binding.replyAvatar.setInfo(
                        quoteMessageItem.sharedUserFullName,
                        quoteMessageItem.sharedUserAvatarUrl,
                        quoteMessageItem.sharedUserId
                            ?: "0",
                    )
                    binding.replyAvatar.visibility = View.VISIBLE
                    binding.replyIv.visibility = View.INVISIBLE
                    (binding.replyContentTv.layoutParams as LayoutParams).marginEnd =
                        16.dp
                    (binding.replyNameTv.layoutParams as LayoutParams).marginEnd =
                        16.dp
                }
                quoteMessageItem.type == MessageCategory.APP_BUTTON_GROUP.name || quoteMessageItem.type == MessageCategory.APP_CARD.name -> {
                    try {
                        if (quoteMessageItem.type == MessageCategory.APP_CARD.name) {
                            val appCard = GsonHelper.customGson.fromJson(quoteMessageItem.content, AppCardData::class.java)
                            binding.replyContentTv.text = appCard.title
                        } else if (quoteMessageItem.type == MessageCategory.APP_BUTTON_GROUP.name) {
                            val buttons = GsonHelper.customGson.fromJson(quoteMessageItem.content, Array<ActionButtonData>::class.java)
                            var content = ""
                            buttons.map { content += "[" + it.label + "]" }
                            binding.replyContentTv.text = content
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                    setIcon(R.drawable.ic_type_touch_app)
                    binding.replyIv.visibility = View.GONE
                    binding.replyAvatar.visibility = View.GONE
                    (binding.replyContentTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                    (binding.replyNameTv.layoutParams as LayoutParams).marginEnd =
                        8.dp
                }
                else -> {
                    binding.replyIv.visibility = View.GONE
                }
            }
        }

        private val dp12 by lazy {
            context.dpToPx(12f)
        }

        private fun getMembershipBadge(messageItem: QuoteMessageItem?): Drawable? {
            if (messageItem?.membership?.isMembership() != true) return null
            return messageItem.membership.membershipIcon().let { icon ->
                if (icon == View.NO_ID) {
                    null
                } else {
                    AppCompatResources.getDrawable(context, icon)?.also {
                        it.setBounds(0, 0, dp12, dp12)
                    }
                }
            }
        }

        private fun setIcon(
            @DrawableRes icon: Int? = null,
        ) {
            if (icon != null) {
                AppCompatResources.getDrawable(context, icon).let {
                    it?.setBounds(0, 0, iconSize, iconSize)
                    TextViewCompat.setCompoundDrawablesRelative(
                        binding.replyContentTv,
                        it,
                        null,
                        null,
                        null,
                    )
                }
            } else {
                TextViewCompat.setCompoundDrawablesRelative(
                    binding.replyContentTv,
                    null,
                    null,
                    null,
                    null,
                )
            }
        }

        fun absolutePath(
            mediaUrl: String?,
            type: String,
            conversationId: String,
            context: Context = MixinApplication.appContext,
        ): String? {
            val mediaPath = context.getMediaPath()?.toUri()?.toString() ?: return null
            return when {
                mediaUrl == null -> null
                mediaUrl.startsWith(mediaPath) -> mediaUrl
                type == MessageCategory.SIGNAL_IMAGE.name || type == MessageCategory.PLAIN_IMAGE.name || type == MessageCategory.ENCRYPTED_IMAGE.name ->
                    File(
                        context.getImagePath().generateConversationPath(conversationId),
                        mediaUrl,
                    ).toUri().toString()
                type == MessageCategory.SIGNAL_VIDEO.name || type == MessageCategory.PLAIN_VIDEO.name || type == MessageCategory.ENCRYPTED_VIDEO.name ->
                    File(
                        context.getVideoPath().generateConversationPath(conversationId),
                        mediaUrl,
                    ).toUri().toString()
                else -> null
            }
        }
    }
