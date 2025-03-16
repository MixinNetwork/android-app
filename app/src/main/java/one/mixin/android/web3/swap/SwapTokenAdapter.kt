package one.mixin.android.web3.swap

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.databinding.ItemWeb3SwapTokenBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.getChainNetwork

class SwapTokenAdapter(private val selectUnique: String? = null) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    fun isEmpty() = tokens.isEmpty()

    var tokens: List<SwapToken> = ArrayList(0)
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var all: Boolean = true
    var isSearch: Boolean =false

    var address: String? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }
    private var onClickListener: ((SwapToken, Boolean) -> Unit)? = null

    fun setOnClickListener(onClickListener: (SwapToken, Boolean) -> Unit) {
        this.onClickListener = onClickListener
    }

    fun onClick(token: SwapToken) {
        onClickListener?.invoke(token, false)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return Web3Holder(ItemWeb3SwapTokenBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return tokens.size
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        (holder as Web3Holder).bind(tokens[position], selectUnique) { token, isAlert ->
            AnalyticsTracker.trackSwapCoinSwitch(
                if (isSearch) {
                    AnalyticsTracker.SwapCoinSwitchMethod.SEARCH_ITEM_CLICK
                } else if (all) {
                    AnalyticsTracker.SwapCoinSwitchMethod.ALL_ITEM_CLICK
                } else {
                    AnalyticsTracker.SwapCoinSwitchMethod.CHAIN_ITEM_CLICK
                }
            )
            onClickListener?.invoke(token, isAlert)
        }
    }
}

class Web3Holder(val binding: ItemWeb3SwapTokenBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(
        token: SwapToken,
        selectUnique: String? = null,
        onClickListener: ((SwapToken, Boolean) -> Unit)?,
    ) {
        binding.apply {
            root.setOnClickListener {
                onClickListener?.invoke(token, false)
            }
            avatar.bg.loadImage(token.icon, R.drawable.ic_avatar_place_holder)
            avatar.badge.loadImage(token.chain.icon, R.drawable.ic_avatar_place_holder)
            nameTv.text = token.name
            balanceTv.text = "${token.balance ?: "0"} ${token.symbol}"
            val chainNetwork = getChainNetwork(token.getUnique(), token.chain.chainId, token.address)
            networkTv.isVisible = chainNetwork != null
            if (chainNetwork != null) {
                binding.networkTv.text = chainNetwork
            }
            if (token.isWeb3) {
                alert.isVisible = true
                select.isVisible = false
                alert.setOnClickListener {
                    onClickListener?.invoke(token, true)
                }
            } else {
                alert.isVisible = false
                select.isVisible = token.getUnique() == selectUnique
                alert.setOnClickListener(null)
            }
        }
    }
}
