package one.mixin.android.ui.setting

import android.view.ViewGroup
import one.mixin.android.databinding.ItemFriendsNoBotBinding
import one.mixin.android.extension.highLight
import one.mixin.android.extension.layoutInflater
import one.mixin.android.ui.common.friends.AbsFriendsAdapter
import one.mixin.android.ui.common.friends.BaseFriendsViewHolder
import one.mixin.android.ui.common.friends.FriendsListener
import one.mixin.android.ui.common.friends.UserItemCallback
import one.mixin.android.vo.User

class FriendsNoBotAdapter(callback: UserItemCallback) : AbsFriendsAdapter<FriendsNoBotViewHolder>(callback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FriendsNoBotViewHolder(ItemFriendsNoBotBinding.inflate(parent.context.layoutInflater, parent, false))
}

class FriendsNoBotViewHolder(private val itemBinding: ItemFriendsNoBotBinding) : BaseFriendsViewHolder(itemBinding.root) {
    override fun bind(item: User, filter: String, listener: FriendsListener?) {
        itemBinding.apply {
            identity.text = item.identityNumber
            identity.highLight(filter)
            normal.text = item.fullName
            normal.highLight(filter)
            avatar.setInfo(item.fullName, item.avatarUrl, item.userId)
        }
        itemView.setOnClickListener {
            listener?.onItemClick(item)
        }
    }
}
