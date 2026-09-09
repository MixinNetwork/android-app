package one.mixin.android.web3.swap

import androidx.fragment.app.Fragment
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.ui.wallet.DepositChooseNetworkBottomSheetDialogFragment

fun Fragment.showTokenNetworks(tokens: List<SwapToken>, selectedAssetId: String? = null, onSelected: (SwapToken) -> Unit) {
    if (tokens.size <= 1) {
        tokens.firstOrNull()?.let(onSelected)
        return
    }
    DepositChooseNetworkBottomSheetDialogFragment.newInstance(
        ArrayList(tokens.sortedByDescending { it.balanceValue }),
        selectedAssetId,
    ).apply {
        onTokenSelected = onSelected
    }.show(parentFragmentManager, DepositChooseNetworkBottomSheetDialogFragment.TAG)
}
