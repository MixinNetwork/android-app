package one.mixin.android.ui.home.web3.market

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.event.ALL_MARKET_PAGE_DATA_SOURCES
import one.mixin.android.event.MarketPageDataSource
import one.mixin.android.event.MarketPageRefreshEvent
import one.mixin.android.event.QuoteColorEvent
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshMarketPageJob
import one.mixin.android.repository.PerpsMarketRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.market.GlobalMarket
import one.mixin.android.vo.market.MarketCategory
import one.mixin.android.vo.market.MarketItem
import timber.log.Timber
import javax.inject.Inject

private const val MARKET_REFRESH_INTERVAL_MS = 30_000L
private const val REFRESH_REQUEUE_DELAY_MS = 250L
private const val PREF_MARKET_PAGE_TOP_TAB = "pref_market_page_top_tab"
private const val PREF_MARKET_PAGE_SUB_TAB_PREFIX = "pref_market_page_sub_tab_"

@HiltViewModel
class MarketPageViewModel
    @Inject
    constructor(
        private val tokenRepository: TokenRepository,
        private val perpsMarketRepository: PerpsMarketRepository,
        private val jobManager: MixinJobManager,
        private val preferences: SharedPreferences,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(initialState())
        val uiState: StateFlow<MarketPageUiState> = _uiState.asStateFlow()

        private var favoriteSpotMarkets: List<MarketItem> = emptyList()
        private var favoritePerpetualMarkets: List<PerpsMarket> = emptyList()
        private var allMarkets: List<MarketItem> = emptyList()
        private var trendingMarkets: List<MarketItem> = emptyList()
        private var topGainerMarkets: List<MarketItem> = emptyList()
        private var topLoserMarkets: List<MarketItem> = emptyList()
        private var stockMarkets: List<MarketItem> = emptyList()
        private var featuredSpotMarkets: List<MarketItem> = emptyList()
        private var perpetualMarkets: List<PerpsMarket> = emptyList()
        private var featuredPerpetualMarkets: List<PerpsMarket> = emptyList()
        private var refreshLoopJob: Job? = null
        private var failedSources: Set<MarketPageDataSource> = emptySet()
        private var hasCompletedInitialRefresh = false
        private var hasLoadedSpotMarkets = false
        private var hasLoadedPerpetualMarkets = false

        init {
            observeSpotMarkets()
            observePerpetualMarkets()
            loadIndicator()
        }

        fun selectTopTab(tab: MarketTopTab) {
            if (_uiState.value.selectedTopTab == tab) return
            AnalyticsTracker.trackMarketsTabSwitch(
                level = AnalyticsTracker.MarketsTabLevel.PRIMARY,
                tab = tab.analyticsValue(),
            )
            preferences.edit().putString(PREF_MARKET_PAGE_TOP_TAB, tab.name).apply()
            val selectedSubTab = _uiState.value.selectedSubTabs[tab]
            _uiState.value =
                _uiState.value.copy(
                    selectedTopTab = tab,
                    sortState = defaultMarketSortState(tab, selectedSubTab),
                )
            rebuildEntries()
            refreshSelectedPage()
        }

        fun selectSubTab(subTab: MarketSubTab) {
            val topTab = _uiState.value.selectedTopTab
            if (topTab == MarketTopTab.INDICATOR || _uiState.value.selectedSubTab == subTab) return
            AnalyticsTracker.trackMarketsTabSwitch(
                level = AnalyticsTracker.MarketsTabLevel.SECONDARY,
                tab = subTab.analyticsValue(),
            )
            val selectedSubTabs = _uiState.value.selectedSubTabs + (topTab to subTab)
            preferences.edit()
                .putString("$PREF_MARKET_PAGE_SUB_TAB_PREFIX${topTab.name}", subTab.name)
                .apply()
            _uiState.value =
                _uiState.value.copy(
                    selectedSubTabs = selectedSubTabs,
                    sortState = defaultMarketSortState(topTab, subTab),
                )
            rebuildEntries()
            refreshSelectedPage()
        }

        fun updateSort(column: MarketSortColumn) {
            val state = _uiState.value
            val sortState = state.sortState.next(column)
            _uiState.value = state.copy(sortState = sortState)
            rebuildEntries()
            sortState.direction.analyticsValue()?.let { direction ->
                AnalyticsTracker.trackMarketsListSort(
                    sortDirection = direction,
                    primaryTab = state.selectedTopTab.analyticsValue(),
                    secondaryTab = state.selectedSubTab.analyticsValue(),
                    sortField = state.analyticsSortField(column),
                )
            }
        }

        fun applyDisplaySettings(settings: MarketDisplaySettings) {
            val state = _uiState.value
            val oldSettings = state.displaySettings
            if (oldSettings.priceChangePeriod != settings.priceChangePeriod) {
                AnalyticsTracker.trackMarketsPriceChangePeriodSwitch(
                    primaryTab = state.selectedTopTab.analyticsValue(),
                    secondaryTab = state.selectedSubTab.analyticsValue(),
                    period = settings.priceChangePeriod.analyticsValue(),
                )
            }
            if (oldSettings.quoteColorReversed != settings.quoteColorReversed) {
                AnalyticsTracker.trackMarketsQuoteColorSwitch(
                    primaryTab = state.selectedTopTab.analyticsValue(),
                    secondaryTab = state.selectedSubTab.analyticsValue(),
                    colorScheme =
                        if (settings.quoteColorReversed) {
                            AnalyticsTracker.MarketsColorScheme.RED_UP_GREEN_DOWN
                        } else {
                            AnalyticsTracker.MarketsColorScheme.GREEN_UP_RED_DOWN
                        },
                )
            }
            preferences.edit()
                .putBoolean(Constants.Account.PREF_QUOTE_COLOR, settings.quoteColorReversed)
                .putInt(
                    Constants.Account.PREF_MARKET_TOP_PERCENTAGE,
                    if (settings.priceChangePeriod == MarketPriceChangePeriod.SEVEN_DAYS) 0 else 1,
                )
                .apply()
            _uiState.value = _uiState.value.copy(displaySettings = settings)
            rebuildEntries()
            if (oldSettings.quoteColorReversed != settings.quoteColorReversed) {
                RxBus.publish(QuoteColorEvent())
            }
            if (oldSettings.priceChangePeriod != settings.priceChangePeriod) {
                refreshNow()
            }
        }

        fun toggleFavorite(entry: MarketListEntry, onResult: (Boolean) -> Unit = {}) {
            when (entry) {
                is MarketListEntry.Spot ->
                    viewModelScope.launch {
                        val success =
                            try {
                                withContext(Dispatchers.IO) {
                                    tokenRepository.updateMarketFavored(
                                        entry.market.symbol,
                                        entry.favoriteId,
                                        entry.isFavored,
                                    )
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                false
                            }
                        if (success) {
                            AnalyticsTracker.trackMarketWatchlist(
                                adding = !entry.isFavored,
                                type = AnalyticsTracker.MarketType.SPOT,
                                source = AnalyticsTracker.MarketWatchlistSource.MARKETS,
                            )
                        }
                        onResult(success)
                    }

                is MarketListEntry.Perpetual ->
                    viewModelScope.launch {
                        val success =
                            try {
                                perpsMarketRepository.updateFavorite(
                                    marketId = entry.favoriteId,
                                    isFavored = entry.isFavored,
                                )
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                false
                            }
                        if (success) {
                            AnalyticsTracker.trackMarketWatchlist(
                                adding = !entry.isFavored,
                                type = AnalyticsTracker.MarketType.PERPS,
                                source = AnalyticsTracker.MarketWatchlistSource.MARKETS,
                            )
                            toast(
                                MixinApplication.appContext.getString(
                                    if (entry.isFavored) {
                                        R.string.watchlist_remove_desc
                                    } else {
                                        R.string.watchlist_add_desc
                                    },
                                    entry.market.tokenSymbol,
                                ),
                            )
                        }
                        onResult(success)
                    }
            }
        }

        fun addRecommendations(entries: List<MarketListEntry>) {
            if (entries.isEmpty() || _uiState.value.isAddingRecommendations) return
            _uiState.value = _uiState.value.copy(isAddingRecommendations = true)
            viewModelScope.launch {
                try {
                    val addedMarketIds =
                        withContext(Dispatchers.IO) {
                            val spotMarketIds =
                                entries
                                    .filterIsInstance<MarketListEntry.Spot>()
                                    .mapTo(mutableSetOf()) { it.favoriteId }
                            val perpetualMarketIds =
                                entries
                                    .filterIsInstance<MarketListEntry.Perpetual>()
                                    .mapTo(mutableSetOf()) { it.favoriteId }
                            tokenRepository.addFavoriteMarkets(spotMarketIds) +
                                perpsMarketRepository.addFavoriteMarkets(perpetualMarketIds)
                        }
                    if (addedMarketIds.isNotEmpty()) {
                        entries
                            .filter { it.favoriteId in addedMarketIds }
                            .mapTo(mutableSetOf()) { entry ->
                                when (entry) {
                                    is MarketListEntry.Spot -> AnalyticsTracker.MarketType.SPOT
                                    is MarketListEntry.Perpetual -> AnalyticsTracker.MarketType.PERPS
                                }
                            }.forEach { type ->
                                AnalyticsTracker.trackMarketWatchlist(
                                    adding = true,
                                    type = type,
                                    source = AnalyticsTracker.MarketWatchlistSource.MARKETS,
                                )
                            }
                        val symbols =
                            entries
                                .filter { it.favoriteId in addedMarketIds }
                                .joinToString(", ") { entry ->
                                    when (entry) {
                                        is MarketListEntry.Spot -> entry.market.symbol
                                        is MarketListEntry.Perpetual -> entry.market.tokenSymbol
                                    }
                                }
                        toast(MixinApplication.appContext.getString(R.string.watchlist_add_desc, symbols))
                    }
                } finally {
                    _uiState.value = _uiState.value.copy(isAddingRecommendations = false)
                }
            }
        }

        fun startRefresh() {
            if (refreshLoopJob?.isActive == true) return
            refreshLoopJob =
                viewModelScope.launch {
                    while (isActive) {
                        refreshNow()
                        delay(MARKET_REFRESH_INTERVAL_MS)
                    }
                }
        }

        fun stopRefresh() {
            refreshLoopJob?.cancel()
            refreshLoopJob = null
        }

        fun refreshNow() {
            val sources =
                if (hasCompletedInitialRefresh) {
                    currentPageRefreshSources()
                } else {
                    ALL_MARKET_PAGE_DATA_SOURCES
                }
            if (!hasCompletedInitialRefresh) {
                _uiState.value = _uiState.value.copy(isLoading = true, hasError = false)
            }
            enqueueRefresh(sources)
        }

        fun onRefreshCompleted(event: MarketPageRefreshEvent) {
            if (event.duration != selectedDuration()) {
                viewModelScope.launch {
                    delay(REFRESH_REQUEUE_DELAY_MS)
                    refreshNow()
                }
                return
            }
            failedSources = (failedSources - event.refreshedSources) + event.failedSources
            if (event.isFullRefresh) {
                hasCompletedInitialRefresh = true
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            rebuildEntries()
        }

        private fun refreshSelectedPage() {
            if (!hasCompletedInitialRefresh) return
            enqueueRefresh(currentPageRefreshSources())
        }

        private fun enqueueRefresh(sources: Set<MarketPageDataSource>) {
            jobManager.addJobInBackground(
                RefreshMarketPageJob(
                    duration = selectedDuration(),
                    sources = sources,
                ),
            )
        }

        private fun currentPageRefreshSources(): Set<MarketPageDataSource> {
            val state = _uiState.value
            return marketPageRefreshSources(state.selectedTopTab, state.selectedSubTab)
        }

        private fun selectedDuration(): String =
            if (_uiState.value.displaySettings.priceChangePeriod == MarketPriceChangePeriod.SEVEN_DAYS) {
                "7d"
            } else {
                "24h"
            }

        fun reloadQuoteColor() {
            val quoteColorReversed =
                preferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
            if (_uiState.value.displaySettings.quoteColorReversed == quoteColorReversed) return
            _uiState.value =
                _uiState.value.copy(
                    displaySettings =
                        _uiState.value.displaySettings.copy(
                            quoteColorReversed = quoteColorReversed,
                        ),
                )
        }

        fun loadIndicator() {
            val indicator =
                runCatching {
                    preferences.getString(Constants.Account.PREF_GLOBAL_MARKET, null)
                        ?.let { GsonHelper.customGson.fromJson(it, GlobalMarket::class.java) }
                }.onFailure { Timber.e(it) }.getOrNull()
            _uiState.value = _uiState.value.copy(indicator = indicator)
        }

        private fun observeSpotMarkets() {
            viewModelScope.launch {
                combine(
                    tokenRepository.observeAllMarkets(),
                    tokenRepository.observeFavoredMarkets(),
                    tokenRepository.observeMarketsByCategory(MarketCategory.TRENDING),
                    tokenRepository.observeMarketsByCategory(MarketCategory.TOP_GAINER),
                    tokenRepository.observeMarketsByCategory(MarketCategory.TOP_LOSER),
                    tokenRepository.observeMarketsByCategory(MarketCategory.STOCK),
                    tokenRepository.observeMarketsByCategory(MarketCategory.FEATURED),
                ) { values ->
                    allMarkets = values[0]
                    favoriteSpotMarkets = values[1]
                    trendingMarkets = values[2]
                    topGainerMarkets = values[3]
                    topLoserMarkets = values[4]
                    stockMarkets = values[5]
                    featuredSpotMarkets = values[6]
                    hasLoadedSpotMarkets = true
                    rebuildEntries()
                }.collect {}
            }
        }

        private fun observePerpetualMarkets() {
            viewModelScope.launch {
                combine(
                    perpsMarketRepository.observeAllMarkets(),
                    perpsMarketRepository.observeFavoriteMarkets(),
                    perpsMarketRepository.observeMarketsByCategory(MarketCategory.FEATURED),
                ) { all, favorites, featured ->
                    perpetualMarkets = all
                    favoritePerpetualMarkets = favorites
                    featuredPerpetualMarkets = featured
                    hasLoadedPerpetualMarkets = true
                    rebuildEntries()
                }.collect {}
            }
        }

        private fun rebuildEntries() {
            val state = _uiState.value
            val favoriteCoinIds = favoriteSpotMarkets.mapTo(mutableSetOf()) { it.coinId }
            val favoritePerpetualMarketIds = favoritePerpetualMarkets.mapTo(mutableSetOf()) { it.marketId }
            fun List<MarketItem>.withFavoriteState() =
                map { market -> market.copy(isFavored = market.coinId in favoriteCoinIds) }

            val stockCoinIds = stockMarkets.mapTo(mutableSetOf()) { it.coinId }
            val favoriteCryptoMarkets = favoriteSpotMarkets.filterNot { it.coinId in stockCoinIds }
            val entries =
                when (state.selectedTopTab) {
                    MarketTopTab.WATCHLIST ->
                        MarketPageMapper.watchlist(
                            spotFavorites = favoriteSpotMarkets,
                            perpetualFavorites = favoritePerpetualMarkets,
                            stockCoinIds = stockCoinIds,
                            subTab = state.selectedSubTab ?: MarketSubTab.CRYPTO,
                            spotFeatured = featuredSpotMarkets,
                            perpetualFeatured = featuredPerpetualMarkets,
                        )

                    MarketTopTab.CRYPTO -> {
                        val subTab = state.selectedSubTab ?: MarketSubTab.TRENDING
                        val source =
                            when (subTab) {
                                MarketSubTab.FAVORITE ->
                                    favoriteCryptoMarkets.ifEmpty {
                                        featuredSpotMarkets.filterNot { it.coinId in stockCoinIds }
                                    }
                                MarketSubTab.TOP_GAINERS -> topGainerMarkets
                                MarketSubTab.TOP_LOSERS -> topLoserMarkets
                                MarketSubTab.ALL -> allMarkets
                                else -> trendingMarkets
                            }
                        val fallbackMarkets =
                            when (subTab) {
                                MarketSubTab.TRENDING,
                                MarketSubTab.TOP_GAINERS,
                                MarketSubTab.TOP_LOSERS,
                                -> allMarkets
                                else -> emptyList()
                            }
                        MarketPageMapper
                            .spotMarkets(
                                markets = source.withFavoriteState(),
                                fallbackMarkets = fallbackMarkets.withFavoriteState(),
                                subTab = subTab,
                                period = state.effectivePriceChangePeriod,
                            )
                            .filterNot { it.coinId in stockCoinIds }
                            .map { MarketListEntry.Spot(it, SpotMarketType.CRYPTO) }
                    }

                    MarketTopTab.PERPETUAL -> {
                        val source =
                            if (state.selectedSubTab == MarketSubTab.FAVORITE) {
                                favoritePerpetualMarkets.ifEmpty {
                                    featuredPerpetualMarkets
                                }
                            } else {
                                perpetualMarkets
                            }
                        MarketPageMapper.perpetualMarkets(
                            markets = source,
                            subTab = state.selectedSubTab ?: MarketSubTab.TRENDING,
                        ).map { market ->
                            MarketListEntry.Perpetual(
                                market = market,
                                isFavored = market.marketId in favoritePerpetualMarketIds,
                            )
                        }
                    }

                    MarketTopTab.INDICATOR -> emptyList()
                }
            val isShowingRecommendations =
                entries.isNotEmpty() &&
                    when {
                        state.selectedTopTab == MarketTopTab.WATCHLIST ->
                            when (state.selectedSubTab) {
                                MarketSubTab.PERPETUAL -> favoritePerpetualMarkets.isEmpty()
                                else -> favoriteSpotMarkets.isEmpty()
                            }

                        state.selectedTopTab == MarketTopTab.CRYPTO &&
                            state.selectedSubTab == MarketSubTab.FAVORITE ->
                            favoriteCryptoMarkets.isEmpty()

                        state.selectedTopTab == MarketTopTab.PERPETUAL &&
                            state.selectedSubTab == MarketSubTab.FAVORITE ->
                            favoritePerpetualMarkets.isEmpty()

                        else -> false
                    }

            _uiState.value =
                state.copy(
                    entries =
                        MarketPageMapper.applySort(
                            entries = entries,
                            sortState = state.sortState,
                            period = state.effectivePriceChangePeriod,
                            useMarketCapForSpot = state.showsMarketCapColumn,
                        ),
                    hasError = entries.isEmpty() && selectedDataSource(state) in failedSources,
                    hasLoadedLocalData = hasLoadedSelectedLocalData(state),
                    isShowingRecommendations = isShowingRecommendations,
                )
        }

        private fun hasLoadedSelectedLocalData(state: MarketPageUiState): Boolean =
            when (state.selectedTopTab) {
                MarketTopTab.WATCHLIST ->
                    if (state.selectedSubTab == MarketSubTab.PERPETUAL) {
                        hasLoadedPerpetualMarkets
                    } else {
                        hasLoadedSpotMarkets
                    }
                MarketTopTab.CRYPTO -> hasLoadedSpotMarkets
                MarketTopTab.PERPETUAL -> hasLoadedPerpetualMarkets
                MarketTopTab.INDICATOR -> true
            }

        private fun selectedDataSource(state: MarketPageUiState): MarketPageDataSource =
            when (state.selectedTopTab) {
                MarketTopTab.WATCHLIST ->
                    when (state.selectedSubTab) {
                        MarketSubTab.PERPETUAL ->
                            if (favoritePerpetualMarkets.isEmpty()) {
                                MarketPageDataSource.PERPETUAL_FEATURED
                            } else {
                                MarketPageDataSource.PERPETUAL_FAVORITE
                            }

                        else -> {
                            if (favoriteSpotMarkets.isEmpty()) {
                                MarketPageDataSource.SPOT_FEATURED
                            } else {
                                MarketPageDataSource.SPOT_FAVORITE
                            }
                        }
                    }

                MarketTopTab.CRYPTO ->
                    when (state.selectedSubTab) {
                        MarketSubTab.FAVORITE -> {
                            val stockCoinIds = stockMarkets.mapTo(mutableSetOf()) { it.coinId }
                            if (favoriteSpotMarkets.none { it.coinId !in stockCoinIds }) {
                                MarketPageDataSource.SPOT_FEATURED
                            } else {
                                MarketPageDataSource.SPOT_FAVORITE
                            }
                        }
                        MarketSubTab.TOP_GAINERS -> MarketPageDataSource.SPOT_TOP_GAINER
                        MarketSubTab.TOP_LOSERS -> MarketPageDataSource.SPOT_TOP_LOSER
                        MarketSubTab.ALL -> MarketPageDataSource.SPOT_ALL
                        else -> MarketPageDataSource.SPOT_TRENDING
                    }

                MarketTopTab.PERPETUAL ->
                    when (state.selectedSubTab) {
                        MarketSubTab.FAVORITE ->
                            if (favoritePerpetualMarkets.isEmpty()) {
                                MarketPageDataSource.PERPETUAL_FEATURED
                            } else {
                                MarketPageDataSource.PERPETUAL_FAVORITE
                            }
                        else -> MarketPageDataSource.PERPETUAL_ALL
                    }

                MarketTopTab.INDICATOR -> MarketPageDataSource.GLOBAL
            }

        private fun initialState(): MarketPageUiState {
            val topTab =
                preferences.getString(PREF_MARKET_PAGE_TOP_TAB, null)
                    ?.let { stored -> MarketTopTab.entries.firstOrNull { it.name == stored } }
                    ?: MarketTopTab.CRYPTO
            val subTabs =
                defaultMarketSubTabs().mapValues { (tab, default) ->
                    preferences.getString("$PREF_MARKET_PAGE_SUB_TAB_PREFIX${tab.name}", null)
                        ?.let { stored -> MarketSubTab.entries.firstOrNull { it.name == stored } }
                        ?.takeIf { it in marketSubTabs(tab) }
                        ?: default
                }
            val priceChangePeriod =
                if (preferences.getInt(Constants.Account.PREF_MARKET_TOP_PERCENTAGE, 0) == 0) {
                    MarketPriceChangePeriod.SEVEN_DAYS
                } else {
                    MarketPriceChangePeriod.TWENTY_FOUR_HOURS
                }
            return MarketPageUiState(
                selectedTopTab = topTab,
                selectedSubTabs = subTabs,
                sortState = defaultMarketSortState(topTab, subTabs[topTab]),
                displaySettings =
                    MarketDisplaySettings(
                        quoteColorReversed =
                            preferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false),
                        priceChangePeriod = priceChangePeriod,
                    ),
            )
        }

        override fun onCleared() {
            stopRefresh()
            super.onCleared()
        }
    }
