package one.mixin.android.ui.setting

import android.view.ViewGroup
import one.mixin.android.databinding.ItemFriendsNoBotBinding
import one.mixin.android.extension.highLight
import one.mixin.android.ui.common.friends.AbsFriendsAdapter
import one.mixin.android.ui.common.friends.BaseFriendsViewHolder
import one.mixin.android.ui.common.friends.FriendsListener
import one.mixin.android.ui.common.friends.UserItemCallback
import one.mixin.android.vo.User
import org.jetbrains.anko.layoutInflater

class FriendsNoBotAdapter(callback: UserItemCallback) : AbsFriendsAdapter<FriendsNoBotViewHolder>(callback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FriendsNoBotViewHolder(ItemFriendsNoBotBinding.inflate(parent.context.layoutInflater, parent, false))
}

class FriendsNoBotViewHolder(private val itemBinding: ItemFriendsNoBotBinding) : BaseFriendsViewHolder(itemBinding.root) {
    override fun bind(item: User, filter: String, listener: FriendsListener?) {
        super.bind(item, filter, listener)
        itemBinding.apply {
            identity.text = item.identityNumber
            identity.highLight(filter)
        }
    }
}
