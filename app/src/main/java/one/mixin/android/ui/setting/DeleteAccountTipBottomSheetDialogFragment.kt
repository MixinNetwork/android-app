package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.FragmentDeleteAccountTipBottomSheetBinding
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class DeleteAccountTipBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DeleteAccountTipBottomSheetDialogFragment"

        fun newInstance() = DeleteAccountTipBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentDeleteAccountTipBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).run {
            setCustomView(contentView)
            dismissClickOutside = false
        }

        binding.apply {
            continueTv.setOnClickListener {
                continueCallback?.invoke()
                dismiss()
            }
            viewWalletTv.setOnClickListener {
                dismiss()
                MainActivity.showWallet(requireContext())
            }
        }
    }

    fun setContinueCallback(callback: () -> Unit): DeleteAccountTipBottomSheetDialogFragment {
        continueCallback = callback
        return this
    }

    private var continueCallback: (() -> Unit)? = null
}
