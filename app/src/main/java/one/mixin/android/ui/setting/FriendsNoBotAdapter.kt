package one.mixin.android.ui.setting

import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_friends_no_bot.view.*
import one.mixin.android.R
import one.mixin.android.extension.highLight
import one.mixin.android.extension.inflate
import one.mixin.android.ui.common.friends.AbsFriendsAdapter
import one.mixin.android.ui.common.friends.BaseFriendsViewHolder
import one.mixin.android.ui.common.friends.FriendsListener
import one.mixin.android.vo.User

class FriendsNoBotAdapter : AbsFriendsAdapter<FriendsNoBotViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FriendsNoBotViewHolder(parent.inflate(R.layout.item_friends_no_bot))
}

class FriendsNoBotViewHolder(itemView: View) : BaseFriendsViewHolder(itemView) {
    override fun bind(item: User, filter: String, listener: FriendsListener?) {
        super.bind(item, filter, listener)
        itemView.identity.text = item.identityNumber
        itemView.identity.highLight(filter)
    }
}