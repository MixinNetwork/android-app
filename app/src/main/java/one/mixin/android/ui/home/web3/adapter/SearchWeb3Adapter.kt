package one.mixin.android.ui.home.web3.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemWalletSearchBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.setQuoteText
import one.mixin.android.util.getChainNetwork
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class SearchWeb3Adapter : ListAdapter<Web3TokenItem, SearchWeb3Adapter.TokenHolder>(TOKEN_DIFF) {
    var callback: Web3SearchCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenHolder {
        val binding = ItemWalletSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TokenHolder(binding)
    }

    override fun onBindViewHolder(holder: TokenHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TokenHolder(private val binding: ItemWalletSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(token: Web3TokenItem) {
            binding.apply {
                badgeCircleIv.loadToken(token)
                nameTv.text = token.name

                val balance = runCatching { BigDecimal(token.balance) }.getOrDefault(BigDecimal.ZERO)
                
                balanceTv.text = "${balance.numberFormat8()} ${token.symbol}"
                val chainNetwork = getChainNetwork(token.assetId, token.chainId, token.assetKey)
                binding.networkTv.isVisible = chainNetwork != null
                if (chainNetwork != null) {
                    binding.networkTv.text = chainNetwork
                }
                if (token.priceUsd == "0") {
                    binding.priceTv.setText(R.string.NA)
                    binding.changeTv.visibility = View.GONE
                } else {
                    binding.changeTv.visibility = View.VISIBLE
                    binding.priceTv.text = "${Fiats.getSymbol()}${token.priceFiat().priceFormat()}"
                    if (token.changeUsd.isNotEmpty()) {
                        val bigChangeUsd = BigDecimal(token.changeUsd)
                        val isRising = bigChangeUsd >= BigDecimal.ZERO
                        binding.changeTv.setQuoteText("${(bigChangeUsd * BigDecimal(100)).numberFormat2()}%", isRising)
                    }
                }
                itemView.setOnClickListener {
                    callback?.onTokenClick(token)
                }
            }
        }
    }

    companion object {
        private val TOKEN_DIFF = object : DiffUtil.ItemCallback<Web3TokenItem>() {
            override fun areItemsTheSame(oldItem: Web3TokenItem, newItem: Web3TokenItem): Boolean {
                return oldItem.assetId == newItem.assetId
            }

            override fun areContentsTheSame(oldItem: Web3TokenItem, newItem: Web3TokenItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
