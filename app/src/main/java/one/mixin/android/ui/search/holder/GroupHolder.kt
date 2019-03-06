package one.mixin.android.ui.search.holder

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
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
        itemView.search_avatar_iv.setInfo(conversation.groupName, conversation.groupIconUrl, conversation.ownerIdentityNumber)
        itemView.ph1.isVisible = isEnd
        itemView.ph2.isVisible = isEnd
        itemView.setOnClickListener {
            onItemClickListener?.onGroupClick(conversation)
        }
    }
}