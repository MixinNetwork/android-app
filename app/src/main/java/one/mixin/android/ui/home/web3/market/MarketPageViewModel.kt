package one.mixin.android.ui.home.web3.market

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import one.mixin.android.RxBus
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsMarketCategory
import one.mixin.android.event.MarketPageDataSource
import one.mixin.android.event.MarketPageRefreshEvent
import one.mixin.android.event.QuoteColorEvent
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshMarketPageJob
import one.mixin.android.repository.PerpsMarketRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.util.GsonHelper
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
        private var trendingPerpetualMarkets: List<PerpsMarket> = emptyList()
        private var featuredPerpetualMarkets: List<PerpsMarket> = emptyList()
        private var refreshLoopJob: Job? = null
        private var failedSources: Set<MarketPageDataSource> = emptySet()
        private var hasCompletedRefresh = false

        init {
            observeSpotMarkets()
            observePerpetualMarkets()
            loadIndicator()
        }

        fun selectTopTab(tab: MarketTopTab) {
            if (_uiState.value.selectedTopTab == tab) return
            preferences.edit().putString(PREF_MARKET_PAGE_TOP_TAB, tab.name).apply()
            _uiState.value =
                _uiState.value.copy(
                    selectedTopTab = tab,
                    sortState = MarketSortState(),
                )
            rebuildEntries()
        }

        fun selectSubTab(subTab: MarketSubTab) {
            val topTab = _uiState.value.selectedTopTab
            if (topTab == MarketTopTab.INDICATOR || _uiState.value.selectedSubTab == subTab) return
            val selectedSubTabs = _uiState.value.selectedSubTabs + (topTab to subTab)
            preferences.edit()
                .putString("$PREF_MARKET_PAGE_SUB_TAB_PREFIX${topTab.name}", subTab.name)
                .apply()
            _uiState.value =
                _uiState.value.copy(
                    selectedSubTabs = selectedSubTabs,
                    sortState = MarketSortState(),
                )
            rebuildEntries()
        }

        fun updateSort(column: MarketSortColumn) {
            _uiState.value = _uiState.value.copy(sortState = _uiState.value.sortState.next(column))
            rebuildEntries()
        }

        fun applyDisplaySettings(settings: MarketDisplaySettings) {
            val oldSettings = _uiState.value.displaySettings
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

        fun toggleFavorite(entry: MarketListEntry) {
            when (entry) {
                is MarketListEntry.Spot ->
                    viewModelScope.launch {
                        val shouldPromptForAlerts =
                            withContext(Dispatchers.IO) {
                                val updated =
                                    tokenRepository.updateMarketFavored(
                                        entry.market.symbol,
                                        entry.favoriteId,
                                        entry.isFavored,
                                    )
                                updated &&
                                    entry.isFavored &&
                                    tokenRepository.hasAlertsByCoinId(entry.favoriteId)
                            }
                        if (shouldPromptForAlerts) {
                            _uiState.value = _uiState.value.copy(pendingAlertCoinId = entry.favoriteId)
                        }
                    }

                is MarketListEntry.Perpetual ->
                    viewModelScope.launch {
                        perpsMarketRepository.updateFavorite(
                            marketId = entry.favoriteId,
                            isFavored = entry.isFavored,
                        )
                    }
            }
        }

        fun keepPriceAlerts() {
            _uiState.value = _uiState.value.copy(pendingAlertCoinId = null)
        }

        fun deletePriceAlerts() {
            val coinId = _uiState.value.pendingAlertCoinId ?: return
            _uiState.value = _uiState.value.copy(pendingAlertCoinId = null)
            viewModelScope.launch(Dispatchers.IO) {
                tokenRepository.deleteAlertsByCoinId(coinId)
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
            if (!hasCompletedRefresh) {
                _uiState.value = _uiState.value.copy(isLoading = true, hasError = false)
            }
            jobManager.addJobInBackground(RefreshMarketPageJob(selectedDuration()))
        }

        fun onRefreshCompleted(event: MarketPageRefreshEvent) {
            if (event.duration != selectedDuration()) {
                viewModelScope.launch {
                    delay(REFRESH_REQUEUE_DELAY_MS)
                    refreshNow()
                }
                return
            }
            failedSources = event.failedSources
            hasCompletedRefresh = true
            _uiState.value = _uiState.value.copy(isLoading = false)
            rebuildEntries()
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
                    rebuildEntries()
                }.collect {}
            }
        }

        private fun observePerpetualMarkets() {
            viewModelScope.launch {
                combine(
                    perpsMarketRepository.observeAllMarkets(),
                    perpsMarketRepository.observeFavoriteMarkets(),
                    perpsMarketRepository.observeMarketsByCategory(PerpsMarketCategory.TRENDING),
                    perpsMarketRepository.observeMarketsByCategory(PerpsMarketCategory.FEATURED),
                ) { values ->
                    perpetualMarkets = values[0]
                    favoritePerpetualMarkets = values[1]
                    trendingPerpetualMarkets = values[2]
                    featuredPerpetualMarkets = values[3]
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
                        val source =
                            when (state.selectedSubTab) {
                                MarketSubTab.TOP_GAINERS -> topGainerMarkets
                                MarketSubTab.TOP_LOSERS -> topLoserMarkets
                                MarketSubTab.ALL -> allMarkets
                                else -> trendingMarkets
                            }
                        source.withFavoriteState()
                            .filterNot { it.coinId in stockCoinIds }
                            .map { MarketListEntry.Spot(it, SpotMarketType.CRYPTO) }
                    }

                    MarketTopTab.PERPETUAL -> {
                        val source =
                            when (state.selectedSubTab) {
                                MarketSubTab.TOP_GAINERS,
                                MarketSubTab.TOP_LOSERS,
                                MarketSubTab.ALL
                                -> perpetualMarkets
                                else -> trendingPerpetualMarkets
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

                    MarketTopTab.STOCK ->
                        MarketPageMapper.stockMarkets(
                            markets = stockMarkets.withFavoriteState(),
                            subTab = state.selectedSubTab ?: MarketSubTab.TRENDING,
                            period = state.displaySettings.priceChangePeriod,
                        ).map { MarketListEntry.Spot(it, SpotMarketType.STOCK) }

                    MarketTopTab.INDICATOR -> emptyList()
                }

            _uiState.value =
                state.copy(
                    entries =
                        MarketPageMapper.applySort(
                            entries = entries,
                            sortState = state.sortState,
                            period = state.effectivePriceChangePeriod,
                        ),
                    hasError = entries.isEmpty() && selectedDataSource(state) in failedSources,
                )
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

                        MarketSubTab.STOCK -> {
                            val stockCoinIds = stockMarkets.mapTo(mutableSetOf()) { it.coinId }
                            if (favoriteSpotMarkets.none { it.coinId in stockCoinIds }) {
                                MarketPageDataSource.SPOT_FEATURED
                            } else {
                                MarketPageDataSource.SPOT_FAVORITE
                            }
                        }
                        else -> {
                            val stockCoinIds = stockMarkets.mapTo(mutableSetOf()) { it.coinId }
                            if (favoriteSpotMarkets.none { it.coinId !in stockCoinIds }) {
                                MarketPageDataSource.SPOT_FEATURED
                            } else {
                                MarketPageDataSource.SPOT_FAVORITE
                            }
                        }
                    }

                MarketTopTab.CRYPTO ->
                    when (state.selectedSubTab) {
                        MarketSubTab.TOP_GAINERS -> MarketPageDataSource.SPOT_TOP_GAINER
                        MarketSubTab.TOP_LOSERS -> MarketPageDataSource.SPOT_TOP_LOSER
                        MarketSubTab.ALL -> MarketPageDataSource.SPOT_ALL
                        else -> MarketPageDataSource.SPOT_TRENDING
                    }

                MarketTopTab.PERPETUAL ->
                    if (state.selectedSubTab == MarketSubTab.TRENDING) {
                        MarketPageDataSource.PERPETUAL_TRENDING
                    } else {
                        MarketPageDataSource.PERPETUAL_ALL
                    }

                MarketTopTab.STOCK -> MarketPageDataSource.SPOT_STOCK
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
                        ?.takeUnless { tab == MarketTopTab.WATCHLIST && it == MarketSubTab.ALL }
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
