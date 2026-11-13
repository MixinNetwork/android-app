package one.mixin.android.ui.address

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.address.page.AddressSearchBottomSheet
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.vo.Address
import one.mixin.android.vo.Chain
import one.mixin.android.widget.bottomSheetHeight

@AndroidEntryPoint
class AddressSearchBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    override val centerOnExpandedScreen = true

    companion object {
        const val TAG = "AddressSearchBottomSheetDialogFragment"
        private const val ARGS_CHAIN_ID = "args_chain_id"

        fun newInstance(chainId: String) = AddressSearchBottomSheetDialogFragment().withArgs {
            putString(ARGS_CHAIN_ID, chainId)
        }
    }

    private val viewModel by viewModels<AddressViewModel>()
    private val chainId by lazy { requireArguments().getString(ARGS_CHAIN_ID).orEmpty() }

    var onAddressClick: ((Address) -> Unit)? = null
    var onAddClick: (() -> Unit)? = null
    var onDeleteAddress: ((Address) -> Unit)? = null

    override fun getTheme() = R.style.AppTheme_Dialog

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            val addressFlow = remember(chainId) { viewModel.addressesFlow(chainId) }
            val addresses by addressFlow.collectAsState(initial = emptyList())
            var chain by remember { mutableStateOf<Chain?>(null) }
            LaunchedEffect(addresses) {
                chain = viewModel.getChains().firstOrNull { it.chainId == chainId }
            }
            AddressSearchBottomSheet(
                addresses = addresses,
                chainId = chainId,
                chain = chain,
                onAddressClick = { address ->
                    dismiss()
                    onAddressClick?.invoke(address)
                },
                onAddClick = {
                    dismiss()
                    onAddClick?.invoke()
                },
                onDeleteAddress = { address ->
                    onDeleteAddress?.invoke(address)
                },
                onDismiss = { dismiss() }
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().bottomSheetHeight() - view.getSafeAreaInsetsTop() - requireContext().appCompatActionBarHeight()
    }

    override fun showError(error: String) {
    }
}
