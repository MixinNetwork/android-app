package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.constraint.ConstraintLayout
import android.support.v4.widget.TextViewCompat
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import kotlinx.android.synthetic.main.view_reply.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem

class ReplyView constructor(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_reply, this, true)
        setBackgroundColor(Color.WHITE)
    }

    private val dp72 by lazy {
        context.dpToPx(72f)
    }
    private val dp12 by lazy {
        context.dpToPx(12f)
    }

    private val icon: Drawable? by lazy {
        AppCompatResources.getDrawable(context, R.drawable.ic_status_camera)?.also {
            it.setBounds(0, 0, dp12, dp12)
        }
    }

    fun bind(messageItem: MessageItem) {
        if (messageItem.type == MessageCategory.SIGNAL_IMAGE.name) {
            reply_view_tv.setText(R.string.photo)
            icon?.let {
                TextViewCompat.setCompoundDrawablesRelative(reply_view_tv, it, null, null, null)
            }
            reply_view_iv.loadImage(messageItem.mediaUrl, dp72, dp72)
            reply_view_iv.visibility = View.VISIBLE
        } else if (messageItem.type == MessageCategory.SIGNAL_TEXT.name) {
            reply_view_tv.text = messageItem.content
            TextViewCompat.setCompoundDrawablesRelative(reply_view_tv, null, null, null, null)
            reply_view_iv.visibility = View.GONE
        }
        reply_name_tv.text = messageItem.userFullName
    }
}
