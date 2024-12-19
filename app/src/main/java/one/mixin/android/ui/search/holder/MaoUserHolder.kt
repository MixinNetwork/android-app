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
        private const val MAO_ICON = "https://kernel.mixin.dev/objects/fe75a8e48aeffb486df622c91bebfe4056ada7009f3151fb49e2a18340bbd615/icon"
    }

    fun bind(
        user: MaoUser,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
    ) {
        binding.apply {
            normal.setName(user)
            maoNameTv.text = user.maoName
            maoNameTv.highLightMao()
            icon.loadImage(MAO_ICON, R.drawable.ic_inscription_icon, transformation = CoilRoundedHexagonTransformation())
            avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            root.setOnClickListener {
                onItemClickListener?.onUserClick(user)
            }
        }
    }
}
