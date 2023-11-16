package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.adapter.SearchAdapter
import one.mixin.android.ui.wallet.adapter.WalletSearchCallback
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AssetListFixedBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "AssetListFixedBottomSheetDialogFragment"
        const val ARGS_ASSETS = "args_assets"

        fun newInstance(assets: ArrayList<String>? = null) =
            AssetListFixedBottomSheetDialogFragment().withArgs {
                putStringArrayList(ARGS_ASSETS, assets)
            }
    }

    private val binding by viewBinding(FragmentAssetListBottomSheetBinding::inflate)

    private val adapter = SearchAdapter()

    private val assetIds by lazy {
        requireArguments().getStringArrayList(ARGS_ASSETS)?.toList() ?: emptyList()
    }

    private var disposable: Disposable? = null
    private var defaultAssets = emptyList<TokenItem>()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight() + requireContext().appCompatActionBarHeight()
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
            adapter.callback =
                object : WalletSearchCallback {
                    override fun onAssetClick(
                        assetId: String,
                        tokenItem: TokenItem?,
                    ) {
                        binding.searchEt.hideKeyboard()
                        tokenItem?.let { onAsset?.invoke(it) }
                        dismiss()
                    }
                }
            searchEt.setHint(getString(R.string.search_placeholder_asset))

            @SuppressLint("AutoDispose")
            disposable =
                searchEt.et.textChanges().debounce(500L, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(destroyScope)
                    .subscribe(
                        {
                            if (it.isNullOrBlank()) {
                                adapter.submitList(defaultAssets)
                            } else {
                                filter(it.toString())
                            }
                        },
                        {},
                    )
        }

        bottomViewModel.assetItems(assetIds).observe(this) {
            defaultAssets = it
            val s = binding.searchEt.et.text
            if (s.isNullOrBlank()) {
                adapter.submitList(defaultAssets)
            } else {
                filter(s.toString())
            }
        }
    }

    private fun filter(s: String) {
        val assetList =
            defaultAssets.filter {
                it.name.containsIgnoreCase(s) || it.symbol.containsIgnoreCase(s)
            }.sortedByDescending { it.name.equalsIgnoreCase(s) || it.symbol.equalsIgnoreCase(s) }
        adapter.submitList(assetList) {
            binding.assetRv.scrollToPosition(0)
        }
    }

    fun setOnAssetClick(callback: (TokenItem) -> Unit): AssetListFixedBottomSheetDialogFragment {
        this.onAsset = callback
        return this
    }

    private var onAsset: ((TokenItem) -> Unit)? = null
}
