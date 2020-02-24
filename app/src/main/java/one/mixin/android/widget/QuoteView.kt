package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import kotlinx.android.synthetic.main.view_quote.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadImageCenterCrop
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.renderConversation
import one.mixin.android.extension.round
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.util.mention.MentionRenderCache
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.QuoteMessageItem
import org.jetbrains.anko.dip

class QuoteView constructor(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_quote, this, true)
        round(dip(4))
    }

    fun bind(quoteMessageItem: QuoteMessageItem) {
        reply_name_tv.text = quoteMessageItem.userFullName
        reply_name_tv.setTextColor(BaseViewHolder.getColorById(quoteMessageItem.userId))
        setBackgroundColor(BaseViewHolder.getColorById(quoteMessageItem.userId))
        background.alpha = 0x0D
        start_view.setBackgroundColor(BaseViewHolder.getColorById(quoteMessageItem.userId))
        when {
            quoteMessageItem.type.endsWith("_TEXT") -> {
                if (quoteMessageItem.mentions != null) {
                    reply_content_tv.renderConversation(quoteMessageItem.content, MentionRenderCache.singleton.getMentionRenderContext(quoteMessageItem.mentions) {})
                } else {
                    reply_content_tv.text = quoteMessageItem.content
                }
                reply_iv.visibility = View.GONE
                reply_avatar.visibility = View.GONE
                (reply_content_tv.layoutParams as LayoutParams).marginEnd =
                    dip(8)
                (reply_name_tv.layoutParams as LayoutParams).marginEnd =
                    dip(8)
                setIcon()
            }
            quoteMessageItem.type == MessageCategory.MESSAGE_RECALL.name -> {
                reply_content_tv.setText(R.string.chat_recall_me)
                reply_iv.visibility = View.GONE
                reply_avatar.visibility = View.GONE
                (reply_content_tv.layoutParams as LayoutParams).marginEnd =
                    dip(8)
                (reply_name_tv.layoutParams as LayoutParams).marginEnd =
                    dip(8)
                setIcon(R.drawable.ic_status_recall)
            }
            quoteMessageItem.type.endsWith("_IMAGE") -> {
                reply_iv.loadImageCenterCrop(
                    quoteMessageItem.mediaUrl,
                    R.drawable.image_holder
                )
                reply_content_tv.setText(R.string.photo)
                setIcon(R.drawable.ic_status_pic)
                reply_iv.visibility = View.VISIBLE
                reply_avatar.visibility = View.GONE
                (reply_content_tv.layoutParams as LayoutParams).marginEnd =
                    dip(16)
                (reply_name_tv.layoutParams as LayoutParams).marginEnd =
                    dip(16)
            }
            quoteMessageItem.type.endsWith("_VIDEO") -> {
                reply_iv.loadImageCenterCrop(
                    quoteMessageItem.mediaUrl,
                    R.drawable.image_holder
                )
                reply_content_tv.setText(R.string.video)
                setIcon(R.drawable.ic_status_video)
                reply_iv.visibility = View.VISIBLE
                reply_avatar.visibility = View.GONE
                (reply_content_tv.layoutParams as LayoutParams).marginEnd =
                    dip(16)
                (reply_name_tv.layoutParams as LayoutParams).marginEnd =
                    dip(16)
            }
            quoteMessageItem.type.endsWith("_LIVE") -> {
                reply_iv.loadImageCenterCrop(
                    quoteMessageItem.thumbUrl,
                    R.drawable.image_holder
                )
                reply_content_tv.setText(R.string.live)
                setIcon(R.drawable.ic_status_live)
                reply_iv.visibility = View.VISIBLE
                reply_avatar.visibility = View.GONE
                (reply_content_tv.layoutParams as LayoutParams).marginEnd =
                    dip(16)
                (reply_name_tv.layoutParams as LayoutParams).marginEnd =
                    dip(16)
            }
            quoteMessageItem.type.endsWith("_DATA") -> {
                quoteMessageItem.mediaName.notNullWithElse({
                    reply_content_tv.text = it
                }, {
                    reply_content_tv.setText(R.string.document)
                })
                setIcon(R.drawable.ic_status_file)
                reply_iv.visibility = View.GONE
                reply_avatar.visibility = View.GONE
                (reply_content_tv.layoutParams as LayoutParams).marginEnd =
                    dip(8)
                (reply_name_tv.layoutParams as LayoutParams).marginEnd =
                    dip(8)
            }
            quoteMessageItem.type.endsWith("_POST") -> {
                reply_content_tv.setText(R.string.post)
                setIcon(R.drawable.ic_status_file)
                reply_iv.visibility = View.GONE
                reply_avatar.visibility = View.GONE
                (reply_content_tv.layoutParams as LayoutParams).marginEnd =
                    dip(8)
                (reply_name_tv.layoutParams as LayoutParams).marginEnd =
                    dip(8)
            }
            quoteMessageItem.type.endsWith("_AUDIO") -> {
                quoteMessageItem.mediaDuration.notNullWithElse({
                    reply_content_tv.text = it.toLong().formatMillis()
                }, {
                    reply_content_tv.setText(R.string.audio)
                })
                setIcon(R.drawable.ic_status_audio)
                reply_iv.visibility = View.GONE
                reply_avatar.visibility = View.GONE
                (reply_content_tv.layoutParams as LayoutParams).marginEnd =
                    dip(8)
                (reply_name_tv.layoutParams as LayoutParams).marginEnd =
                    dip(8)
            }
            quoteMessageItem.type.endsWith("_STICKER") -> {
                reply_content_tv.setText(R.string.conversation_status_sticker)
                setIcon(R.drawable.ic_status_stiker)
                reply_iv.loadImageCenterCrop(
                    quoteMessageItem.assetUrl,
                    R.drawable.image_holder
                )
                reply_iv.visibility = View.VISIBLE
                reply_avatar.visibility = View.GONE
                (reply_content_tv.layoutParams as LayoutParams).marginEnd =
                    dip(16)
                (reply_name_tv.layoutParams as LayoutParams).marginEnd =
                    dip(16)
            }
            quoteMessageItem.type.endsWith("_CONTACT") -> {
                reply_content_tv.text = quoteMessageItem.sharedUserIdentityNumber
                setIcon(R.drawable.ic_status_contact)
                reply_avatar.setInfo(
                    quoteMessageItem.sharedUserFullName,
                    quoteMessageItem.sharedUserAvatarUrl,
                    quoteMessageItem.sharedUserId
                        ?: "0"
                )
                reply_avatar.visibility = View.VISIBLE
                reply_iv.visibility = View.INVISIBLE
                (reply_content_tv.layoutParams as LayoutParams).marginEnd =
                    dip(16)
                (reply_name_tv.layoutParams as LayoutParams).marginEnd =
                    dip(16)
            }
            quoteMessageItem.type == MessageCategory.APP_BUTTON_GROUP.name || quoteMessageItem.type == MessageCategory.APP_CARD.name -> {
                reply_content_tv.setText(R.string.extensions)
                setIcon(R.drawable.ic_touch_app)
                reply_iv.visibility = View.GONE
                reply_avatar.visibility = View.GONE
                (reply_content_tv.layoutParams as LayoutParams).marginEnd =
                    dip(8)
                (reply_name_tv.layoutParams as LayoutParams).marginEnd =
                    dip(8)
            }
            else -> {
                reply_iv.visibility = View.GONE
            }
        }
    }

    private fun setIcon(@DrawableRes icon: Int? = null) {
        icon.notNullWithElse({ drawable ->
            AppCompatResources.getDrawable(context, drawable).let {
                it?.setBounds(0, 0, context.dpToPx(10f), context.dpToPx(10f))
                TextViewCompat.setCompoundDrawablesRelative(
                    reply_content_tv,
                    it,
                    null,
                    null,
                    null
                )
            }
        }, {
            TextViewCompat.setCompoundDrawablesRelative(
                reply_content_tv,
                null,
                null,
                null,
                null
            )
        })
    }
}
