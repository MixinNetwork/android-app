package one.mixin.android.ui.wallet.transfer

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentTransferBottomSheetBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.wallet.transfer.data.TransferStatus
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class TransferBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "TransferBottomSheetDialogFragment"
        const val ARGS_TRANSFER = "args_transfer"

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            TransferBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_TRANSFER, t)
            }
    }

    private val t: AssetBiometricItem by lazy {
        requireArguments().getParcelableCompat(ARGS_TRANSFER, AssetBiometricItem::class.java)!!
    }

    private val transferViewModel by viewModels<TransferViewModel>()

    private val binding by viewBinding(FragmentTransferBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        binding.content.render(t)
        binding.header.setContent(R.string.Transfer_confirmation, R.string.Transfer_confirmation_desc, t.asset!!)
        binding.transferAlert.isVisible = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            transferViewModel.status.collect { status ->
                when (status) {
                    TransferStatus.AWAITING_CONFIRMATION -> {}
                    TransferStatus.FAILED -> {}
                    TransferStatus.IN_PROGRESS -> {}
                    TransferStatus.SUCCESSFUL -> {}
                }
            }
        }
    }
}