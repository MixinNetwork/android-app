package one.mixin.android.ui.home.web3.market

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.FragmentChooseTokensBottomSheetBinding
import one.mixin.android.databinding.ItemChooseTokenBinding
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet
import java.math.BigDecimal

@AndroidEntryPoint
class ChooseTokensBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "ChooseTokensBottomSheetDialogFragment"
        private const val ASSETS = "assets"

        fun newInstance(assets: ArrayList<TokenItem>) =
            ChooseTokensBottomSheetDialogFragment().withArgs {
                assets.sortBy { BigDecimal(it.balance) }
                putParcelableArrayList(ASSETS, assets)
            }
    }

    private val assets by lazy {
        requireArguments().getParcelableArrayListCompat(ASSETS, TokenItem::class.java)
    }

    private val binding by viewBinding(FragmentChooseTokensBottomSheetBinding::inflate)

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
            adapter.submitList(assets)
            adapter.callback = { t ->
                callback?.invoke(t)
                dismiss()
            }
        }
    }

    var callback: ((TokenItem) -> Unit)? = null

    class AssetAdapter : ListAdapter<TokenItem, ItemHolder>(TokenItem.DIFF_CALLBACK) {
        var callback: ((TokenItem) -> Unit)? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ItemHolder {
            return ItemHolder(
                ItemChooseTokenBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
            )
        }

        override fun onBindViewHolder(
            holder: ItemHolder,
            position: Int,
        ) {
            getItem(position)?.let { holder.bind(it, callback) }
        }
    }

    class ItemHolder(val binding: ItemChooseTokenBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(
            tokenItem: TokenItem,
            callback: ((TokenItem) -> Unit)? = null,
        ) {
            binding.assetIcon.loadToken(tokenItem)
            binding.chain.text = getChainName(tokenItem.chainId, tokenItem.chainName, tokenItem.assetKey)
            binding.value.text = "${tokenItem.balance} ${tokenItem.symbol}"
            binding.root.setOnClickListener {
                callback?.invoke(tokenItem)
            }
        }
    }
}
