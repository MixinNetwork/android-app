package one.mixin.android.ui.search.holder

import one.mixin.android.databinding.ItemSearchContactBinding
import one.mixin.android.extension.highLight
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.SearchBot

class BotHolder(
    val binding: ItemSearchContactBinding,
) : NormalHolder(binding.root) {
    fun bind(
        user: SearchBot,
        target: String,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
    ) {
        binding.apply {
            normal.setName(user)
            normal.highLight(target)
            mixinIdTv.text = user.identityNumber
            mixinIdTv.highLight(target)
            avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            root.setOnClickListener {
                onItemClickListener?.onBotClick(user)
            }
        }
    }
}
