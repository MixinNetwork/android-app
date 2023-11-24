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
class NetworkFeeBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "NetworkFeeBottomSheetDialogFragment"
        private const val ARGS_FEES = "args_fees"
        private const val ARGS_CURRENT_FEE = "args_current_fee"

        fun newInstance(
            fees: ArrayList<NetworkFee>,
            currentFee: String?,
        ) =
            NetworkFeeBottomSheetDialogFragment().withArgs {
                currentFee?.let { putString(ARGS_CURRENT_FEE, it) }
                putParcelableArrayList(ARGS_FEES, fees)
            }
    }

    private val binding by viewBinding(FragmentNetworkFeeBottomSheetBinding::inflate)

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
        val fees: ArrayList<NetworkFee> = requireArguments().getParcelableArrayListCompat(ARGS_FEES, NetworkFee::class.java) as ArrayList<NetworkFee>
        binding.apply {
            rightIv.setOnClickListener { dismiss() }
            val currentFee = requireArguments().getString(ARGS_CURRENT_FEE)
            val feeAdapter = FeeAdapter(currentFee ?: fees.first().token.assetId)
            feeAdapter.callback = { networkFee ->
                feeAdapter.currentFee = networkFee.token.assetId
                this@NetworkFeeBottomSheetDialogFragment.callback?.invoke(networkFee)
            }
            feeRv.adapter = feeAdapter
            feeAdapter.submitList(fees)
        }
    }

    var callback: ((NetworkFee) -> Unit)? = null

    class FeeAdapter(initCurrentFee: String) : ListAdapter<NetworkFee, NetworkFeeHolder>(NetworkFee.DIFF_CALLBACK) {
        var currentFee: String = initCurrentFee
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                if (value == field) return

                field = value
                notifyDataSetChanged()
            }
        var callback: ((NetworkFee) -> Unit)? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): NetworkFeeHolder =
            NetworkFeeHolder(
                ItemNetworkFeeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
            )

        override fun onBindViewHolder(
            holder: NetworkFeeHolder,
            position: Int,
        ) {
            getItem(position)?.let { holder.bind(it, currentFee, callback) }
        }
    }

    class NetworkFeeHolder(val binding: ItemNetworkFeeBinding) : ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(
            networkFee: NetworkFee,
            currentFee: String,
            callback: ((NetworkFee) -> Unit)? = null,
        ) {
            binding.apply {
                nameTv.text = networkFee.token.name
                feeTv.text = "${networkFee.fee} ${networkFee.token.symbol}"
                assetIcon.apply {
                    bg.loadImage(networkFee.token.iconUrl, R.drawable.ic_avatar_place_holder)
                    badge.loadImage(networkFee.token.chainIconUrl, R.drawable.ic_avatar_place_holder)
                }
                checkIv.isVisible = currentFee == networkFee.token.assetId
                root.setOnClickListener {
                    if (currentFee == networkFee.token.assetId) return@setOnClickListener

                    callback?.invoke(networkFee)
                }
            }
        }
    }
}

@Parcelize
data class NetworkFee(
    val token: TokenItem,
    val fee: String,
) : Parcelable {
    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<NetworkFee>() {
                override fun areItemsTheSame(
                    oldItem: NetworkFee,
                    newItem: NetworkFee,
                ) =
                    oldItem.token.assetId == newItem.token.assetId

                override fun areContentsTheSame(
                    oldItem: NetworkFee,
                    newItem: NetworkFee,
                ) =
                    oldItem == newItem
            }
    }
}
