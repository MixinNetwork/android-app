package one.mixin.android.ui.home.web3.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.isNightMode
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.trade.perps.PerpsActivity
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.WalletActivity.Destination
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType
import one.mixin.android.vo.market.MarketItem

@AndroidEntryPoint
class MarketSearchFragment : BaseFragment() {
    companion object {
        const val TAG = "MarketSearchFragment"
    }

    private val marketSearchViewModel by viewModels<MarketSearchViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(inflater.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MixinAppTheme(darkTheme = context.isNightMode()) {
                    val state by marketSearchViewModel.uiState.collectAsStateWithLifecycle()
                    val recentSearches by marketSearchViewModel.recentSearches.collectAsStateWithLifecycle()
                    MarketSearchPage(
                        state = state,
                        recentSearches = recentSearches,
                        onQueryChanged = marketSearchViewModel::updateQuery,
                        onCancel = ::closeSearch,
                        onSelectTab = marketSearchViewModel::selectTab,
                        onClearRecentSearches = {
                            marketSearchViewModel.removeRecentSearch(requireContext().defaultSharedPreferences)
                        },
                        onRecentSearchClick = ::openRecentSearch,
                        onSpotMarketClick = ::openSpotMarket,
                        onPerpetualMarketClick = ::openPerpetualMarket,
                    )
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        marketSearchViewModel.loadRecentSearches(requireContext().defaultSharedPreferences)
    }

    private fun closeSearch() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    private fun openRecentSearch(search: RecentSearch) {
        when (search.type) {
            RecentSearchType.MARKET -> {
                val coinId = search.primaryKey ?: return
                lifecycleScope.launch {
                    marketSearchViewModel.findSpotMarket(coinId)?.let(::openSpotMarket)
                }
            }
            RecentSearchType.PERPETUAL -> {
                val marketId = search.primaryKey ?: return
                lifecycleScope.launch {
                    marketSearchViewModel.findPerpetualMarket(marketId)?.let(::openPerpetualMarket)
                }
            }
            else -> Unit
        }
    }

    private fun openSpotMarket(market: MarketItem) {
        saveRecentSearch(
            RecentSearch(
                type = RecentSearchType.MARKET,
                iconUrl = market.iconUrl,
                title = market.symbol,
                primaryKey = market.coinId,
            ),
        )
        WalletActivity.showWithMarket(
            requireActivity(),
            market,
            Destination.Market,
            AnalyticsTracker.MarketSource.MORE_SEARCH,
        )
    }

    private fun openPerpetualMarket(market: PerpsMarket) {
        saveRecentSearch(
            RecentSearch(
                type = RecentSearchType.PERPETUAL,
                iconUrl = market.iconUrl,
                title = market.displaySymbol.ifBlank { market.tokenSymbol },
                subTitle = market.tokenSymbol,
                primaryKey = market.marketId,
            ),
        )
        PerpsActivity.showDetail(
            context = requireContext(),
            marketId = market.marketId,
            marketSymbol = market.displaySymbol,
            marketDisplaySymbol = market.displaySymbol,
            marketTokenSymbol = market.tokenSymbol,
            source = AnalyticsTracker.PerpsSource.MORE_EXPLORE,
        )
    }

    private fun saveRecentSearch(search: RecentSearch) {
        marketSearchViewModel.saveRecentSearch(
            requireContext().defaultSharedPreferences,
            search,
        )
    }
}
