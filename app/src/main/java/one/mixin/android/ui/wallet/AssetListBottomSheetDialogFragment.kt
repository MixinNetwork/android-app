package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.databinding.ItemAssetBinding
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.dp
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView

@AndroidEntryPoint
class AssetListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "AssetListBottomSheetDialogFragment"

        fun newInstance() = AssetListBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentAssetListBottomSheetBinding::inflate)

    private val adapter = AssetAdapter {
        binding.searchEt.hideKeyboard()
        callback?.invoke(it)
        dismiss()
    }

    private var assets = listOf<AssetItem>()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight() + 48.dp
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.apply {
            closeIb.setOnClickListener {
                searchEt.hideKeyboard()
                dismiss()
            }
            assetRv.adapter = adapter
            searchEt.setHint(getString(R.string.search_placeholder_asset))
            searchEt.listener = object : SearchView.OnSearchViewListener {
                override fun afterTextChanged(s: Editable?) {
                    filter(s.toString())
                }

                override fun onSearch() {}
            }
        }
        bottomViewModel.assetItems().observe(this) {
            assets = it
            adapter.submitList(assets)
        }
    }

    private fun filter(s: String) {
        val assetList = assets.filter {
            it.name.containsIgnoreCase(s) || it.symbol.containsIgnoreCase(s)
        }.sortedByDescending { it.name.equalsIgnoreCase(s) || it.symbol.equalsIgnoreCase(s) }
        adapter.submitList(assetList)
    }

    fun setCallback(callback: (AssetItem) -> Unit): AssetListBottomSheetDialogFragment {
        this.callback = callback
        return this
    }

    private var callback: ((AssetItem) -> Unit)? = null

    class AssetAdapter(private val onItemClick: (AssetItem) -> Unit) : ListAdapter<AssetItem, ItemHolder>(AssetItem.DIFF_CALLBACK) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_asset, parent, false)
            )

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            val itemAssert = getItem(position)
            val binding = ItemAssetBinding.bind(holder.itemView)
            binding.typeAvatar.updateLayoutParams<MarginLayoutParams> {
                marginStart = 20.dp
            }
            binding.typeAvatar.bg.loadImage(itemAssert.iconUrl, R.drawable.ic_avatar_place_holder)
            binding.typeAvatar.badge.loadImage(itemAssert.chainIconUrl, R.drawable.ic_avatar_place_holder)
            binding.name.text = itemAssert.name
            binding.value.text = itemAssert.balance.numberFormat()
            binding.valueEnd.text = itemAssert.symbol
            holder.itemView.setOnClickListener {
                onItemClick(itemAssert)
            }
        }
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
