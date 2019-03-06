package one.mixin.android.ui.search.holder

import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.item_search_contact.view.*
import one.mixin.android.extension.highLight
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.User

class ContactHolder constructor(containerView: View) : NormalHolder(containerView) {
    fun bind(user: User, target: String, onItemClickListener: SearchFragment.OnSearchClickListener?, isEnd: Boolean = false) {
        itemView.search_name.text = user.fullName
        itemView.search_name.highLight(target)
        itemView.search_avatar_iv.setInfo(user.fullName, user.avatarUrl, user.userId)
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