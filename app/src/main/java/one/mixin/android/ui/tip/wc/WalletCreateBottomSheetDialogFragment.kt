package one.mixin.android.ui.tip.wc

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.core.view.isInvisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.databinding.FragmentWalletCreateBottomSheetBinding
import one.mixin.android.extension.visibleDisplayHeight
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.transfer.data.TransferStatus
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class WalletCreateBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "WalletCreateBottomSheetDialogFragment"

        fun newInstance() =
            WalletCreateBottomSheetDialogFragment()
    }

    private val keyViewModel by viewModels<WalletCreateViewModel>()

    private val binding by viewBinding(FragmentWalletCreateBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
            setCustomViewHeight(requireActivity().visibleDisplayHeight())
        }

        binding.apply {
            bottom.setOnClickListener({
                dismiss()
            }, {
                createAccount()
            }, {
                dismiss()
            })
        }

        lifecycleScope.launch {
            keyViewModel.status.collect { status ->
                when (status) {
                    TransferStatus.FAILED -> {
                        binding.contentVa.displayedChild = 0
                        binding.header.filed(keyViewModel.errorMessage)
                        binding.bottom.isInvisible = false
                    }

                    TransferStatus.SUCCESSFUL -> {
                        binding.header.success()
                        binding.contentVa.displayedChild = 1
                        keyViewModel.key?.let { key ->
                            binding.key.text = key
                        }
                        binding.bottom.isInvisible = false
                    }

                    TransferStatus.IN_PROGRESS -> {
                        binding.contentVa.displayedChild = 0
                        binding.header.progress()
                        binding.bottom.isInvisible = true
                    }

                    else -> {
                        binding.contentVa.displayedChild = 0
                        binding.bottom.isInvisible = false
                        binding.header.awaiting()
                    }
                }
            }
        }
    }

    private fun createAccount() {
        lifecycleScope.launch {
            keyViewModel.updateStatus(TransferStatus.IN_PROGRESS)
            delay(3000)
            keyViewModel.success("0x10fab41d2caCF05E3CE2123450Bda4AF8806F480")
        }
    }
}
