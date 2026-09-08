package one.mixin.android.web3.swap

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.groupId
import one.mixin.android.api.response.web3.groupSwapTokens
import one.mixin.android.databinding.ItemWeb3SwapTokenBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.loadImage
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.getChainNetwork

class SwapTokenAdapter(private val selectUnique: String? = null, var grouped: Boolean = false, private val networkSelection: Boolean = false) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    fun isEmpty() = getFilteredTokens().isEmpty()
    var tokenType: String = AnalyticsTracker.SpotTokenType.SEND

    var tokens: List<SwapToken> = ArrayList(0)
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field !== value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var stocks: List<SwapToken> = ArrayList(0)
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field !== value) {
                field = value
                notifyDataSetChanged()
            }
        }

    private fun rawFilteredTokens() = if (chain == null) {
        tokens
    } else if (keyword.isNullOrBlank() && chain == "") {
        stocks
    } else if (chain == "") {
        tokens.filter { it.category.equalsIgnoreCase("stock") }
    } else {
        tokens.filter { it.chain.chainId == chain }
    }

    private fun getFilteredTokens(): List<SwapToken> =
        if (grouped) rawFilteredTokens().groupSwapTokens().map { it.first() } else rawFilteredTokens()

    fun groupAssets(token: SwapToken): List<SwapToken> {
        val groups = rawFilteredTokens().groupSwapTokens()
        return groups.firstOrNull { group -> group.any { it.walletId == token.walletId && it.assetId == token.assetId } }
            ?: groups.firstOrNull { grouped && it.first().groupId == token.groupId }.orEmpty()
    }

    var keyword:String? = null
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var chain: String? = null
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
        return getFilteredTokens().size
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val item = getFilteredTokens()[position]
        val group = if (grouped) groupAssets(item) else emptyList()
        val amount = if (grouped) group.sumOf { it.balanceValue }.toPlainString() else item.balance ?: "0"
        val selected = if (group.any { it.assetId == selectUnique }) item.assetId else selectUnique
        (holder as Web3Holder).bind(item, selected, amount, grouped, networkSelection) { token, isAlert ->
            val method = if (isSearch) {
                AnalyticsTracker.TradeTokenSelectMethod.SEARCH_ITEM_CLICK
            } else if (all) {
                AnalyticsTracker.TradeTokenSelectMethod.ALL_ITEM_CLICK
            } else {
                AnalyticsTracker.TradeTokenSelectMethod.CHAIN_ITEM_CLICK
            }
            AnalyticsTracker.trackTradeTokenSelect(method)
            AnalyticsTracker.trackSpotTokenSelect(method, tokenType, token.chain.name, token.symbol)

            onClickListener?.invoke(token, isAlert)
        }
    }
}

class Web3Holder(val binding: ItemWeb3SwapTokenBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(
        token: SwapToken,
        selectUnique: String? = null,
        amount: String = token.balance ?: "0",
        grouped: Boolean = false,
        networkSelection: Boolean = false,
        onClickListener: ((SwapToken, Boolean) -> Unit)?,
    ) {
        binding.apply {
            root.setOnClickListener {
                onClickListener?.invoke(token, false)
            }
            avatar.bg.loadImage(token.icon, R.drawable.ic_avatar_place_holder)
            avatar.badge.loadImage(token.chain.icon, R.drawable.ic_avatar_place_holder)
            avatar.badge.isVisible = !grouped
            icSpam.isVisible = token.isSpam()
            updateNameLayout(token.isSpam())
            nameTv.text = if (networkSelection) token.chain.name else token.name
            balanceTv.text = "${amount} ${token.symbol}"
            val chainNetwork = getChainNetwork(token.assetId, token.chain.chainId, token.address)
            networkTv.isVisible = !grouped && !networkSelection && chainNetwork != null
            if (chainNetwork != null) {
                binding.networkTv.text = chainNetwork
            }
            if (token.isWeb3 && !networkSelection) {
                alert.isVisible = true
                select.isVisible = false
                alert.setOnClickListener {
                    onClickListener?.invoke(token, true)
                }
            } else {
                alert.isVisible = false
                select.isVisible = token.assetId == selectUnique
                alert.setOnClickListener(null)
            }
        }
    }

    private fun updateNameLayout(isSpam: Boolean) {
        val layoutParams: RelativeLayout.LayoutParams = binding.nameTv.layoutParams as? RelativeLayout.LayoutParams ?: return
        layoutParams.removeRule(RelativeLayout.RIGHT_OF)
        if (isSpam) {
            layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.ic_spam)
            layoutParams.marginStart = 2.dp
        } else {
            layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.avatar)
            layoutParams.marginStart = 16.dp
        }
        binding.nameTv.layoutParams = layoutParams
        updateBalanceLayout(isSpam)
    }

    private fun updateBalanceLayout(isSpam: Boolean) {
        val layoutParams: RelativeLayout.LayoutParams = binding.balanceTv.layoutParams as? RelativeLayout.LayoutParams ?: return
        layoutParams.removeRule(RelativeLayout.RIGHT_OF)
        layoutParams.removeRule(RelativeLayout.ALIGN_START)
        if (isSpam) {
            layoutParams.addRule(RelativeLayout.ALIGN_START, R.id.ic_spam)
            layoutParams.marginStart = 0
        } else {
            layoutParams.addRule(RelativeLayout.ALIGN_START, R.id.name_tv)
        }
        binding.balanceTv.layoutParams = layoutParams
    }
}
