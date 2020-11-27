package one.mixin.android.ui.conversation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import one.mixin.android.databinding.ItemContactNormalBinding
import one.mixin.android.extension.inflate
import one.mixin.android.ui.common.friends.AbsFriendsAdapter
import one.mixin.android.ui.common.friends.BaseFriendsViewHolder
import one.mixin.android.ui.common.friends.FriendsListener
import one.mixin.android.ui.common.friends.UserItemCallback
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class FriendsAdapter(callback: UserItemCallback) : AbsFriendsAdapter<FriendsViewHolder>(callback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FriendsViewHolder(ItemContactNormalBinding.inflate(LayoutInflater.from(parent.context), parent, false))
}

class FriendsViewHolder(val binding: ItemContactNormalBinding) : BaseFriendsViewHolder(binding.root) {
    override fun bind(item: User, filter: String, listener: FriendsListener?) {
        super.bind(item, filter, listener)
        item.showVerifiedOrBot(binding.verifiedIv, binding.botIv)
    }
}
