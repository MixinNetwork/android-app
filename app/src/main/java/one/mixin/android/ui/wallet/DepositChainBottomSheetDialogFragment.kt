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
import one.mixin.android.databinding.FragmentDepositChainBottomSheetBinding
import one.mixin.android.databinding.ItemDepositChainBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class DepositChainBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DepositChainBottomSheetDialogFragment"
        private const val ASSET = "asset"
        fun newInstance(asset: AssetItem) = DepositChainBottomSheetDialogFragment().withArgs {
            putParcelable(ASSET, asset)
        }
    }

    private val asset by lazy {
        requireArguments().getParcelable<AssetItem>(ASSET)
    }

    private val binding by viewBinding(FragmentDepositChainBottomSheetBinding::inflate)

    private val adapter = AssetAdapter()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.apply {
            assetRv.adapter = adapter
            adapter.submitList(mutableListOf(asset))
            adapter.callback = {
                dismiss()
            }
        }
    }

    class AssetAdapter : ListAdapter<AssetItem, ItemHolder>(AssetItem.DIFF_CALLBACK) {
        var callback: (() -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
            return ItemHolder(
                ItemDepositChainBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return super.getItemCount() + 1
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            if (position != 0) {
                getItem(position - 1)?.let { holder.bind(it, callback) }
            } else {
                holder.bind(null)
            }
        }
    }

    class ItemHolder(val binding: ItemDepositChainBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(assetItem: AssetItem?, callback: (() -> Unit)? = null) {
            if (assetItem == null) {
                binding.icon.isVisible = true
                binding.assetIcon.isVisible = false
                binding.content.setText(R.string.choose_network_content)
                binding.root.setBackgroundResource(R.drawable.bg_round_chain_yellow)
                binding.content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            } else {
                binding.icon.isVisible = false
                binding.assetIcon.isVisible = true
                binding.assetIcon.bg.loadImage(
                    assetItem.chainIconUrl,
                    R.drawable.ic_avatar_place_holder
                )
                binding.content.text = assetItem.chainName
                binding.root.setBackgroundResource(R.drawable.bg_round_chain)
                binding.content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                binding.root.setOnClickListener {
                    callback?.invoke()
                }
            }
        }
    }
}
