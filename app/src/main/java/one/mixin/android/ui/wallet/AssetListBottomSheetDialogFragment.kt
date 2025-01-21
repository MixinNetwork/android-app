package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId.BinanceSmartChain
import one.mixin.android.Constants.ChainId.Base
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.Polygon
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.Constants.ChainId.TRON_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.extension.addToList
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.Components.RecentTokens
import one.mixin.android.ui.wallet.adapter.SearchAdapter
import one.mixin.android.ui.wallet.adapter.WalletSearchCallback
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AssetListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "AssetListBottomSheetDialogFragment"
        const val ARGS_FOR_TYPE = "args_for_type"
        const val ARGS_ASSETS = "args_assets"
        const val ARGS_ASSET_ID = "args_asset_id"

        const val POS_RV = 0
        const val POS_EMPTY_RECEIVE = 1
        const val POS_EMPTY_SEND = 2

        const val TYPE_FROM_SEND = 0
        const val TYPE_FROM_RECEIVE = 1
        const val TYPE_FROM_TRANSFER = 2

        fun newInstance(
            fromType: Int,
            assets: ArrayList<String>? = null,
            currentAssetId: String? = null,
        ) =
            AssetListBottomSheetDialogFragment().withArgs {
                putInt(ARGS_FOR_TYPE, fromType)
                putStringArrayList(ARGS_ASSETS, assets)
                putString(ARGS_ASSET_ID, currentAssetId)
            }
    }

    private val binding by viewBinding(FragmentAssetListBottomSheetBinding::inflate)

    private val fromType: Int by lazy {
        requireArguments().getInt(ARGS_FOR_TYPE)
    }

    private val key by lazy {
        when (fromType) {
            TYPE_FROM_SEND, TYPE_FROM_TRANSFER -> Constants.Account.PREF_WALLET_SEND
            TYPE_FROM_RECEIVE -> Constants.Account.PREF_WALLET_RECEIVE
            else -> Constants.Account.PREF_WALLET_SEND
        }
    }

    private val adapter by lazy { SearchAdapter(requireArguments().getString(ARGS_ASSET_ID)) }

    private val assetIds by lazy {
        requireArguments().getStringArrayList(ARGS_ASSETS)
    }

    private var disposable: Disposable? = null
    private var currentSearch: Job? = null
    private var currentQuery: String = ""
    private var defaultAssets = emptyList<TokenItem>()
    private var currentChain: String? = null

    private fun initRadio() {
        binding.apply {
            radio.isVisible = true
            radioAll.isChecked = true
            radioAll.isVisible = true
            radioEth.isVisible = true
            radioTron.isVisible = true
            radioBase.isVisible = true
            radioBsc.isVisible = true
            radioPolygon.isVisible = true
            radioGroup.setOnCheckedChangeListener { _, id ->
                currentChain = when (id) {
                    R.id.radio_eth -> {
                        ETHEREUM_CHAIN_ID
                    }

                    R.id.radio_solana -> {
                        SOLANA_CHAIN_ID
                    }

                    R.id.radio_base -> {
                        Base
                    }

                    R.id.radio_tron -> {
                        TRON_CHAIN_ID
                    }

                    R.id.radio_bsc -> {
                        BinanceSmartChain
                    }

                    R.id.radio_polygon -> {
                        Polygon
                    }

                    else -> {
                        null
                    }
                }
                filter(searchEt.et.text?.toString() ?: "")
            }
        }
    }

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
            initRadio()
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
                        tokenItem?.let {
                            defaultSharedPreferences.addToList(key, it.assetId)
                            onAsset?.invoke(it)
                        }
                        dismiss()
                    }
                }
            searchEt.setHint(getString(R.string.search_placeholder_asset))
            if (fromType == TYPE_FROM_SEND) {
                depositTv.setOnClickListener {
                    onDeposit?.invoke()
                    dismiss()
                }
            }
            if (fromType == TYPE_FROM_SEND || fromType == TYPE_FROM_TRANSFER) {
                searchEt.listener =
                    object : SearchView.OnSearchViewListener {
                        override fun afterTextChanged(s: Editable?) {
                            filter(s.toString())
                        }

                        override fun onSearch() {}
                    }
            } else {
                @SuppressLint("AutoDispose")
                disposable =
                    searchEt.et.textChanges().debounce(500L, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .autoDispose(destroyScope)
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
                            {},
                        )
            }
        }

        if (fromType == TYPE_FROM_SEND || fromType == TYPE_FROM_TRANSFER) {
            bottomViewModel.assetItemsWithBalance()
        } else {
            bottomViewModel.assetItems()
        }.observe(this) {
            defaultAssets =
                it.let { list ->
                    if (!assetIds.isNullOrEmpty()) {
                        list.filter { item -> assetIds!!.contains(item.assetId) }
                    } else {
                        list
                    }
                }
            if (fromType == TYPE_FROM_SEND) {
                adapter.submitList(defaultAssets)
                if (defaultAssets.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY_SEND
                } else {
                    binding.rvVa.displayedChild = POS_RV
                }
            } else {
                if (binding.searchEt.et.text.isNullOrBlank()) {
                    adapter.submitList(defaultAssets)
                }
            }
        }
    }

    private val composeId by lazy {
        View.generateViewId()
    }

    override fun onStart() {
        super.onStart()
        binding.apply {
            root.findViewById<ComposeView>(composeId).let {
                if (it == null) {
                    val composeView = ComposeView(requireContext()).apply {
                        id = View.generateViewId()
                        setContent {
                            RecentTokens (key) {
                                defaultSharedPreferences.addToList(key, it.assetId)
                                this@AssetListBottomSheetDialogFragment.onAsset?.invoke(it)
                                dismiss()
                            }
                        }
                    }

                    root.addView(
                        composeView,
                        RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            addRule(RelativeLayout.BELOW, searchView.id)
                        })

                    radio.updateLayoutParams<RelativeLayout.LayoutParams> {
                        addRule(RelativeLayout.BELOW, composeView.id)
                    }
                    root.requestLayout()
                    root.invalidate()
                }
            }
        }
    }

    private fun filter(s: String) {
        val assetList =
            defaultAssets.filter {
                it.name.containsIgnoreCase(s) || it.symbol.containsIgnoreCase(s)
            }.sortedByDescending { it.name.equalsIgnoreCase(s) || it.symbol.equalsIgnoreCase(s) }.filter { item ->
                ((currentChain != null && item.chainId == currentChain) || currentChain == null)
            }
        adapter.submitList(assetList) {
            binding.assetRv.scrollToPosition(0)
        }
    }

    private fun search(query: String) {
        currentSearch?.cancel()
        currentSearch =
            lifecycleScope.launch {
                if (!isAdded) return@launch

                binding.rvVa.displayedChild = POS_RV
                binding.pb.isVisible = true

                val localAssets =
                    bottomViewModel.fuzzySearchAssets(query).let { list ->
                        if (!assetIds.isNullOrEmpty()) {
                            list?.filter { item -> assetIds!!.contains(item.assetId) }
                        } else {
                            list
                        }
                    }?.filter { item ->
                        ((currentChain != null && item.chainId == currentChain) || currentChain == null)
                    }
                adapter.submitList(localAssets)

                val remoteAssets = bottomViewModel.queryAsset(query).filter { item ->
                    ((currentChain != null && item.chainId == currentChain) || currentChain == null)
                }
                val result = sortQueryAsset(query, localAssets, remoteAssets)

                adapter.submitList(result) {
                    binding.assetRv.scrollToPosition(0)
                }
                binding.pb.isVisible = false

                if (localAssets.isNullOrEmpty() && remoteAssets.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY_RECEIVE
                }
            }
    }

    fun setOnAssetClick(callback: (TokenItem) -> Unit): AssetListBottomSheetDialogFragment {
        this.onAsset = callback
        return this
    }

    fun setOnDepositClick(callback: () -> Unit): AssetListBottomSheetDialogFragment {
        this.onDeposit = callback
        return this
    }

    private var onAsset: ((TokenItem) -> Unit)? = null
    private var onDeposit: (() -> Unit)? = null
}
