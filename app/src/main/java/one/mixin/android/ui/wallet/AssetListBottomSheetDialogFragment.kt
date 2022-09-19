package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.textChanges
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.databinding.ItemAssetBinding
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.dp
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AssetListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "AssetListBottomSheetDialogFragment"
        const val ARGS_FOR_SEND = "args_for_send"

        const val POS_RV = 0
        const val POS_EMPTY = 1

        fun newInstance(forSend: Boolean) = AssetListBottomSheetDialogFragment().withArgs {
            putBoolean(ARGS_FOR_SEND, forSend)
        }
    }

    private val binding by viewBinding(FragmentAssetListBottomSheetBinding::inflate)

    private val adapter = AssetAdapter { assetItem ->
        binding.searchEt.hideKeyboard()

        if (!forSend && assetItem.getDestination().isEmpty()) {
            lifecycleScope.launch {
                val dialog = indeterminateProgressDialog(
                    message = R.string.Please_wait_a_bit,
                ).apply {
                    setCancelable(false)
                }
                dialog.show()
                val asset = bottomViewModel.findOrSyncAsset(assetItem.assetId)
                dialog.dismiss()

                if (asset == null) return@launch

                callback?.invoke(asset)
                dismiss()
            }
        } else {
            callback?.invoke(assetItem)
            dismiss()
        }
    }

    private val forSend: Boolean by lazy {
        requireArguments().getBoolean(ARGS_FOR_SEND)
    }

    private var disposable: Disposable? = null
    private var currentSearch: Job? = null
    private var currentQuery: String = ""
    private var defaultAssets = emptyList<AssetItem>()

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
            if (forSend) {
                searchEt.listener = object : SearchView.OnSearchViewListener {
                    override fun afterTextChanged(s: Editable?) {
                        filter(s.toString())
                    }

                    override fun onSearch() {}
                }
            } else {
                @SuppressLint("AutoDispose")
                disposable = searchEt.et.textChanges().debounce(500L, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            if (it.isNullOrBlank()) {
                                binding.rvVa.displayedChild = POS_RV
                                adapter.submitList(defaultAssets)
                            } else {
                                if (it.toString() != currentQuery) {
                                    currentQuery = it.toString()
                                    search(it.toString())
                                }
                            }
                        },
                        {}
                    )
            }
        }
        bottomViewModel.assetItems().observe(this) {
            defaultAssets = it
            if (binding.searchEt.et.text.isNullOrBlank()) {
                adapter.submitList(it)
            }
        }
    }

    private fun filter(s: String) {
        val assetList = defaultAssets.filter {
            it.name.containsIgnoreCase(s) || it.symbol.containsIgnoreCase(s)
        }.sortedByDescending { it.name.equalsIgnoreCase(s) || it.symbol.equalsIgnoreCase(s) }
        adapter.submitList(assetList)
    }

    private fun search(query: String) {
        currentSearch?.cancel()
        currentSearch = lifecycleScope.launch {
            if (!isAdded) return@launch

            binding.rvVa.displayedChild = POS_RV
            binding.pb.isVisible = true

            val localAssets = bottomViewModel.fuzzySearchAssets(query)
            adapter.submitList(localAssets)

            val remoteAssets = bottomViewModel.queryAsset(query)
            val result = sortQueryAsset(query, localAssets, remoteAssets)

            adapter.submitList(result)
            binding.pb.isVisible = false

            if (localAssets.isNullOrEmpty() && remoteAssets.isEmpty()) {
                binding.rvVa.displayedChild = POS_EMPTY
            }
        }
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
