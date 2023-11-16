package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDepositChooseNetworkBottomSheetBinding
import one.mixin.android.databinding.ItemChooseNetworkBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class DepositChooseNetworkBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DepositChooseNetworkBottomSheetDialogFragment"
        private const val ASSET = "asset"

        fun newInstance(asset: TokenItem) =
            DepositChooseNetworkBottomSheetDialogFragment().withArgs {
                putParcelable(ASSET, asset)
            }
    }

    private val asset by lazy {
        requireArguments().getParcelableCompat(ASSET, TokenItem::class.java)
    }

    private val binding by viewBinding(FragmentDepositChooseNetworkBottomSheetBinding::inflate)

    private val adapter = AssetAdapter()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        dialog.setCancelable(false)
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.apply {
            assetRv.adapter = adapter
            adapter.submitList(mutableListOf(asset))
            adapter.callback = {
                callback?.invoke()
                dismiss()
            }
        }
    }

    var callback: (() -> Unit)? = null

    class AssetAdapter : ListAdapter<TokenItem, ItemHolder>(TokenItem.DIFF_CALLBACK) {
        var callback: (() -> Unit)? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ItemHolder {
            return ItemHolder(
                ItemChooseNetworkBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
            )
        }

        override fun getItemCount(): Int {
            return super.getItemCount() + 1
        }

        override fun onBindViewHolder(
            holder: ItemHolder,
            position: Int,
        ) {
            if (position != 0) {
                getItem(position - 1)?.let { holder.bind(it, callback) }
            } else {
                holder.bind(null)
            }
        }
    }

    class ItemHolder(val binding: ItemChooseNetworkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            tokenItem: TokenItem?,
            callback: (() -> Unit)? = null,
        ) {
            if (tokenItem == null) {
                binding.icon.isVisible = true
                binding.assetIcon.isVisible = false
                binding.content.setText(R.string.Choose_network_tip)
                binding.root.setBackgroundResource(R.drawable.bg_round_choose_network_yellow)
                binding.content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            } else {
                binding.icon.isVisible = false
                binding.assetIcon.isVisible = true
                binding.assetIcon.bg.loadImage(
                    tokenItem.chainIconUrl,
                    R.drawable.ic_avatar_place_holder,
                )
                binding.content.text = getChainName(tokenItem.chainId, tokenItem.chainName, tokenItem.assetKey)
                binding.root.setBackgroundResource(R.drawable.bg_round_choose_network)
                binding.content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                binding.root.setOnClickListener {
                    callback?.invoke()
                }
            }
        }
    }
}
