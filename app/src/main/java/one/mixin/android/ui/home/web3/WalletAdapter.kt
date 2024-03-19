package one.mixin.android.ui.home.web3

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.Constants.InternalWeb3Wallet
import one.mixin.android.databinding.ItemWalletInternalBinding
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.vo.InternalWeb3

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
        fun bind(web3: InternalWeb3) {
            binding.apply {
                avatar.setImageResource(web3.icon)
                name.text = web3.name
                url.text = web3.url
                root.setOnClickListener{
                    WebActivity.show(it.context, web3.url, null)
                }
            }
        }
    }