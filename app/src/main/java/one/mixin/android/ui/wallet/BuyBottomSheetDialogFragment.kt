package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.FragmentBuyBottomBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class BuyBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "BuyBottomSheetDialogFragment"

        fun newInstance() = BuyBottomSheetDialogFragment().withArgs {
        }
    }

    private val binding by viewBinding(FragmentBuyBottomBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            titleView.apply {
                rightIv.setOnClickListener { dismiss() }
            }
            verifiedRl.setOnClickListener {
                dismiss()
                onVerifiedClick?.invoke()
            }
            unverifiedRl.setOnClickListener {
                dismiss()
                onUnverifiedClick?.invoke()
            }
        }
    }

    var onUnverifiedClick: (() -> Unit)? = null
    var onVerifiedClick: (() -> Unit)? = null
}
