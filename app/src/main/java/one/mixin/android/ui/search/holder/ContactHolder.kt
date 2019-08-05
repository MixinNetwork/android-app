package one.mixin.android.ui.search.holder

import android.view.View
import kotlinx.android.synthetic.main.item_search_contact.view.*
import one.mixin.android.extension.highLight
import one.mixin.android.extension.nonBlankFullName
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class ContactHolder constructor(containerView: View) : NormalHolder(containerView) {
    fun bind(user: User, target: String, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        itemView.search_name.text = user.fullName.nonBlankFullName(user.identityNumber)
        itemView.search_name.highLight(target)
        itemView.search_avatar_iv.setInfo(user.fullName, user.avatarUrl, user.userId)
        user.showVerifiedOrBot(itemView.verified_iv, itemView.bot_iv)
        itemView.setOnClickListener {
            onItemClickListener?.onUserClick(user)
        }
    }
}
