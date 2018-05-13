package one.mixin.android.ui.search.holder

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_search_contact.view.*
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.ConversationItemMinimal

class GroupHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    init {
        itemView.bot_iv.visibility = View.GONE
        itemView.verified_iv.visibility = View.GONE
    }

    fun bind(conversation: ConversationItemMinimal, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        bind(conversation, onItemClickListener, false)
    }

    fun bind(
        conversation: ConversationItemMinimal,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
        isEnd: Boolean
    ) {
        itemView.search_name.text = conversation.groupName
        itemView.search_avatar_iv.setInfo(if (conversation.groupName != null &&
            conversation.groupName.isNotEmpty()) conversation.groupName[0] else ' ',
            conversation.groupIconUrl, conversation.ownerIdentityNumber)
        itemView.divider.visibility = View.VISIBLE
        itemView.setOnClickListener {
            onItemClickListener?.onGroupClick(conversation)
        }
    }
}