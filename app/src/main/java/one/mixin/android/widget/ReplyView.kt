package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import one.mixin.android.R
import one.mixin.android.databinding.ViewReplyBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadImageCenterCrop
import one.mixin.android.extension.renderMessage
import one.mixin.android.extension.round
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.util.mention.MentionRenderCache
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import org.jetbrains.anko.dip

class ReplyView constructor(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    val binding = ViewReplyBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setBackgroundColor(context.colorFromAttribute(R.attr.bg_white))
        binding.replyViewIv.round(dip(3))
    }

    private val dp72 by lazy {
        context.dpToPx(72f)
    }
    private val dp12 by lazy {
        context.dpToPx(12f)
    }

    private fun setIcon(@DrawableRes icon: Int) = AppCompatResources.getDrawable(context, icon)?.also {
        it.setBounds(0, 0, dp12, dp12)
    }.let {
        TextViewCompat.setCompoundDrawablesRelative(binding.replyViewTv, it, null, null, null)
    }

    var messageItem: MessageItem? = null
    fun bind(messageItem: MessageItem) {
        this.messageItem = messageItem
        binding.replyStartView.setBackgroundColor(BaseViewHolder.getColorById(messageItem.userId))
        binding.replyNameTv.setTextColor(BaseViewHolder.getColorById(messageItem.userId))
        when {
            messageItem.type.endsWith("_IMAGE") -> {
                binding.replyNameTv.setText(R.string.photo)
                setIcon(R.drawable.ic_type_pic)
                binding.replyViewIv.loadImageCenterCrop(messageItem.mediaUrl, R.drawable.image_holder)
                (binding.replyNameTv.layoutParams as LayoutParams).endToStart = R.id.reply_view_iv
                binding.replyViewIv.visibility = View.VISIBLE
                binding.replyAvatar.visibility = View.GONE
            }
            messageItem.type.endsWith("_VIDEO") -> {
                binding.replyNameTv.setText(R.string.video)
                setIcon(R.drawable.ic_type_video)
                binding.replyViewIv.loadImageCenterCrop(messageItem.mediaUrl, R.drawable.image_holder)
                (binding.replyNameTv.layoutParams as LayoutParams).endToStart = R.id.reply_view_iv
                binding.replyViewIv.visibility = View.VISIBLE
                binding.replyAvatar.visibility = View.GONE
            }
            messageItem.type.endsWith("_LIVE") -> {
                binding.replyNameTv.setText(R.string.live)
                setIcon(R.drawable.ic_type_live)
                binding.replyViewIv.loadImageCenterCrop(messageItem.thumbUrl, R.drawable.image_holder)
                (binding.replyNameTv.layoutParams as LayoutParams).endToStart = R.id.reply_view_iv
                binding.replyViewIv.visibility = View.VISIBLE
                binding.replyAvatar.visibility = View.GONE
            }
            messageItem.type.endsWith("_STICKER") -> {
                binding.replyNameTv.setText(R.string.sticker)
                setIcon(R.drawable.ic_type_stiker)
                binding.replyViewIv.loadImageCenterCrop(messageItem.assetUrl, R.drawable.image_holder)
                (binding.replyNameTv.layoutParams as LayoutParams).endToStart = R.id.reply_view_iv
                binding.replyViewIv.visibility = View.VISIBLE
                binding.replyAvatar.visibility = View.GONE
            }
            messageItem.type.endsWith("_CONTACT") -> {
                binding.replyNameTv.setText(R.string.contact)
                setIcon(R.drawable.ic_type_contact)
                (binding.replyNameTv.layoutParams as LayoutParams).endToStart = R.id.reply_close_iv
                binding.replyAvatar.setInfo(
                    messageItem.sharedUserFullName,
                    messageItem.sharedUserAvatarUrl,
                    messageItem.sharedUserId
                        ?: "0"
                )
                binding.replyAvatar.visibility = View.VISIBLE
                binding.replyViewIv.visibility = View.INVISIBLE
            }
            messageItem.type.endsWith("_DATA") -> {
                binding.replyNameTv.setText(R.string.document)
                setIcon(R.drawable.ic_type_file)
                (binding.replyNameTv.layoutParams as LayoutParams).endToStart = R.id.reply_close_iv
                binding.replyViewIv.visibility = View.GONE
                binding.replyAvatar.visibility = View.GONE
            }
            messageItem.type.endsWith("_POST") -> {
                binding.replyNameTv.setText(R.string.post)
                setIcon(R.drawable.ic_type_file)
                (binding.replyNameTv.layoutParams as LayoutParams).endToStart = R.id.reply_close_iv
                binding.replyViewIv.visibility = View.GONE
                binding.replyAvatar.visibility = View.GONE
            }
            messageItem.type.endsWith("_AUDIO") -> {
                binding.replyNameTv.text = messageItem.mediaDuration?.toLongOrNull()?.formatMillis() ?: ""
                setIcon(R.drawable.ic_type_audio)
                (binding.replyNameTv.layoutParams as LayoutParams).endToStart = R.id.reply_close_iv
                binding.replyViewIv.visibility = View.GONE
                binding.replyAvatar.visibility = View.GONE
            }
            messageItem.type.endsWith("_LOCATION") -> {
                binding.replyNameTv.setText(R.string.location)
                setIcon(R.drawable.ic_type_location)
                (binding.replyNameTv.layoutParams as LayoutParams).endToStart = R.id.reply_close_iv
                binding.replyViewIv.visibility = View.GONE
                binding.replyAvatar.visibility = View.GONE
            }
            messageItem.type.endsWith("_TEXT") -> {
                if (messageItem.mentions?.isNotBlank() == true) {
                    val mentionRenderContext = MentionRenderCache.singleton.getMentionRenderContext(messageItem.mentions) { _ -> }
                    binding.replyNameTv.renderMessage(messageItem.content, mentionRenderContext)
                } else {
                    binding.replyNameTv.text = messageItem.content
                }
                TextViewCompat.setCompoundDrawablesRelative(binding.replyNameTv, null, null, null, null)
                (binding.replyNameTv.layoutParams as LayoutParams).endToStart = R.id.reply_close_iv
                binding.replyViewIv.visibility = View.GONE
                binding.replyAvatar.visibility = View.GONE
            }
            messageItem.type == MessageCategory.APP_CARD.name || messageItem.type == MessageCategory.APP_BUTTON_GROUP.name -> {
                binding.replyNameTv.setText(R.string.extensions)
                setIcon(R.drawable.ic_type_touch_app)
                (binding.replyNameTv.layoutParams as LayoutParams).endToStart = R.id.reply_close_iv
                binding.replyViewIv.visibility = View.GONE
                binding.replyAvatar.visibility = View.GONE
            }
        }
        binding.replyNameTv.text = messageItem.userFullName
    }
}
