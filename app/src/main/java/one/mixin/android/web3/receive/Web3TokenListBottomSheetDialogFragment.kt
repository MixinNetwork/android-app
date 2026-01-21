package one.mixin.android.web3.receive

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.doOnPreDraw
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
import one.mixin.android.Constants.Account.PREF_WALLET_SEND
import one.mixin.android.Constants.Account.PREF_WALLET_RECEIVE
import one.mixin.android.Constants.ChainId.Arbitrum
import one.mixin.android.Constants.ChainId.Avalanche
import one.mixin.android.Constants.ChainId.Base
import one.mixin.android.Constants.ChainId.BinanceSmartChain
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.Optimism
import one.mixin.android.Constants.ChainId.Polygon
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.Constants.ChainId.TON_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.addToList
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.scrollToCenterCheckedRadio
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.components.RecentTokens
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import timber.log.Timber
import java.math.BigDecimal
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

        const val ARGS_WALLET_ID = "args_wallet_id"
        fun newInstance(
            walletId: String? = null,
            type: Int = TYPE_FROM_SEND
        ): Web3TokenListBottomSheetDialogFragment {
            return Web3TokenListBottomSheetDialogFragment()
                .withArgs {
                    putString(ARGS_WALLET_ID, walletId)
                }.apply {
                    this.type = type
                }
        }
    }

    private val binding by viewBinding(FragmentAssetListBottomSheetBinding::inflate)

    private val adapter by lazy { Web3TokenAdapter() }
    private val walletId: String? by lazy { arguments?.getString(ARGS_WALLET_ID) }

    private var disposable: Disposable? = null
    private var currentSearch: Job? = null
    private var currentQuery: String = ""
    private var defaultAssets = emptyList<Web3TokenItem>()
    private var type: Int = TYPE_FROM_SEND
    private var currentChain: String? = null

    private val key by lazy {
        when (type) {
            TYPE_FROM_SEND -> PREF_WALLET_SEND
            TYPE_FROM_RECEIVE -> PREF_WALLET_RECEIVE
            else -> PREF_WALLET_SEND
        }
    }

    private fun initRadio() {
        binding.apply {
            radio.isVisible = true
            radioAll.isChecked = true
            radio.scrollToCenterCheckedRadio(radioGroup)
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
                        ETHEREUM_CHAIN_ID
                    }

                    R.id.radio_solana -> {
                        SOLANA_CHAIN_ID
                    }

                    R.id.radio_base -> {
                        Base
                    }

                    R.id.radio_bsc -> {
                        BinanceSmartChain
                    }

                    R.id.radio_polygon -> {
                        Polygon
                    }

                    R.id.radio_arbritrum -> {
                        Arbitrum
                    }

                    R.id.radio_optimism -> {
                        Optimism
                    }

                    R.id.radio_toncoin -> {
                        TON_CHAIN_ID
                    }

                    R.id.radio_avalanche -> {
                        Avalanche
                    }

                    else -> {
                        null
                    }
                }
                radio.scrollToCenterCheckedRadio(radioGroup)
                loadData()
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
        binding.ph.doOnPreDraw {
            binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
                height = binding.ph.getSafeAreaInsetsTop() + requireContext().appCompatActionBarHeight()
            }
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
                                adapter.tokens = ArrayList(defaultAssets)
                            } else {
                                Timber.e("textChanges: $it")
                                if (it.toString() != currentQuery) {
                                    currentQuery = it.toString()
                                    currentSearch?.cancel()
                                    currentSearch = filter(it.toString())
                                }
                            }
                        },
                        {},
                    )
        }

        walletId?.let { it ->
            bottomViewModel.web3TokenItemsExcludeHidden(it, PREF_WALLET_SEND == this.key).observe(this) { items ->
                defaultAssets = if (type == TYPE_FROM_SEND)
                    items.filter { t ->
                        t.balance.toBigDecimalOrNull().run {
                            this != null && this > BigDecimal.ZERO
                        }
                    } else {
                    items
                }
                if (binding.searchEt.et.text.isNullOrBlank()) {
                    adapter.tokens = ArrayList(defaultAssets)
                }
                if (defaultAssets.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY_SEND
                } else {
                    binding.rvVa.displayedChild = POS_RV
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
                        id = composeId
                        setContent {
                            RecentTokens(true, key) { tokenItem ->
                                requireContext().defaultSharedPreferences.addToList(key, tokenItem.assetId)
                                this@Web3TokenListBottomSheetDialogFragment.lifecycleScope.launch {
                                    var web3Token = defaultAssets.find { it.assetId == tokenItem.assetId }
                                    if (web3Token == null) {
                                        web3Token = bottomViewModel.findOrSyncAsset(tokenItem.assetId)?.let { tokenItem ->
                                            Web3TokenItem(
                                                walletId = walletId ?: "",
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
                                                hidden = false,
                                                level = Constants.AssetLevel.VERIFIED
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

    private fun filter(s: String) =
        lifecycleScope.launch {
            if (s.isBlank() && currentChain == null) {
                adapter.tokens = ArrayList(defaultAssets)
                if (defaultAssets.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY_RECEIVE
                } else {
                    binding.rvVa.displayedChild = POS_RV
                }
                return@launch
            }
            val assetList =
                defaultAssets.toMutableList()

            val total = search(s, assetList)
            adapter.tokens = ArrayList(total.filter {
                it.name.containsIgnoreCase(s) || 
                it.symbol.containsIgnoreCase(s) || 
                (it.chainName?.containsIgnoreCase(s) ?: false) ||
                it.getChainDisplayName().containsIgnoreCase(s)
            }.sortedByDescending { 
                it.name.equalsIgnoreCase(s) || it.symbol.equalsIgnoreCase(s) 
            })
            if (!isAdded) {
                return@launch
            }
            loadData()
        }

    private fun loadData() {
        adapter.chain = currentChain
        if (adapter.itemCount == 0) {
            binding.rvVa.displayedChild = POS_EMPTY_RECEIVE
        } else {
            binding.rvVa.displayedChild = POS_RV
        }
        binding.assetRv.scrollToPosition(0)
        binding.pb.isVisible = false
    }

    private suspend fun search(
        query: String,
        localTokens: MutableList<Web3TokenItem>
    ): List<Web3TokenItem> {
        if (query.isBlank() || TYPE_FROM_SEND == type) return localTokens
        binding.pb.isVisible = true
        val fuzzyResults = bottomViewModel.queryAsset(walletId = walletId, query = query, web3 = true)
        val remoteAssets = fuzzyResults.filter {
            it.chainId in listOf(
                SOLANA_CHAIN_ID,
                ETHEREUM_CHAIN_ID,
                Base,
                Optimism,
                Arbitrum,
                Avalanche,
                BinanceSmartChain,
                Polygon,
            )
        }.map { item ->
            bottomViewModel.web3TokenItemById(walletId ?: "", item.assetId).let { local ->
                if (local != null && (local.level >= 10 || local.hidden == false)) {
                    local
                } else {
                    Web3TokenItem(
                        walletId = walletId ?: "",
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
                        hidden = item.hidden,
                        level = Constants.AssetLevel.VERIFIED
                    )
                }
            }
        }

        binding.pb.isVisible = false

        return localTokens.plus(
            remoteAssets.filterNot { r ->
                localTokens.any { l ->
                    l.chainId == r.chainId && l.assetId == r.assetId
                }
            }).sortedByDescending {
            (it.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(it.priceUsd.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        }
    }

    fun setOnAssetClick(callback: (Web3TokenItem) -> Unit): Web3TokenListBottomSheetDialogFragment {
        this.onAsset = callback
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