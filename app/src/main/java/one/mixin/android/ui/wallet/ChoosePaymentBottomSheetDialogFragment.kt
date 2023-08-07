package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentBottomChoosePaymenmtBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class ChoosePaymentBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "ChoosePaymentBottomSheetDialogFragment"
        const val ARGS_IS_GOOGLE_PAY = "args_is_google_pay"

        fun newInstance(isGooglePay: Boolean) = ChoosePaymentBottomSheetDialogFragment().withArgs {
            putBoolean(ARGS_IS_GOOGLE_PAY, isGooglePay)
        }
    }

    private val binding by viewBinding(FragmentBottomChoosePaymenmtBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        val isGooglePay = requireArguments().getBoolean(ARGS_IS_GOOGLE_PAY)
        binding.apply {
            payCheckIv.isVisible = isGooglePay
            creditCheckIv.isVisible = !isGooglePay
            // Todo real data
            payDesc.text = getString(R.string.Gateway_fee_price, "1.99%")
            creditDesc.text = getString(R.string.Gateway_fee_price, "1.99%")
            titleView.apply {
                rightIv.setOnClickListener { dismiss() }
            }
            payRl.setOnClickListener {
                dismiss()
                onPaymentClick?.invoke(true)
            }
            creditRl.setOnClickListener {
                dismiss()
                onPaymentClick?.invoke(false)
            }
        }
    }

    var onPaymentClick: ((Boolean) -> Unit)? = null
}
