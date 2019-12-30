package one.mixin.android.ui.conversation.holder

import android.view.View
import kotlinx.android.synthetic.main.item_chat_stranger.view.*
import one.mixin.android.R
import one.mixin.android.ui.conversation.adapter.ConversationAdapter

class StrangerHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    fun bind(onItemListener: ConversationAdapter.OnItemListener, isBot: Boolean) {
        if (isBot) {
            itemView.stranger_info.setText(R.string.bot_interact_info)
            itemView.stranger_block_bn.setText(R.string.bot_interact_open)
            itemView.stranger_add_bn.setText(R.string.bot_interact_hi)
        } else {
            itemView.stranger_info.setText(R.string.stranger_from)
            itemView.stranger_block_bn.setText(R.string.setting_block)
            itemView.stranger_add_bn.setText(R.string.contact_add_contact_title)
        }
        itemView.stranger_block_bn.setOnClickListener {
            if (isBot) {
                onItemListener.onOpenHomePage()
            } else {
                onItemListener.onBlockClick()
            }
        }
        itemView.stranger_add_bn.setOnClickListener {
            if (isBot) {
                onItemListener.onSayHi()
            } else {
                onItemListener.onAddClick()
            }
        }
    }
}
