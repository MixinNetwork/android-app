package one.mixin.android.web3.swap

import androidx.fragment.app.Fragment
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.databinding.FragmentChooseTokensBottomSheetBinding
import one.mixin.android.widget.BottomSheet

fun Fragment.showTokenNetworks(tokens: List<SwapToken>, selectedAssetId: String? = null, onSelected: (SwapToken) -> Unit) {
    if (tokens.size <= 1) {
        tokens.firstOrNull()?.let(onSelected)
        return
    }
    val content = FragmentChooseTokensBottomSheetBinding.inflate(layoutInflater)
    val sheet = BottomSheet.Builder(requireActivity()).setCustomView(content.root).create()
    content.chooseNetwork.text = getString(R.string.Choose_Token, tokens.first().symbol)
    content.chooseNetworkSub.text = getString(R.string.choose_token_desc, tokens.first().symbol)
    content.close.setOnClickListener { sheet.dismiss() }
    content.assetRv.adapter = SwapTokenAdapter(selectedAssetId, networkSelection = true).apply {
        this.tokens = tokens.sortedByDescending { it.balanceValue }
        setOnClickListener { token, _ ->
            sheet.dismiss()
            onSelected(token)
        }
    }
    sheet.show()
}
