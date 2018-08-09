package one.mixin.android.ui.conversation.holder

import android.view.View
import one.mixin.android.R
import one.mixin.android.ui.conversation.adapter.ConversationAdapter

class SecretHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
    }

    fun bind(onItemListener: ConversationAdapter.OnItemListener) {
        itemView.setOnClickListener {
            onItemListener.onUrlClick(itemView.context.getString(R.string.secret_url))
        }
    }
}