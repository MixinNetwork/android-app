package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_WEB3_WALLET_RECENT
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.addToList
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getStringList
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.remove
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.market.MarketSearchFragment
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.safe.toWeb3TokenItem
import timber.log.Timber

@AndroidEntryPoint
class WalletSearchWeb3Fragment : BaseFragment() {
    companion object {
        const val ARGS_WALLET_ID = "args_wallet_id"
    }

    private val viewModel by viewModels<Web3ViewModel>()
    private val walletViewModel by viewModels<WalletViewModel>()
    private val walletId by lazy { requireNotNull(requireArguments().getString(ARGS_WALLET_ID)) }
    private var state by mutableStateOf(WalletSearchUiState())
    private var recentSearches by mutableStateOf<List<WalletSearchResult>>(emptyList())
    private var recentTokens: List<Web3TokenItem> = emptyList()
    private var resultTokens: List<Web3TokenItem> = emptyList()
    private var searchJob: Job? = null
    private var recentJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MixinAppTheme(darkTheme = context.isNightMode()) {
                    WalletSearchPage(
                        state = state,
                        recentSearches = recentSearches,
                        onQueryChanged = ::search,
                        onCancel = {
                            view?.hideKeyboard()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        },
                        onSelectTab = { state = state.copy(selectedTab = it) },
                        onClearRecent = {
                            recentJob?.cancel()
                            defaultSharedPreferences.remove("$PREF_WEB3_WALLET_RECENT:$walletId:search")
                            recentSearches = emptyList()
                        },
                        onResultClick = ::openResult,
                        onSearchMarkets = {
                            view?.hideKeyboard()
                            findNavController().navigate(
                                R.id.market_search_fragment,
                                bundleOf(
                                    MarketSearchFragment.ARGS_QUERY to state.query.trim(),
                                    MarketSearchFragment.ARGS_MARKET_SOURCE to AnalyticsTracker.MarketSource.WALLET_SEARCH,
                                    MarketSearchFragment.ARGS_PERPS_SOURCE to AnalyticsTracker.PerpsSource.WALLET_SEARCH,
                                ),
                                NavOptions.Builder().setPopUpTo(R.id.wallet_search_web3_fragment, true).build(),
                            )
                        },
                    )
                }
            }
        }

    override fun onResume() {
        super.onResume()
        loadRecentSearches()
    }

    private fun loadRecentSearches() {
        recentJob?.cancel()
        recentJob = viewLifecycleOwner.lifecycleScope.launch {
            val preferences = defaultSharedPreferences
            val stored = preferences.getStringList("$PREF_WEB3_WALLET_RECENT:$walletId:search")
            val tokens = viewModel.web3TokensExcludeHiddenRaw(walletId).associateBy { it.assetId }.toMutableMap()
            val missingIds = stored.orEmpty().distinct().filter { it !in tokens }
            val assets = withContext(Dispatchers.IO) { walletViewModel.findAssetsByIds(missingIds) }
            assets.forEach { asset ->
                tokens[asset.assetId] = asset.toWeb3TokenItem(walletId).copy(balance = "0", precision = asset.precision, level = asset.level ?: Constants.AssetLevel.UNKNOWN)
            }
            val ids = web3WalletSearchRecentIds(stored, tokens.keys)
            recentTokens = ids.mapNotNull { tokens[it] }
            recentSearches = recentTokens.map { WalletSearchResult.Asset(it.toTokenItem()) }
        }
    }

    private fun search(query: String) {
        searchJob?.cancel()
        state = state.copy(
            query = query,
            selectedTab = walletSearchTabForQuery(state.query, query, state.selectedTab),
            results = if (query.isBlank()) emptyList() else state.results,
            isSearching = query.isNotBlank(),
        )
        if (query.isBlank()) return
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(200)
            try {
                val text = query.trim()
                val localTokens = viewModel.web3TokensExcludeHiddenRaw(walletId).filter { token ->
                    token.name.contains(text, ignoreCase = true) ||
                        token.symbol.contains(text, ignoreCase = true) ||
                        token.chainName?.contains(text, ignoreCase = true) == true
                }
                resultTokens = if (localTokens.isNotEmpty() || text.length < 2) {
                    localTokens
                } else {
                    val remoteTokens = walletViewModel.queryAsset(walletId, text, true)
                    val hasLitecoinAddress = viewModel.getAddressesByChainId(walletId, Constants.ChainId.Litecoin) != null
                    val filteredTokens = remoteTokens.filter {
                        (it.chainId in Constants.Web3EvmChainIds || it.chainId == Constants.ChainId.SOLANA_CHAIN_ID || it.chainId in Constants.Web3UtxoChainIds) &&
                            (it.chainId != Constants.ChainId.Litecoin || hasLitecoinAddress)
                    }
                    val remoteWeb3Tokens = filteredTokens.map { tokenItem ->
                        val t = viewModel.web3TokenItemById(walletId, tokenItem.assetId)
                        val c = viewModel.web3TokenItemById(walletId, tokenItem.chainId)
                        Web3TokenItem(
                            walletId = walletId,
                            assetId = tokenItem.assetId,
                            chainId = tokenItem.chainId,
                            name = tokenItem.name,
                            assetKey = tokenItem.assetKey ?: t?.assetKey ?: "",
                            symbol = tokenItem.symbol,
                            iconUrl = tokenItem.iconUrl,
                            precision = t?.precision ?: tokenItem.precision,
                            kernelAssetId = "",
                            balance = t?.balance ?: "0",
                            priceUsd = tokenItem.priceUsd,
                            changeUsd = tokenItem.changeUsd,
                            chainIcon = tokenItem.chainIconUrl ?: t?.chainIcon ?: c?.iconUrl,
                            chainName = tokenItem.chainName ?: c?.name,
                            chainSymbol = tokenItem.chainSymbol ?: c?.chainSymbol,
                            hidden = false,
                            level = tokenItem.level ?: Constants.AssetLevel.UNKNOWN
                        )
                    }
                    remoteWeb3Tokens
                }
                val results = resultTokens.map { WalletSearchResult.Asset(it.toTokenItem()) }
                state = state.copy(
                    results = results,
                    selectedTab = selectedWalletSearchTab(state.selectedTab, walletSearchTabs(results.map { it.tab })),
                    isSearching = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Web3 wallet search failed")
                resultTokens = emptyList()
                state = state.copy(results = emptyList(), isSearching = false)
            }
        }
    }

    private fun openResult(result: WalletSearchResult) {
        val assetId = (result as WalletSearchResult.Asset).token.assetId
        val token = (if (state.query.isBlank()) recentTokens else resultTokens).first { it.assetId == assetId }
        defaultSharedPreferences.addToList("$PREF_WEB3_WALLET_RECENT:$walletId:search", token.assetId)
        view?.hideKeyboard()
        viewLifecycleOwner.lifecycleScope.launch {
            val address = viewModel.getAddressesByChainId(walletId, token.chainId) ?: return@launch
            findNavController().navigate(
                R.id.action_wallet_search_web3_to_web3_transactions,
                bundleOf("args_token" to token, "args_address" to address.destination),
            )
        }
    }
}
