package one.mixin.android.ui.home.web3.market

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.RxBus
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.withDefaults
import one.mixin.android.api.service.RouteService
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.event.QuoteColorEvent
import one.mixin.android.repository.TokenRepository
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.market.GlobalMarket
import one.mixin.android.vo.market.MarketItem
import timber.log.Timber
import javax.inject.Inject

private const val MARKET_REFRESH_INTERVAL_MS = 3_000L
private const val PREF_MARKET_PAGE_TOP_TAB = "pref_market_page_top_tab"
private const val PREF_MARKET_PAGE_SUB_TAB_PREFIX = "pref_market_page_sub_tab_"
private const val CATEGORY_ALL = "all"
private const val CATEGORY_STOCKS = "stocks"
private const val CATEGORY_TRENDING = "trending"
private const val CATEGORY_TOP_GAINERS = "top_gainers"
private const val CATEGORY_TOP_LOSERS = "top_losers"

@HiltViewModel
class MarketPageViewModel
    @Inject
    constructor(
        private val tokenRepository: TokenRepository,
        private val perpsMarketDao: PerpsMarketDao,
        private val routeService: RouteService,
        private val preferences: SharedPreferences,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(initialState())
        val uiState: StateFlow<MarketPageUiState> = _uiState.asStateFlow()

        private var favoriteMarkets: List<MarketItem> = emptyList()
        private var allMarkets: List<MarketItem> = emptyList()
        private var trendingMarkets: List<MarketItem> = emptyList()
        private var topGainerMarkets: List<MarketItem> = emptyList()
        private var topLoserMarkets: List<MarketItem> = emptyList()
        private var stockMarkets: List<MarketItem> = emptyList()
        private var perpetualMarkets: List<PerpsMarket> = emptyList()
        private var marketRefreshJob: Job? = null
        private var perpetualRefreshJob: Job? = null

        init {
            observeFavorites()
            observePerpetualMarkets()
            loadIndicator()
            refreshMarkets()
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
                refreshMarkets()
            }
        }

        fun toggleFavorite(entry: MarketListEntry) {
            val coinId = entry.favoriteCoinId ?: return
            val symbol =
                when (entry) {
                    is MarketListEntry.Spot -> entry.market.symbol
                    is MarketListEntry.Perpetual -> entry.backingMarket?.symbol ?: entry.market.tokenSymbol
                }
            viewModelScope.launch(Dispatchers.IO) {
                tokenRepository.updateMarketFavored(symbol, coinId, entry.isFavored)
            }
        }

        fun refreshAll() {
            loadIndicator()
            refreshMarkets()
            viewModelScope.launch {
                refreshPerpetualMarkets()
            }
        }

        fun refreshMarkets() {
            if (marketRefreshJob?.isActive == true) return
            marketRefreshJob =
                viewModelScope.launch {
                    if (allMarkets.isEmpty() && trendingMarkets.isEmpty()) {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    val duration =
                        if (_uiState.value.displaySettings.priceChangePeriod == MarketPriceChangePeriod.SEVEN_DAYS) {
                            "7d"
                        } else {
                            "24h"
                        }
                    val results =
                        coroutineScope {
                            val all = async { tokenRepository.fetchMarkets(CATEGORY_ALL, duration) }
                            val trending = async { tokenRepository.fetchMarkets(CATEGORY_TRENDING, duration) }
                            val gainers = async { tokenRepository.fetchMarkets(CATEGORY_TOP_GAINERS, duration) }
                            val losers = async { tokenRepository.fetchMarkets(CATEGORY_TOP_LOSERS, duration) }
                            val stocks = async { tokenRepository.fetchMarkets(CATEGORY_STOCKS, duration) }
                            MarketFetchResult(
                                all = all.await(),
                                trending = trending.await(),
                                gainers = gainers.await(),
                                losers = losers.await(),
                                stocks = stocks.await(),
                            )
                        }
                    results.all?.let { allMarkets = it }
                    results.trending?.let { trendingMarkets = it }
                    results.gainers?.let { topGainerMarkets = it }
                    results.losers?.let { topLoserMarkets = it }
                    results.stocks?.let { stockMarkets = it }
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            hasError = results.allFailed,
                        )
                    rebuildEntries()
                }
        }

        fun startPerpetualRefresh() {
            if (perpetualRefreshJob?.isActive == true) return
            perpetualRefreshJob =
                viewModelScope.launch {
                    while (isActive) {
                        refreshPerpetualMarkets()
                        delay(MARKET_REFRESH_INTERVAL_MS)
                    }
                }
        }

        fun stopPerpetualRefresh() {
            perpetualRefreshJob?.cancel()
            perpetualRefreshJob = null
        }

        fun loadIndicator() {
            val indicator =
                runCatching {
                    preferences.getString(Constants.Account.PREF_GLOBAL_MARKET, null)
                        ?.let { GsonHelper.customGson.fromJson(it, GlobalMarket::class.java) }
                }.onFailure { Timber.e(it) }.getOrNull()
            _uiState.value = _uiState.value.copy(indicator = indicator)
        }

        private fun observeFavorites() {
            viewModelScope.launch {
                tokenRepository.observeFavoredMarkets().collect { markets ->
                    favoriteMarkets = markets
                    rebuildEntries()
                }
            }
        }

        private fun observePerpetualMarkets() {
            viewModelScope.launch {
                perpsMarketDao.observeAllMarkets().collect { markets ->
                    perpetualMarkets = markets
                    rebuildEntries()
                }
            }
        }

        private suspend fun refreshPerpetualMarkets() {
            runCatching {
                val response = withContext(Dispatchers.IO) { routeService.getPerpsMarkets() }
                val markets = response.data
                if (response.isSuccess && markets != null) {
                    withContext(Dispatchers.IO) {
                        perpsMarketDao.upsertList(markets.map(PerpsMarket::withDefaults))
                    }
                }
            }.onFailure { Timber.e(it, "Failed to refresh market page perpetual markets") }
        }

        private fun rebuildEntries() {
            val state = _uiState.value
            val favoriteCoinIds = favoriteMarkets.mapTo(mutableSetOf()) { it.coinId }
            fun List<MarketItem>.withFavoriteState() =
                map { market -> market.copy(isFavored = market.coinId in favoriteCoinIds) }

            val stockCoinIds = stockMarkets.mapTo(mutableSetOf()) { it.coinId }
            val entries =
                when (state.selectedTopTab) {
                    MarketTopTab.WATCHLIST ->
                        MarketPageMapper.watchlist(
                            favorites = favoriteMarkets,
                            stockCoinIds = stockCoinIds,
                            perpetualById = perpetualMarkets.associateBy { it.marketId },
                            subTab = state.selectedSubTab ?: MarketSubTab.ALL,
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
                        val backingMarkets =
                            (allMarkets + stockMarkets)
                                .withFavoriteState()
                                .mapNotNull { market -> market.perpsMarketId?.let { it to market } }
                                .toMap()
                        MarketPageMapper.perpetualMarkets(
                            markets = perpetualMarkets,
                            subTab = state.selectedSubTab ?: MarketSubTab.TRENDING,
                            period = state.displaySettings.priceChangePeriod,
                        ).map { market ->
                            MarketListEntry.Perpetual(market, backingMarkets[market.marketId])
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
                            period = state.displaySettings.priceChangePeriod,
                        ),
                )
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
            stopPerpetualRefresh()
            super.onCleared()
        }

        private data class MarketFetchResult(
            val all: List<MarketItem>?,
            val trending: List<MarketItem>?,
            val gainers: List<MarketItem>?,
            val losers: List<MarketItem>?,
            val stocks: List<MarketItem>?,
        ) {
            val allFailed: Boolean
                get() = all == null && trending == null && gainers == null && losers == null && stocks == null
        }
    }
