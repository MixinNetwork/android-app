package one.mixin.android.web3.receive

import android.annotation.SuppressLint
import android.app.Dialog
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
import one.mixin.android.Constants.ChainId
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.addToList
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.components.RecentTokens
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class Web3TokenListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "Web3TokenListBottomSheetDialogFragment"

        const val POS_RV = 0
        const val POS_EMPTY_RECEIVE = 1
        const val POS_EMPTY_SEND = 2

        const val TYPE_FROM_SEND = 0
        const val TYPE_FROM_RECEIVE = 1

        const val WEB3_ASSET_PREFERENCE = "WEB3_TRANSFER_ASSET"

        fun newInstance(type: Int = TYPE_FROM_SEND): Web3TokenListBottomSheetDialogFragment {
            return Web3TokenListBottomSheetDialogFragment().apply {
                this.type = type
            }
        }
    }

    private val binding by viewBinding(FragmentAssetListBottomSheetBinding::inflate)

    private val adapter by lazy { Web3TokenAdapter() }

    private var disposable: Disposable? = null
    private var currentSearch: Job? = null
    private var currentQuery: String = ""
    private var defaultAssets = emptyList<Web3TokenItem>()
    private var type: Int = TYPE_FROM_SEND
    private var currentChain: String? = null

    private val key by lazy {
        when (type) {
            TYPE_FROM_SEND -> Constants.Account.PREF_WALLET_SEND
            TYPE_FROM_RECEIVE -> Constants.Account.PREF_WALLET_RECEIVE
            else -> Constants.Account.PREF_WALLET_SEND
        }
    }

    private fun initRadio() {
        binding.apply {
            radio.isVisible = true
            radioAll.isChecked = true
            radioAll.isVisible = true
            radioEth.isVisible = true
            radioTron.isVisible = false
            radioBase.isVisible = true
            radioBsc.isVisible = true
            radioPolygon.isVisible = true
            radioSolana.isVisible = true
            radioGroup.setOnCheckedChangeListener { _, id ->
                currentChain = when (id) {
                    R.id.radio_eth -> {
                        ChainId.ETHEREUM_CHAIN_ID
                    }

                    R.id.radio_solana -> {
                        ChainId.SOLANA_CHAIN_ID
                    }

                    R.id.radio_base -> {
                        ChainId.Base
                    }

                    R.id.radio_bsc -> {
                        ChainId.BinanceSmartChain
                    }

                    R.id.radio_polygon -> {
                        ChainId.Polygon
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
            adapter.setOnClickListener { tokenItem ->
                searchEt.hideKeyboard()
                if (asyncOnAsset != null) {
                    asyncClick(tokenItem)
                } else {
                    requireContext().defaultSharedPreferences.addToList(key, tokenItem.assetId)
                    onAsset?.invoke(tokenItem)
                }
                dismiss()
            }
            searchEt.setHint(getString(R.string.search_placeholder_asset))
            depositTv.isVisible = false

            @SuppressLint("AutoDispose")
            disposable =
                searchEt.et.textChanges().debounce(500L, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(destroyScope)
                    .subscribe(
                        {
                            if (it.isNullOrBlank()) {
                                binding.rvVa.displayedChild = POS_RV
                                adapter.tokens = ArrayList(defaultAssets.filter { item ->
                                    ((currentChain != null && item.chainId == currentChain) || currentChain == null)
                                })
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

        bottomViewModel.web3TokenItems().observe(this) { items ->
            defaultAssets = items
            if (binding.searchEt.et.text.isNullOrBlank()) {
                adapter.tokens = ArrayList(defaultAssets.filter { item ->
                    ((currentChain != null && item.chainId == currentChain) || currentChain == null)
                })
            }
            if (defaultAssets.isEmpty()) {
                binding.rvVa.displayedChild = POS_EMPTY_SEND
            } else {
                binding.rvVa.displayedChild = POS_RV
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
                        id = composeId
                        setContent {
                            RecentTokens(true, key) { tokenItem ->
                                requireContext().defaultSharedPreferences.addToList(key, tokenItem.assetId)
                                this@Web3TokenListBottomSheetDialogFragment.lifecycleScope.launch {
                                    var web3Token = defaultAssets.find { it.assetId == tokenItem.assetId }
                                    if (web3Token == null) {
                                        web3Token = bottomViewModel.findOrSyncAsset(tokenItem.assetId)?.let { tokenItem ->
                                            Web3TokenItem(
                                                walletId = "",
                                                assetId = tokenItem.assetId,
                                                chainId = tokenItem.chainId,
                                                name = tokenItem.name,
                                                assetKey = tokenItem.assetKey ?: "",
                                                symbol = tokenItem.symbol,
                                                iconUrl = tokenItem.iconUrl,
                                                precision = 0,
                                                kernelAssetId = "",
                                                balance = "0",
                                                priceUsd = tokenItem.priceUsd,
                                                changeUsd = tokenItem.changeUsd,
                                                chainIcon = tokenItem.chainIconUrl,
                                                chainName = tokenItem.chainName,
                                                chainSymbol = tokenItem.chainSymbol,
                                                hidden = false
                                            )
                                        }
                                    }
                                    web3Token?.let {
                                        if (asyncOnAsset != null) {
                                            asyncClick(it)
                                        } else {
                                            this@Web3TokenListBottomSheetDialogFragment.onAsset?.invoke(
                                                it
                                            )
                                            dismiss()
                                        }
                                    }
                                }
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
                it.name.containsIgnoreCase(s) || 
                it.symbol.containsIgnoreCase(s) || 
                (it.chainName?.containsIgnoreCase(s) ?: false) ||
                it.getChainDisplayName().containsIgnoreCase(s)
            }.sortedByDescending { 
                it.name.equalsIgnoreCase(s) || it.symbol.equalsIgnoreCase(s) 
            }.filter { item ->
                ((currentChain != null && item.chainId == currentChain) || currentChain == null)
            }
        adapter.tokens = ArrayList(assetList)
        if (adapter.itemCount == 0) {
            binding.rvVa.displayedChild = POS_EMPTY_RECEIVE
        } else {
            binding.rvVa.displayedChild = POS_RV
        }
        binding.assetRv.scrollToPosition(0)
    }

    private fun search(query: String) {
        currentSearch?.cancel()
        currentSearch =
            lifecycleScope.launch {
                if (!isAdded) return@launch

                binding.rvVa.displayedChild = POS_RV
                binding.pb.isVisible = true

                val remoteAssets = if (type == TYPE_FROM_RECEIVE) {
                    val fuzzyResults = bottomViewModel.queryAsset(query, true)
                    fuzzyResults.filter {
                        it.chainId in listOf(Constants.ChainId.SOLANA_CHAIN_ID, Constants.ChainId.Base,
                            Constants.ChainId.BinanceSmartChain, Constants.ChainId.ETHEREUM_CHAIN_ID,
                            Constants.ChainId.Polygon)
                    }.map { item ->
                        Web3TokenItem(
                            walletId = "",
                            assetId = item.assetId,
                            chainId = item.chainId,
                            name = item.name,
                            assetKey = item.assetKey ?: "",
                            symbol = item.symbol,
                            iconUrl = item.iconUrl,
                            precision = 9,
                            kernelAssetId = "",
                            balance = item.balance,
                            priceUsd = item.priceUsd,
                            changeUsd = item.changeUsd,
                            chainIcon = item.chainIconUrl,
                            chainName = item.chainName,
                            chainSymbol = item.chainSymbol,
                            hidden = item.hidden
                        )
                    }
                } else {
                    emptyList()
                }

                val result = remoteAssets ?: defaultAssets.filter {
                    it.name.containsIgnoreCase(query) ||
                    it.symbol.containsIgnoreCase(query) ||
                    (it.chainName?.containsIgnoreCase(query) == true) ||
                    it.getChainDisplayName().containsIgnoreCase(query)
                }.filter { item ->
                    ((currentChain != null && item.chainId == currentChain) || currentChain == null)
                }

                adapter.tokens = ArrayList(result)
                binding.pb.isVisible = false

                if (result.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY_RECEIVE
                } else {
                    binding.rvVa.displayedChild = POS_RV
                    binding.assetRv.scrollToPosition(0)
                }
            }
    }

    fun setOnAssetClick(callback: (Web3TokenItem) -> Unit): Web3TokenListBottomSheetDialogFragment {
        this.onAsset = callback
        return this
    }

    fun setType(type: Int): Web3TokenListBottomSheetDialogFragment {
        this.type = type
        return this
    }

    fun setOnClickListener(callback: (Web3TokenItem) -> Unit): Web3TokenListBottomSheetDialogFragment {
        return setOnAssetClick(callback)
    }

    private fun asyncClick(token: Web3TokenItem) {
        lifecycleScope.launch {
            val dialog =
                indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                    setCancelable(false)
                }
            asyncOnAsset?.invoke(token)
            dialog.dismiss()
            dismiss()
        }
    }

    private var onAsset: ((Web3TokenItem) -> Unit)? = null
    var asyncOnAsset: (suspend (Web3TokenItem) -> Unit)? = null
}
