package one.mixin.android.ui.search.holder

import one.mixin.android.databinding.ItemSearchContactBinding
import one.mixin.android.extension.highLight
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class ContactHolder constructor(
    val binding: ItemSearchContactBinding
) : NormalHolder(binding.root) {
    fun bind(user: User, target: String, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        binding.searchName.text = user.fullName
        binding.searchName.highLight(target)
        binding.searchAvatarIv.setInfo(user.fullName, user.avatarUrl, user.userId)
        user.showVerifiedOrBot(binding.verifiedIv, binding.botIv)
        binding.root.setOnClickListener {
            onItemClickListener?.onUserClick(user)
        }
    }
}
