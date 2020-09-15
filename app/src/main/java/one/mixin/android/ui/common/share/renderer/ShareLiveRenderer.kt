package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import kotlinx.android.synthetic.main.item_chat_video.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImageMark
import one.mixin.android.extension.realSize
import one.mixin.android.vo.MessageStatus
import one.mixin.android.websocket.LiveMessagePayload
import one.mixin.android.extension.dp
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.timeAgoClock

open class ShareLiveRenderer(val context: Context) : ShareMessageRenderer {
    val contentView: View = LayoutInflater.from(context).inflate(R.layout.item_chat_video, null)

    private val mediaWidth by lazy {
        (context.realSize().x * 0.6).toInt()
    }

    fun render(data: LiveMessagePayload) {
        contentView.chat_name.visibility = View.GONE
        contentView.chat_warning.visibility = View.GONE
        contentView.duration_tv.visibility = View.GONE
        contentView.progress.visibility = View.GONE
        contentView.play.isVisible = true
        contentView.live_tv.visibility = View.VISIBLE
        (contentView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
        (contentView.chat_image_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
        (contentView.duration_tv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
        (contentView.live_tv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
        (contentView.chat_time.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
        contentView.chat_image.layoutParams.width = mediaWidth
        contentView.chat_image.layoutParams.height = mediaWidth * data.height / data.width
        contentView.chat_image.loadImageMark(data.thumbUrl, R.drawable.image_holder, R.drawable.chat_mark_image_me)
        contentView.chat_time.timeAgoClock(nowInUtc())
        setStatusIcon(context, MessageStatus.DELIVERED.name, isSecret = true, isWhite = true) { statusIcon, secretIcon ->
            statusIcon?.setBounds(0, 0, 12.dp, 12.dp)
            secretIcon?.setBounds(0, 0, 8.dp, 8.dp)
            TextViewCompat.setCompoundDrawablesRelative(contentView.chat_time, secretIcon, null, statusIcon, null)
        }
    }
}