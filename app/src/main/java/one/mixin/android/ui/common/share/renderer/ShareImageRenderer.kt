package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import kotlinx.android.synthetic.main.item_chat_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImageMark
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.realSize
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.ShareImageData

open class ShareImageRenderer(val context: Context) : ShareMessageRenderer {

    val contentView: View = LayoutInflater.from(context).inflate(R.layout.item_chat_image, null)

    private val mediaWidth by lazy {
        (context.realSize().x * 0.6).toInt()
    }

    fun render(data: ShareImageData) {
        contentView.chat_name.visibility = View.GONE
        contentView.chat_warning.visibility = View.GONE
        contentView.progress.visibility = View.GONE
        (contentView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
        (contentView.chat_image_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
        (contentView.chat_time.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
        contentView.chat_image.layoutParams.width = mediaWidth
        // Todo
        contentView.chat_image.layoutParams.height = mediaWidth
        contentView.chat_image.loadImageMark(data.url, R.drawable.image_holder, R.drawable.chat_mark_image_me)
        contentView.chat_time.timeAgoClock(nowInUtc())
        setStatusIcon(context, MessageStatus.DELIVERED.name, isSecret = true, isWhite = true) { statusIcon, secretIcon ->
            statusIcon?.setBounds(0, 0, 12.dp, 12.dp)
            secretIcon?.setBounds(0, 0, 8.dp, 8.dp)
            TextViewCompat.setCompoundDrawablesRelative(contentView.chat_time, secretIcon, null, statusIcon, null)
        }
    }
}