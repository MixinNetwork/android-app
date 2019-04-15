package one.mixin.android.ui.search.holder

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_search_contact.view.*
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.User

class ContactHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    fun bind(user: User, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        itemView.search_name.text = user.fullName
        itemView.search_avatar_iv.setInfo(user.fullName, user.avatarUrl, user.userId)
        itemView.verified_iv.visibility = if (user.isVerified == true) {
            View.VISIBLE
        } else {
            View.GONE
        }
        itemView.bot_iv.visibility = if (user.isBot()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        itemView.setOnClickListener {
            onItemClickListener?.onUserClick(user)
        }
    }
}