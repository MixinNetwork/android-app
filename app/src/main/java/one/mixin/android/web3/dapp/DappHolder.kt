package one.mixin.android.web3.dapp

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemFavoriteBinding
import one.mixin.android.vo.Dapp

class DappHolder(val binding: ItemFavoriteBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(web3: Dapp, onDappClick: (String) -> Unit) {
        binding.apply {
            avatar.loadUrl(web3.iconUrl)
            name.text = web3.name
            mixinIdTv.text = web3.homeUrl
            verifiedIv.isVisible = false
            root.setOnClickListener {
                onDappClick.invoke(web3.homeUrl)
            }
        }
    }
}
