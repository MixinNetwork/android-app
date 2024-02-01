package one.mixin.android.ui.wallet.transfer

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.databinding.FragmentTransferBottomSheetBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.conversation.PreconditionBottomSheetDialogFragment
import one.mixin.android.ui.wallet.transfer.data.Transfer
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class TransferBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "TransferBottomSheetDialogFragment"
        private const val ARGS_TRANSFER = "args_transfer"

        fun newInstance(
        ) =
            TransferBottomSheetDialogFragment().withArgs {
                // putParcelable(ARGS_TRANSFER, transfer)
            }
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            transferViewModel.status.collect {

            }
        }
    }
}