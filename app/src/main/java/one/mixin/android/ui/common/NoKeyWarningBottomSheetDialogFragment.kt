package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import one.mixin.android.R
import one.mixin.android.databinding.FragmentNoKeyWarningBottomSheetBinding
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.db.web3.vo.isWatch
import one.mixin.android.extension.withArgs
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

class NoKeyWarningBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "NoKeyWarningBottomSheetDialogFragment"
        private const val ARG_WALLET = "arg_wallet"

        fun newInstance(wallet: Web3Wallet): NoKeyWarningBottomSheetDialogFragment {
            return NoKeyWarningBottomSheetDialogFragment().withArgs {
                putParcelable(ARG_WALLET, wallet)
            }
        }
    }

    private val binding by viewBinding(FragmentNoKeyWarningBottomSheetBinding::inflate)
    private val wallet: Web3Wallet by lazy {
        requireArguments().getParcelable<Web3Wallet>(ARG_WALLET)!!
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

        binding.titleTv.text = wallet.name

        val titleDrawable = if (wallet.isWatch()) {
            R.drawable.ic_wallet_watch
        } else {
            null
        }

        binding.titleTv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, titleDrawable ?: 0, 0)
        val messageResId = if (wallet.isWatch()) {
            R.string.watch_only_wallet_warning_message
        } else if (!wallet.hasLocalPrivateKey) {
            R.string.no_key_wallet_warning_message
        } else {
            R.string.watch_only_wallet_warning_message
        }
        binding.messageTv.setText(messageResId)
        binding.confirmButton.setOnClickListener {
            onConfirm?.invoke()
            dismiss()
        }
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }
}