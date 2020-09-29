package one.mixin.android.ui.common.share.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import kotlinx.android.synthetic.main.item_chat_action.view.chat_name
import kotlinx.android.synthetic.main.item_chat_post.view.*
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.postOptimize
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.markdown.MarkdownActivity
import one.mixin.android.util.markdown.MarkwonUtil
import one.mixin.android.vo.MessageStatus

open class SharePostRenderer(val context: Activity) : ShareMessageRenderer {

    val contentView: View = LayoutInflater.from(context).inflate(R.layout.item_chat_post, null)

    init {
        contentView.chat_tv.layoutParams.width = context.maxItemWidth() * 2 / 3
        contentView.chat_tv.maxHeight = context.maxItemWidth() / 2 * 10 / 16
        contentView.chat_tv.round(3.dp)
        contentView.chat_name.visibility = View.GONE
        (contentView.chat_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0.5f
        (contentView.chat_time.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 12.dp
        (contentView.chat_post.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 12.dp
        (contentView.chat_tv.layoutParams as ViewGroup.MarginLayoutParams).apply {
            marginStart = 8.dp
            marginEnd = 14.dp
        }
    }

    private val miniMarkwon by lazy {
        MarkwonUtil.getMiniMarkwon(context)
    }

    fun render(content: String, isNightMode: Boolean) {
        miniMarkwon.setMarkdown(contentView.chat_tv, content.postOptimize())
        contentView.chat_time.timeAgoClock(nowInUtc())
        setStatusIcon(context, MessageStatus.DELIVERED.name, isSecret = true, isWhite = true) { statusIcon, secretIcon ->
            statusIcon?.setBounds(0, 0, 12.dp, 12.dp)
            secretIcon?.setBounds(0, 0, 8.dp, 8.dp)
            TextViewCompat.setCompoundDrawablesRelative(contentView.chat_time, secretIcon, null, statusIcon, null)
        }
        contentView.chat_tv.setOnClickListener {
            MarkdownActivity.show(context, content)
        }
        contentView.chat_layout.setBackgroundResource(
            if (!isNightMode) {
                R.drawable.chat_bubble_post_me_last
            } else {
                R.drawable.chat_bubble_post_me_last_night
            }
        )
    }
}
