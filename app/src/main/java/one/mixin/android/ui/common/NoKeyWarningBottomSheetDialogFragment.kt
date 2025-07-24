package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import one.mixin.android.databinding.FragmentNoKeyWarningBottomSheetBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

class NoKeyWarningBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "NoKeyWarningBottomSheetDialogFragment"
        private const val ARG_WALLET_NAME = "arg_wallet_name"

        fun newInstance(walletName: String): NoKeyWarningBottomSheetDialogFragment {
            return NoKeyWarningBottomSheetDialogFragment().withArgs {
                putString(ARG_WALLET_NAME, walletName)
            }
        }
    }

    private val binding by viewBinding(FragmentNoKeyWarningBottomSheetBinding::inflate)
    private val walletName: String by lazy {
        requireArguments().getString(ARG_WALLET_NAME, "")
    }

    var onConfirm: (() -> Unit)? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).run {
            setCustomView(contentView)
        }

        binding.titleTv.text = walletName
        binding.confirmButton.setOnClickListener {
            onConfirm?.invoke()
            dismiss()
        }
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }
}