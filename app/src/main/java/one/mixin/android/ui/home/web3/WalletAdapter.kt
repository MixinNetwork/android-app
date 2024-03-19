package one.mixin.android.ui.home.web3

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.Constants.InternalWeb3Wallet
import one.mixin.android.databinding.ItemWalletInternalBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.vo.ConnectionUI

class WalletAdapter : RecyclerView.Adapter<Web3Holder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Web3Holder {
            return Web3Holder(ItemWalletInternalBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount(): Int {
            return InternalWeb3Wallet.size
        }

        override fun onBindViewHolder(holder: Web3Holder, position: Int) {
            holder.bind(InternalWeb3Wallet[position])
        }
    }

    class Web3Holder(val binding: ItemWalletInternalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(web3: ConnectionUI) {
            binding.apply {
                if (web3.internalIcon != null) {
                    avatar.setImageResource(web3.internalIcon)
                } else if (web3.icon!=null) {
                    avatar.loadImage(web3.uri)
                }
                name.text = web3.name
                url.text = web3.uri
                root.setOnClickListener{
                    WebActivity.show(it.context, web3.uri, null)
                }
            }
        }
    }