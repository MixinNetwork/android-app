package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAddFeeBottomSheetBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class AddFeeBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "AddFeeBottomSheetDialogFragment"
        private const val ARGS_TOKEN = "args_token"

        fun newInstance(networkFee: TokenItem) =
            AddFeeBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_TOKEN, networkFee)
            }
    }

    var onAction: ((type: ActionType, tokenItem: TokenItem) -> Unit)? = null

    enum class ActionType {
        SWAP, DEPOSIT
    }

    private val binding by viewBinding(FragmentAddFeeBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }
        val tokenItem = requireArguments().getParcelable<TokenItem>(ARGS_TOKEN)
        binding.apply {
            swapTv.text = getString(R.string.fee_swap, tokenItem?.symbol ?: "-")
            swapDescTv.text = getString(R.string.fee_swap_other_coin_to, tokenItem?.symbol ?: "-")
            depositTv.text = getString(R.string.fee_deposit, tokenItem?.symbol ?: "-")
            swapLayout.setOnClickListener {
                tokenItem?.let { onAction?.invoke(ActionType.SWAP, it) }
                dismiss()
            }
            depositLayout.setOnClickListener {
                tokenItem?.let { onAction?.invoke(ActionType.DEPOSIT, it) }
                dismiss()
            }
        }
    }
}

