package one.mixin.android.ui.conversation.holder

import one.mixin.android.R
import one.mixin.android.databinding.ItemChatSecretBinding
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder

class SecretHolder constructor(val binding: ItemChatSecretBinding) : BaseViewHolder(binding.root) {
    fun bind(onItemListener: MessageAdapter.OnItemListener) {
        itemView.setOnClickListener {
            onItemListener.onUrlClick(itemView.context.getString(R.string.secret_url))
        }
    }
}
