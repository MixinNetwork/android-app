package one.mixin.android.ui.search.holder

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.item_search_contact.view.*
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.User

class ContactHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    fun bind(user: User, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        bind(user, onItemClickListener, false)
    }

    fun bind(user: User, onItemClickListener: SearchFragment.OnSearchClickListener?, isEnd: Boolean) {
        itemView.search_name.text = user.fullName
        itemView.search_avatar_iv.setInfo(user.fullName, user.avatarUrl, user.identityNumber)
        itemView.ph1.isVisible = isEnd
        itemView.ph2.isVisible = isEnd
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