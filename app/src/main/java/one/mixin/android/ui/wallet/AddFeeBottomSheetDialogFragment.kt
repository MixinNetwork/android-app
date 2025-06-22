package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAddFeeBottomSheetBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
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
        private const val ARGS_WEB3_TOKEN = "args_web3_token"

        fun newInstance(networkFee: TokenItem) =
            AddFeeBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_TOKEN, networkFee)
            }

        fun newInstance(fee: Web3TokenItem) =
            AddFeeBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_WEB3_TOKEN, fee)
            }
    }

    var onAction: ((type: ActionType, tokenItem: TokenItem) -> Unit)? = null
    var onWeb3Action: ((type: ActionType, tokenItem: Web3TokenItem) -> Unit)? = null

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
        val tokenItem = requireArguments().getParcelable<TokenItem?>(ARGS_TOKEN)
        val web3TokenItem = requireArguments().getParcelable<Web3TokenItem?>(ARGS_WEB3_TOKEN)
        binding.apply {
            rightIv.setOnClickListener {
                dismiss()
            }
            if (tokenItem != null) {
                swapTv.text = getString(R.string.fee_swap, tokenItem.symbol ?: "-")
                swapDescTv.text = getString(R.string.fee_swap_other_coin_to, tokenItem.symbol ?: "-")
                depositTv.text = getString(R.string.fee_deposit, tokenItem.symbol ?: "-")
                swapLayout.setOnClickListener {
                    onAction?.invoke(ActionType.SWAP, tokenItem)
                    dismiss()
                }
                depositLayout.setOnClickListener {
                    onAction?.invoke(ActionType.DEPOSIT, tokenItem)
                    dismiss()
                }
            } else if (web3TokenItem != null) {
                swapTv.text = getString(R.string.fee_swap, web3TokenItem.symbol ?: "-")
                swapDescTv.text = getString(R.string.fee_swap_other_coin_to, web3TokenItem.symbol ?: "-")
                depositTv.text = getString(R.string.fee_deposit, web3TokenItem.symbol ?: "-")
                swapLayout.setOnClickListener {
                    onWeb3Action?.invoke(ActionType.SWAP, web3TokenItem)
                    dismiss()
                }
                depositLayout.setOnClickListener {
                    onWeb3Action?.invoke(ActionType.DEPOSIT, web3TokenItem)
                    dismiss()
                }
            }
        }
    }
}
