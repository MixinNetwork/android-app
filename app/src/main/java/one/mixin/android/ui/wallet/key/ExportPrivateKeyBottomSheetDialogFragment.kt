package one.mixin.android.ui.wallet.key

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.core.view.isInvisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.walletconnect.util.randomBytes
import dagger.hilt.android.AndroidEntryPoint
import io.ipfs.multibase.binary.Base32
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.databinding.FragmentExportPrivateKeyBottomSheetBinding
import one.mixin.android.extension.visibleDisplayHeight
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.transfer.data.TransferStatus
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class ExportPrivateKeyBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PrivateKeyBottomSheetDialogFragment"

        fun newInstance() =
            ExportPrivateKeyBottomSheetDialogFragment()
    }

    private val keyViewModel by viewModels<ExportPrivateKeyViewModel>()

    private val binding by viewBinding(FragmentExportPrivateKeyBottomSheetBinding::inflate)

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
                // todo
                demoKey()
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
                        keyViewModel.key?.let {
                            key->
                            binding.blurOverlay.setKey(key)
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

    // todo test code, remove it
    private fun demoKey() {
        lifecycleScope.launch {
            keyViewModel.updateStatus(TransferStatus.IN_PROGRESS)
            val encode = Base32().encodeAsString(randomBytes(64))
            delay(3000)
            keyViewModel.success(encode)
        }
    }
}
