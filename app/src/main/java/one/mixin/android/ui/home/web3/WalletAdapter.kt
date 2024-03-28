package one.mixin.android.ui.home.web3

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemWalletInternalBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.vo.Dapp

class WalletAdapter : RecyclerView.Adapter<Web3Holder>() {
    var connections: List<Dapp> = emptyList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): Web3Holder {
        return Web3Holder(ItemWalletInternalBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return  connections.size
    }

    override fun onBindViewHolder(
        holder: Web3Holder,
        position: Int,
    ) {
        holder.bind(connections[position])
    }
}

class Web3Holder(val binding: ItemWalletInternalBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(web3: Dapp) {
        binding.apply {
            avatar.loadImage(web3.iconUrl)
            name.text = web3.name
            url.text = web3.homeUrl
            root.setOnClickListener {
                WebActivity.show(it.context, web3.homeUrl, null)
            }
        }
    }
}
