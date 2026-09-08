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
import one.mixin.android.db.web3.vo.Web3TokenGroup
import one.mixin.android.db.web3.vo.groupId
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.setQuoteText
import one.mixin.android.util.getChainNetwork
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class SearchWeb3Adapter : ListAdapter<Web3TokenGroup, SearchWeb3Adapter.TokenHolder>(TOKEN_DIFF) {
    var callback: Web3SearchCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenHolder {
        val binding = ItemWalletSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TokenHolder(binding)
    }

    override fun onBindViewHolder(holder: TokenHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TokenHolder(private val binding: ItemWalletSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: Web3TokenGroup) {
            val token = group.representative
            binding.apply {
                badgeCircleIv.loadToken(token)
                badgeCircleIv.badge.isVisible = false
                nameTv.text = token.name
                icSpam.isVisible = token.isSpam()
                val balance = group.balance
                
                balanceTv.text = "${balance.numberFormat8()} ${token.symbol}"
                val chainNetwork = getChainNetwork(token.assetId, token.chainId, token.assetKey)
                binding.networkTv.isVisible = false
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
        private val TOKEN_DIFF = object : DiffUtil.ItemCallback<Web3TokenGroup>() {
            override fun areItemsTheSame(oldItem: Web3TokenGroup, newItem: Web3TokenGroup): Boolean {
                return oldItem.representative.groupId == newItem.representative.groupId
            }

            override fun areContentsTheSame(oldItem: Web3TokenGroup, newItem: Web3TokenGroup): Boolean {
                return oldItem == newItem
            }
        }
    }
}
