package one.mixin.android.ui.address

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.address.page.AddressSearchBottomSheet
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.Address

@AndroidEntryPoint
class AddressSearchBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
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

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            val addresses by viewModel.addressesFlow(chainId).collectAsState(initial = emptyList())
            AddressSearchBottomSheet(
                addresses = addresses,
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
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
    }
}
