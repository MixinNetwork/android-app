package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import androidx.appcompat.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import kotlinx.android.synthetic.main.view_reply.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadImageCenterCrop
import one.mixin.android.extension.round
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import org.jetbrains.anko.dip

class ReplyView constructor(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_reply, this, true)
        setBackgroundColor(Color.WHITE)
        reply_view_iv.round(dip(3))
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
        TextViewCompat.setCompoundDrawablesRelative(reply_view_tv, it, null, null, null)
    }

    var messageItem: MessageItem? = null
    fun bind(messageItem: MessageItem) {
        this.messageItem = messageItem
        reply_start_view.setBackgroundColor(BaseViewHolder.colors[messageItem.userIdentityNumber.toLong().rem(BaseViewHolder.colors.size).toInt()])
        reply_name_tv.setTextColor(BaseViewHolder.colors[messageItem.userIdentityNumber.toLong().rem(BaseViewHolder.colors.size).toInt()])
        when {
            messageItem.type.endsWith("_IMAGE") -> {
                reply_view_tv.setText(R.string.photo)
                setIcon(R.drawable.ic_status_pic)
                reply_view_iv.loadImageCenterCrop(messageItem.mediaUrl, R.drawable.image_holder)
                (reply_view_tv.layoutParams as ConstraintLayout.LayoutParams).endToStart = R.id.reply_view_iv
                reply_view_iv.visibility = View.VISIBLE
                reply_avatar.visibility = View.GONE
            }
            messageItem.type.endsWith("_VIDEO") -> {
                reply_view_tv.setText(R.string.video)
                setIcon(R.drawable.ic_status_video)
                reply_view_iv.loadImageCenterCrop(messageItem.mediaUrl, R.drawable.image_holder)
                (reply_view_tv.layoutParams as ConstraintLayout.LayoutParams).endToStart = R.id.reply_view_iv
                reply_view_iv.visibility = View.VISIBLE
                reply_avatar.visibility = View.GONE
            }
            messageItem.type.endsWith("_STICKER") -> {
                reply_view_tv.setText(R.string.sticker)
                setIcon(R.drawable.ic_status_stiker)
                reply_view_iv.loadImageCenterCrop(messageItem.assetUrl, R.drawable.image_holder)
                (reply_view_tv.layoutParams as ConstraintLayout.LayoutParams).endToStart = R.id.reply_view_iv
                reply_view_iv.visibility = View.VISIBLE
                reply_avatar.visibility = View.GONE
            }
            messageItem.type.endsWith("_CONTACT") -> {
                reply_view_tv.setText(R.string.contact)
                setIcon(R.drawable.ic_status_contact)
                (reply_view_tv.layoutParams as ConstraintLayout.LayoutParams).endToStart = R.id.reply_close_iv
                reply_avatar.setInfo(messageItem.sharedUserFullName, messageItem.sharedUserAvatarUrl, messageItem.sharedUserIdentityNumber
                    ?: "0")
                reply_avatar.visibility = View.VISIBLE
                reply_view_iv.visibility = View.INVISIBLE
            }
            messageItem.type.endsWith("_DATA") -> {
                reply_view_tv.setText(R.string.document)
                setIcon(R.drawable.ic_status_file)
                (reply_view_tv.layoutParams as ConstraintLayout.LayoutParams).endToStart = R.id.reply_close_iv
                reply_view_iv.visibility = View.GONE
                reply_avatar.visibility = View.GONE
            }
            messageItem.type.endsWith("_AUDIO") -> {
                reply_view_tv.text = messageItem.mediaDuration!!.toLong().formatMillis()
                setIcon(R.drawable.ic_status_audio)
                (reply_view_tv.layoutParams as ConstraintLayout.LayoutParams).endToStart = R.id.reply_close_iv
                reply_view_iv.visibility = View.GONE
                reply_avatar.visibility = View.GONE
            }
            messageItem.type.endsWith("_TEXT") -> {
                reply_view_tv.text = messageItem.content
                TextViewCompat.setCompoundDrawablesRelative(reply_view_tv, null, null, null, null)
                (reply_view_tv.layoutParams as ConstraintLayout.LayoutParams).endToStart = R.id.reply_close_iv
                reply_view_iv.visibility = View.GONE
                reply_avatar.visibility = View.GONE
            }
            messageItem.type == MessageCategory.APP_CARD.name || messageItem.type == MessageCategory.APP_BUTTON_GROUP.name -> {
                reply_view_tv.setText(R.string.extensions)
                setIcon(R.drawable.ic_touch_app)
                (reply_view_tv.layoutParams as ConstraintLayout.LayoutParams).endToStart = R.id.reply_close_iv
                reply_view_iv.visibility = View.GONE
                reply_avatar.visibility = View.GONE
            }
        }
        reply_name_tv.text = messageItem.userFullName
    }
}
