package one.mixin.android.ui.search.holder

import one.mixin.android.databinding.ItemSearchContactBinding
import one.mixin.android.extension.highLight
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class ContactHolder constructor(
    val binding: ItemSearchContactBinding,
) : NormalHolder(binding.root) {
    fun bind(
        user: User,
        target: String,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
    ) {
        binding.apply {
            normal.text = user.fullName
            normal.highLight(target)
            mixinIdTv.text = user.identityNumber
            avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            user.showVerifiedOrBot(verifiedIv, botIv, membershipIv)
            root.setOnClickListener {
                onItemClickListener?.onUserClick(user)
            }
        }
    }
}
