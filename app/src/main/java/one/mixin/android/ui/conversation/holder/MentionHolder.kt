package one.mixin.android.ui.conversation.holder

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_chat_mention.view.*
import one.mixin.android.extension.highLight
import one.mixin.android.ui.conversation.adapter.MentionAdapter
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class MentionHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    @SuppressLint("SetTextI18n")
    fun bind(user: User, keyword: String?, listener: MentionAdapter.OnUserClickListener) {
        itemView.name.text = user.fullName
        itemView.id_tv.text = "@${user.identityNumber}"
        itemView.id_tv.highLight(keyword)
        itemView.icon_iv.setInfo(user.fullName, user.avatarUrl, user.userId)
        user.showVerifiedOrBot(itemView.verified_iv, itemView.bot_iv)
        itemView.setOnClickListener {
            user.fullName?.let { name ->
                listener.onUserClick(name)
            }
        }
    }
}
