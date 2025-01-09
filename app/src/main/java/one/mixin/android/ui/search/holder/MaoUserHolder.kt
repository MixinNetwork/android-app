package one.mixin.android.ui.search.holder

import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchMaoUserBinding
import one.mixin.android.extension.expandTouchArea
import one.mixin.android.extension.highLightMao
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.sp
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.MaoUser
class MaoUserHolder constructor(
    val binding: ItemSearchMaoUserBinding,
) : NormalHolder(binding.root) {

    init {
        binding.open.expandTouchArea()
    }

    fun bind(
        user: MaoUser,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
    ) {
        binding.apply {
            normal.setName(user)
            maoNameTv.text = user.maoName
            maoNameTv.highLightMao()
            maoNameTv.loadImage(R.drawable.ic_mao, 14.sp)
            open.isVisible = user.appId != null
            open.setOnClickListener {
                user.appId?.let {
                    onItemClickListener?.onMaoAppClick(it)
                }
            }
            avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            root.setOnClickListener {
                onItemClickListener?.onUserClick(user)
            }
        }
    }
}
