package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentIdentityVerificationStateBottomBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.sumsub.KycState
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class IdentityVerificationStateBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "IdentityVerificationStateBottomSheetDialogFragment"
        const val ARGS_KYC_STATE = "args_kyc_state"
        const val ARGS_TOKEN = "args_token"

        fun newInstance(kycState: String, token: String?) = IdentityVerificationStateBottomSheetDialogFragment().withArgs {
            putString(ARGS_KYC_STATE, kycState)
            token?.let { putString(ARGS_TOKEN, it) }
        }
    }

    private val binding by viewBinding(FragmentIdentityVerificationStateBottomBinding::inflate)

    private lateinit var kycState: String
    private var token: String? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        kycState = requireNotNull(requireArguments().getString(ARGS_KYC_STATE)) { "required kycState can no be null"}
        token = requireArguments().getString(ARGS_TOKEN)
        binding.apply {
            when (kycState) {
                KycState.PENDING.value -> {
                    imageView.setImageResource(R.drawable.ic_identity_verifying)
                    titleTv.setText(R.string.Identity_Verifying)
                    tipTv.setText(R.string.identity_verifying_tip)
                    okTv.setText(R.string.OK)
                    updateTip(false)
                    okTv.setOnClickListener { dismiss() }
                }
                KycState.RETRY.value -> {
                    imageView.setImageResource(R.drawable.ic_verification_failed)
                    titleTv.setText(R.string.Verification_Failed)
                    tipTv.setText(R.string.identity_verification_failed_tip)
                    okTv.setText(R.string.Continue)
                    updateTip(false)
                    okTv.setOnClickListener {
                        token?.let { t ->
                            onRetry?.invoke(t)
                        }
                        dismiss()
                    }
                }
//                IdentityVerificationState.Additional -> {
//                    imageView.setImageResource(R.drawable.ic_identity_verifying)
//                    titleTv.setText(R.string.Additional_Verification)
//                    tipTv.setText(R.string.identity_additional_verification_tip)
//                    okTv.setText(R.string.OK)
//                    updateTip(false)
//                }
                KycState.BLOCKED.value -> {
                    imageView.setImageResource(R.drawable.ic_identity_verifying)
                    titleTv.setText(R.string.Service_Unavailable)
                    tipTv.setText(R.string.identity_service_unavailable_tip)
                    okTv.setText(R.string.OK)
                    updateTip(true)
                    okTv.setOnClickListener { dismiss() }
                }
            }
        }
    }

    private fun updateTip(isWarning: Boolean) {
        binding.apply {
            if (isWarning) {
                tipTv.updateLayoutParams<MarginLayoutParams> {
                    topMargin = 18.dp
                    bottomMargin = 32.dp
                    leftMargin = 22.dp
                    rightMargin = 22.dp
                }
                tipTv.setBackgroundResource(R.drawable.bg_round_8_solid_gray)
                tipTv.setTextColor(requireContext().getColor(R.color.colorRed))
                tipTv.setPadding(14.dp, 12.dp, 14.dp, 12.dp)
            } else {
                tipTv.updateLayoutParams<MarginLayoutParams> {
                    topMargin = 32.dp
                    bottomMargin = 68.dp
                    leftMargin = 36.dp
                    rightMargin = 36.dp
                }
                tipTv.setBackgroundResource(0)
                tipTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_primary))
                tipTv.setPadding(0, 0, 0, 0)
            }
        }
    }

    var onRetry: ((String) -> Unit)? = null
}
