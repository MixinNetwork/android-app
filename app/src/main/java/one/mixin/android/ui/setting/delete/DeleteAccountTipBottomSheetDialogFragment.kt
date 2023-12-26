package one.mixin.android.ui.setting.delete

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDeleteAccountTipBottomSheetBinding
import one.mixin.android.databinding.ItemAssetBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class DeleteAccountTipBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DepositTipBottomSheetDialogFragment"

        fun newInstance() = DeleteAccountTipBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentDeleteAccountTipBottomSheetBinding::inflate)
    private val chatViewModel by viewModels<ConversationViewModel>()
    private val adapter = TypeAdapter()

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).run {
            setCustomView(contentView)
            dismissClickOutside = false
        }

        binding.apply {
            close.setOnClickListener {
                dismiss()
            }
            continueTv.setOnClickListener {
                continueCallback?.invoke(this@DeleteAccountTipBottomSheetDialogFragment)
            }
            viewWalletTv.setOnClickListener {
                dismiss()
                MainActivity.showWallet(requireContext())
            }
            assetRv.adapter = adapter
        }
        chatViewModel.assetItemsWithBalance().observe(
            this,
        ) { r: List<TokenItem>? ->
            if (r != null && r.isNotEmpty()) {
                assets = r
                adapter.submitList(r)
            }
        }
    }

    private var assets = listOf<TokenItem>()

    fun setContinueCallback(callback: (DialogFragment) -> Unit): DeleteAccountTipBottomSheetDialogFragment {
        continueCallback = callback
        return this
    }

    private var continueCallback: ((DialogFragment) -> Unit)? = null

    class TypeAdapter : ListAdapter<TokenItem, ItemHolder>(TokenItem.DIFF_CALLBACK) {
        private var typeListener: OnTypeClickListener? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ItemHolder =
            ItemHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_asset, parent, false),
            )

        override fun onBindViewHolder(
            holder: ItemHolder,
            position: Int,
        ) {
            val itemAssert = getItem(position)
            val binding = ItemAssetBinding.bind(holder.itemView)
            binding.typeAvatar.bg.loadImage(itemAssert.iconUrl, R.drawable.ic_avatar_place_holder)
            binding.typeAvatar.badge.loadImage(
                itemAssert.chainIconUrl,
                R.drawable.ic_avatar_place_holder,
            )
            binding.name.text = itemAssert.name
            binding.value.text = itemAssert.balance.numberFormat()
            binding.valueEnd.text = itemAssert.symbol
            holder.itemView.setOnClickListener {
                typeListener?.onTypeClick(itemAssert)
            }
        }
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface OnTypeClickListener {
        fun onTypeClick(asset: TokenItem)
    }
}
