package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAddFeeBottomSheetBinding
import one.mixin.android.databinding.FragmentNetworkFeeBottomSheetBinding
import one.mixin.android.databinding.ItemNetworkFeeBinding
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class AddFeeBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "AddFeeBottomSheetDialogFragment"
        private const val ARGS_NETWORK_FEE = "args_network_fee"

        fun newInstance(networkFee: NetworkFee) =
            AddFeeBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_NETWORK_FEE, networkFee)
            }
    }

    var onAction: ((type: ActionType, networkFee: NetworkFee) -> Unit)? = null

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
        val networkFee = requireArguments().getParcelable<NetworkFee>(ARGS_NETWORK_FEE)
        binding.apply {
            swapTv.text = getString(R.string.fee_swap, networkFee?.token?.symbol ?: "-")
            swapDescTv.text = getString(R.string.fee_swap_other_coin_to, networkFee?.token?.symbol ?: "-")
            depositTv.text = getString(R.string.fee_deposit, networkFee?.token?.symbol ?: "-")
            swapLayout.setOnClickListener {
                networkFee?.let { onAction?.invoke(ActionType.SWAP, it) }
                dismiss()
            }
            depositLayout.setOnClickListener {
                networkFee?.let { onAction?.invoke(ActionType.DEPOSIT, it) }
                dismiss()
            }
        }
    }
}
