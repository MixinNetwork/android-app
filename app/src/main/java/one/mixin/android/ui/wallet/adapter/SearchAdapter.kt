package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import one.mixin.android.databinding.ItemWalletSearchBinding
import one.mixin.android.vo.safe.TokenItem

class SearchAdapter(private val currentAssetId: String? = null) : ListAdapter<TokenItem, AssetHolder>(TokenItem.DIFF_CALLBACK) {
    var callback: WalletSearchCallback? = null
    
    private var allTokens: List<TokenItem> = emptyList()
    
    var chain: String? = null
        set(value) {
            if (field != value) {
                field = value
                super.submitList(getFilteredTokens())
            }
        }
    
    private fun getFilteredTokens(): List<TokenItem> {
        return if (chain.isNullOrBlank()) {
            allTokens
        } else {
            allTokens.filter { it.chainId == chain }
        }
    }
    
    override fun submitList(list: List<TokenItem>?) {
        allTokens = list ?: emptyList()
        super.submitList(getFilteredTokens())
    }
    
    override fun submitList(list: List<TokenItem>?, commitCallback: Runnable?) {
        allTokens = list ?: emptyList()
        super.submitList(getFilteredTokens(), commitCallback)
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
        getItem(position)?.let { holder.bind(it, callback, currentAssetId) }
    }
}
