package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.widget.TextViewCompat
import kotlinx.android.synthetic.main.item_chat_image.view.*
import kotlinx.android.synthetic.main.item_chat_image.view.chat_image
import kotlinx.android.synthetic.main.item_chat_image.view.chat_layout
import kotlinx.android.synthetic.main.item_chat_image.view.chat_name
import kotlinx.android.synthetic.main.item_chat_image.view.chat_time
import kotlinx.android.synthetic.main.item_chat_image.view.chat_warning
import kotlinx.android.synthetic.main.item_chat_image.view.progress
import kotlinx.android.synthetic.main.item_chat_video.view.*
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImageMark
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.realSize
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.ShareImageData

class ShareImageRenderer(val context: Context) : ShareMessageRenderer {

    val contentView: View = LayoutInflater.from(context).inflate(R.layout.item_chat_image, null)

    private val mediaWidth by lazy {
        (context.realSize().x * 0.6).toInt()
    }

    init {
        contentView.chat_image.setShape(R.drawable.chat_mark_image_me)
        contentView.chat_name.visibility = View.GONE
        contentView.chat_warning.visibility = View.GONE
        contentView.progress.visibility = View.GONE
        (contentView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER
        (contentView.chat_time.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
        (contentView.chat_time.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 10.dp
        contentView.chat_image.layoutParams.width = mediaWidth
        contentView.chat_image.adjustViewBounds = true
        contentView.chat_image.scaleType = ImageView.ScaleType.FIT_XY
        setStatusIcon(context, MessageStatus.DELIVERED.name, isSecret = true, isWhite = true) { statusIcon, secretIcon ->
            statusIcon?.setBounds(0, 0, 12.dp, 12.dp)
            secretIcon?.setBounds(0, 0, 8.dp, 8.dp)
            TextViewCompat.setCompoundDrawablesRelative(contentView.chat_time, secretIcon, null, statusIcon, null)
        }
    }

    fun render(data: ShareImageData) {
        contentView.chat_image.loadImageMark(data.url, R.drawable.image_holder, R.drawable.chat_mark_image_me)
        contentView.chat_time.timeAgoClock(nowInUtc())
    }
}