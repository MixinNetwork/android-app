package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import one.mixin.android.R
import one.mixin.android.databinding.FragmentBlockConfirmationsBottomSheetBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

class BlockConfirmationsBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "BlockConfirmationsBottomSheetDialogFragment"

        private const val KEY_CONFIRMATIONS = "key_confirmations"

        fun newInstance(confirmations: Int) = BlockConfirmationsBottomSheetDialogFragment().withArgs {
            putInt(KEY_CONFIRMATIONS, confirmations)
        }
    }

    private val binding by viewBinding(FragmentBlockConfirmationsBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        val confirmations = arguments?.getInt(KEY_CONFIRMATIONS) ?: 0
        binding.contentTv.text = getString(R.string.block_confirmations_content, confirmations)
        (dialog as BottomSheet).run {
            setCustomView(contentView)
        }

        binding.gotItBtn.setOnClickListener {
            dismiss()
        }
    }
}
