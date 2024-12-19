package one.mixin.android.ui.search.holder

import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchContactBinding
import one.mixin.android.databinding.ItemSearchMaoUserBinding
import one.mixin.android.extension.highLightMao
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.MaoUser
import one.mixin.android.vo.User
import one.mixin.android.widget.CoilRoundedHexagonTransformation

class MaoUserHolder constructor(
    val binding: ItemSearchMaoUserBinding,
) : NormalHolder(binding.root) {

    companion object {
        private const val iconUrl = "https://mixin.one/assets/7d8b4b8d8f6b1b4e1b4e1b4e1b4e1b4e.png"
    }

    fun bind(
        user: MaoUser,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
    ) {
        binding.apply {
            normal.setName(user)
            mixinIdTv.text = user.maoName
            mixinIdTv.highLightMao()
            icon.loadImage(iconUrl, R.drawable.ic_avatar_place_holder, transformation = CoilRoundedHexagonTransformation())
            avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            root.setOnClickListener {
                onItemClickListener?.onUserClick(user)
            }
        }
    }
}
