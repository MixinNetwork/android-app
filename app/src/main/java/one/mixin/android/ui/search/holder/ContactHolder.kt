package one.mixin.android.ui.search.holder

import one.mixin.android.databinding.ItemSearchContactBinding
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.User

class ContactHolder constructor(
    val binding: ItemSearchContactBinding,
) : NormalHolder(binding.root) {
    fun bind(
        user: User,
        target: String,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
    ) {
        binding.apply {
            normal.setName(user)
            normal.highLight(target)
            mixinIdTv.text = user.identityNumber
            avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            root.setOnClickListener {
                onItemClickListener?.onUserClick(user)
            }
        }
    }
}
