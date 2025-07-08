package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import one.mixin.android.databinding.FragmentAddWalletBottomSheetBinding
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.widget.BottomSheet

class AddWalletBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "AddWalletBottomSheetDialogFragment"
        fun newInstance() = AddWalletBottomSheetDialogFragment()
    }

    private val binding by lazy {
        FragmentAddWalletBottomSheetBinding.inflate(LayoutInflater.from(context))
    }

    var callback: (() -> Unit)? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }
        binding.apply {
            rightIv.setOnClickListener { dismiss() }
            importMnemonicPhrase.setOnClickListener {
                callback?.invoke()
                dismiss()
            }
        }
    }
}

