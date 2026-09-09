package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import one.mixin.android.databinding.ItemWalletSearchBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.TokenGroup
import one.mixin.android.vo.safe.groupTokens

class SearchAdapter(private val currentAssetId: String? = null, private val grouped: Boolean = false) : ListAdapter<TokenItem, AssetHolder>(TokenItem.DIFF_CALLBACK) {
    var callback: WalletSearchCallback? = null
    
    private var allTokens: List<TokenItem> = emptyList()
    
    var chain: String? = null
        set(value) {
            if (field != value) {
                field = value
                super.submitList(getFilteredTokens()) { notifyItemRangeChanged(0, itemCount) }
            }
        }
    
    fun getFilteredTokens(): List<TokenItem> {
        val tokens = if (chain.isNullOrBlank()) {
            allTokens
        } else {
            allTokens.filter { it.chainId == chain }
        }
        return if (grouped) tokens.groupTokens().map { it.representative } else tokens
    }

    fun groupAssets(assetId: String): List<TokenItem> {
        val tokens = allTokens.filter { chain.isNullOrBlank() || it.chainId == chain }
        return tokens.groupTokens().firstOrNull { group -> group.tokens.any { it.assetId == assetId } }?.tokens.orEmpty()
    }
    
    override fun submitList(list: List<TokenItem>?) {
        allTokens = list ?: emptyList()
        super.submitList(getFilteredTokens()) { notifyItemRangeChanged(0, itemCount) }
    }
    
    override fun submitList(list: List<TokenItem>?, commitCallback: Runnable?) {
        allTokens = list ?: emptyList()
        super.submitList(getFilteredTokens()) {
            notifyItemRangeChanged(0, itemCount)
            commitCallback?.run()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): AssetHolder {
        return AssetHolder(ItemWalletSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(
        holder: AssetHolder,
        position: Int,
    ) {
        getItem(position)?.let { item ->
            val group = if (grouped) groupAssets(item.assetId).takeIf { it.isNotEmpty() }?.let(::TokenGroup) else null
            holder.bind(group?.representative ?: item, callback, currentAssetId, group)
        }
    }
}
