package one.mixin.android.ui.wallet.transfer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import one.mixin.android.databinding.ItemAssetChangeBinding
import one.mixin.android.databinding.ItemTransferAssetChangeBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.forEachWithIndex
import one.mixin.android.util.getChainName
import one.mixin.android.vo.safe.TokenItem

class TransferAssetChangeItem : LinearLayout {
    private val _binding: ItemTransferAssetChangeBinding
    private val dp28 = 28.dp
    private val dp8 = 8.dp
    private val dp6 = 6.dp

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ItemTransferAssetChangeBinding.inflate(LayoutInflater.from(context), this)
        setPadding(dp28, dp8, dp28, dp8)
    }

    @SuppressLint("SetTextI18s")
    fun setContent(
        @StringRes titleRes: Int,
        amounts: List<String>,
        tokens: List<TokenItem>,
    ) {
        _binding.apply {
            title.text = context.getString(titleRes).uppercase()
            assetContainer.removeAllViews()
            amounts.forEachWithIndex { index, amount ->
                val token = tokens[index]
                val item = AssetChangeItem(context)
                item.setContent(amount, token)
                assetContainer.addView(item, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp6
                    bottomMargin = dp6
                })
            }
        }
    }

    private class AssetChangeItem @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : LinearLayout(context, attrs, defStyleAttr) {
        private val binding = ItemAssetChangeBinding.inflate(LayoutInflater.from(context), this, true)

        @SuppressLint("SetTextI18n")
        fun setContent(amount: String, token: TokenItem) {
            binding.apply {
                avatar.loadUrl(token.iconUrl)
                this.amount.text = "- $amount ${token.symbol}"
                network.text = getChainName(token.chainId, token.chainName, token.assetKey) ?: ""
            }
        }
    }
}