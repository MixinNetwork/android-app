package one.mixin.android.ui.common.share.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatPostBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.postOptimize
import one.mixin.android.extension.round
import one.mixin.android.ui.conversation.markdown.MarkdownActivity
import one.mixin.android.util.markdown.MarkwonUtil
import one.mixin.android.vo.MessageStatus

open class SharePostRenderer(val context: Activity) {

    private val binding = ItemChatPostBinding.inflate(LayoutInflater.from(context), null, false)
    val contentView get() = binding.root

    init {
        binding.chatTv.layoutParams.width = context.maxItemWidth() * 2 / 3
        binding.chatTv.maxHeight = context.maxItemWidth() / 2 * 10 / 16
        binding.chatTv.round(3.dp)
        binding.chatName.visibility = View.GONE
        (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0.5f
        (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 12.dp
        (binding.chatPost.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 12.dp
        (binding.chatTv.layoutParams as ViewGroup.MarginLayoutParams).apply {
            marginStart = 8.dp
            marginEnd = 14.dp
        }
    }

    private val miniMarkwon by lazy {
        MarkwonUtil.getMiniMarkwon(context)
    }

    fun render(content: String, isNightMode: Boolean) {
        miniMarkwon.setMarkdown(binding.chatTv, content.postOptimize())
        binding.chatTime.load(
            true,
            nowInUtc(),
            MessageStatus.DELIVERED.name,
            isPin = false,
            isRepresentative = false,
            isSecret = true,
            isWhite = true
        )
        binding.chatTv.setOnClickListener {
            MarkdownActivity.show(context, content)
        }
        binding.chatLayout.setBackgroundResource(
            if (!isNightMode) {
                R.drawable.chat_bubble_post_me_last
            } else {
                R.drawable.chat_bubble_post_me_last_night
            }
        )
    }
}
