package one.mixin.android.ui.media

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.noties.markwon.Markwon
import one.mixin.android.R
import one.mixin.android.databinding.ItemPostBinding
import one.mixin.android.extension.postLengthOptimize
import one.mixin.android.extension.postOptimize
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.util.markdown.MarkwonUtil
import one.mixin.android.vo.MessageItem

class PostAdapter(
    private val context: Activity,
    private val onClickListener: (messageItem: MessageItem) -> Unit
) : SharedMediaHeaderAdapter<PostHolder>() {
    private val miniMarkwon by lazy {
        MarkwonUtil.getMiniMarkwon(context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PostHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_post,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        getItem(position)?.let { item ->
            holder.bind(item, miniMarkwon)
            holder.chatTv.setOnClickListener {
                onClickListener(item)
            }
        }
    }

    override fun getHeaderTextMargin() = 20f
}

class PostHolder(itemView: View) : NormalHolder(itemView) {
    private val binding = ItemPostBinding.bind(itemView)
    val chatTv get() = binding.chatTv
    fun bind(item: MessageItem, miniMarkwon: Markwon) {
        if (chatTv.tag != item.content.hashCode()) {
            if (!item.thumbImage.isNullOrEmpty()) {
                miniMarkwon.setMarkdown(chatTv, item.thumbImage.postLengthOptimize())
                chatTv.tag = item.content.hashCode()
            } else if (!item.content.isNullOrEmpty()) {
                miniMarkwon.setMarkdown(chatTv, item.content.postOptimize())
                chatTv.tag = item.content.hashCode()
            } else {
                chatTv.text = null
                chatTv.tag = null
            }
        }
    }
}
