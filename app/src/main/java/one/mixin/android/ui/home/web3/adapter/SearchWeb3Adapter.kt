package one.mixin.android.ui.home.web3.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemWalletSearchBinding
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import java.math.BigDecimal

class SearchWeb3Adapter : ListAdapter<Web3Token, SearchWeb3Adapter.TokenHolder>(TOKEN_DIFF) {
    var callback: Web3SearchCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenHolder {
        val binding = ItemWalletSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TokenHolder(binding)
    }

    override fun onBindViewHolder(holder: TokenHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TokenHolder(private val binding: ItemWalletSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(token: Web3Token) {
            binding.apply {
                badgeCircleIv.loadToken(token)
                nameTv.text = token.name

                val balance = runCatching { BigDecimal(token.balance) }.getOrDefault(BigDecimal.ZERO)
                val price = runCatching { BigDecimal(token.price) }.getOrDefault(BigDecimal.ZERO)
                val value = balance * price
                
                balanceTv.text = balance.numberFormat8()
                if (price > BigDecimal.ZERO) {
                    priceTv.text = "$${value.numberFormat2()}"
                } else {
                    priceTv.text = ""
                }
                
                itemView.setOnClickListener {
                    callback?.onTokenClick(token)
                }
            }
        }
    }

    companion object {
        private val TOKEN_DIFF = object : DiffUtil.ItemCallback<Web3Token>() {
            override fun areItemsTheSame(oldItem: Web3Token, newItem: Web3Token): Boolean {
                return oldItem.fungibleId == newItem.fungibleId
            }

            override fun areContentsTheSame(oldItem: Web3Token, newItem: Web3Token): Boolean {
                return oldItem == newItem
            }
        }
    }
}
