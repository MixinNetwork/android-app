package one.mixin.android.ui.conversation.holder

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_chat_mention.view.*
import one.mixin.android.ui.conversation.adapter.MentionAdapter
import one.mixin.android.vo.User

class MentionHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    init {
        itemView.avatar_av.setTextSize(12f)
    }

    fun bind(user: User, keyword: String?, listener: MentionAdapter.OnUserClickListener) {
        val userName = user.fullName
        itemView.avatar_av.setInfo(userName, user.avatarUrl, user.identityNumber)
        itemView.name.text = userName
        itemView.setOnClickListener { listener.onUserClick(keyword, userName!!) }
    }
}
