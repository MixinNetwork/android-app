package one.mixin.android.ui.conversation.holder

import android.view.View
import kotlinx.android.synthetic.main.item_chat_stranger.view.*
import one.mixin.android.ui.conversation.adapter.ConversationAdapter

class StrangerHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    override fun chatLayout(isMe: Boolean, isLast: Boolean) {
    }

    fun bind(onItemListener: ConversationAdapter.OnItemListener) {
        itemView.stranger_block_bn.setOnClickListener { onItemListener.onBlockClick() }
        itemView.stranger_add_bn.setOnClickListener { onItemListener.onAddClick() }
    }
}