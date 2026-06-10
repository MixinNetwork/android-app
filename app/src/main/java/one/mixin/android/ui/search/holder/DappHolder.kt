package one.mixin.android.ui.search.holder

import android.annotation.SuppressLint
import one.mixin.android.databinding.ItemSearchDappBinding
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.Dapp

class DappHolder constructor(val binding: ItemSearchDappBinding) : NormalHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(
        dapp: Dapp,
        target: String?,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
    ) {
        binding.apply {
            avatar.loadUrl(dapp.iconUrl)
            nameTv.setTextOnly(dapp.name)
            if (target != null) {
                nameTv.highLight(target)
            }
            mixinIdTv.text = dapp.homeUrl
            root.setOnClickListener {
                onItemClickListener?.onDappClick(dapp)
            }
        }
    }
}
