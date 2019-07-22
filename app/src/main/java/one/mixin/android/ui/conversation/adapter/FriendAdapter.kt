package one.mixin.android.ui.conversation.adapter

import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_contact_normal.view.*
import one.mixin.android.R
import one.mixin.android.extension.inflate
import one.mixin.android.ui.common.friends.AbsFriendsAdapter
import one.mixin.android.ui.common.friends.BaseFriendsViewHolder
import one.mixin.android.ui.common.friends.FriendsListener
import one.mixin.android.ui.common.friends.UserItemCallback
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class FriendsAdapter(callback: UserItemCallback) : AbsFriendsAdapter<FriendsViewHolder>(callback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FriendsViewHolder(parent.inflate(R.layout.item_contact_normal))
}

class FriendsViewHolder(itemView: View) : BaseFriendsViewHolder(itemView) {
    override fun bind(item: User, filter: String, listener: FriendsListener?) {
        super.bind(item, filter, listener)
        item.showVerifiedOrBot(itemView.verified_iv, itemView.bot_iv)
    }
}
