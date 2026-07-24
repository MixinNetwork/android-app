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
private const val CATEGORY_FAVORITE = "favorite"
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

        private var favoriteSpotMarkets: List<MarketItem> = emptyList()
        private var favoritePerpetualMarkets: List<PerpsMarket> = emptyList()
        private var allMarkets: List<MarketItem> = emptyList()
        private var trendingMarkets: List<MarketItem> = emptyList()
        private var topGainerMarkets: List<MarketItem> = emptyList()
        private var topLoserMarkets: List<MarketItem> = emptyList()
        private var stockMarkets: List<MarketItem> = emptyList()
        private var perpetualMarkets: List<PerpsMarket> = emptyList()
        private var marketRefreshJob: Job? = null
        private var perpetualRefreshJob: Job? = null

        init {
            observeSpotFavorites()
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
            when (entry) {
                is MarketListEntry.Spot ->
                    viewModelScope.launch(Dispatchers.IO) {
                        val updated =
                            tokenRepository.updateMarketFavored(
                                entry.market.symbol,
                                entry.favoriteId,
                                entry.isFavored,
                            )
                        if (updated && entry.isFavored && tokenRepository.hasAlertsByCoinId(entry.favoriteId)) {
                            _uiState.value = _uiState.value.copy(pendingAlertCoinId = entry.favoriteId)
                        }
                    }

                is MarketListEntry.Perpetual ->
                    viewModelScope.launch {
                        runCatching {
                            val response =
                                withContext(Dispatchers.IO) {
                                    if (entry.isFavored) {
                                        routeService.unfavoritePerpsMarket(entry.favoriteId)
                                    } else {
                                        routeService.favoritePerpsMarket(entry.favoriteId)
                                    }
                                }
                            if (response.isSuccess) {
                                favoritePerpetualMarkets =
                                    if (entry.isFavored) {
                                        favoritePerpetualMarkets.filterNot { it.marketId == entry.favoriteId }
                                    } else {
                                        (favoritePerpetualMarkets + entry.market).distinctBy { it.marketId }
                                    }
                                rebuildEntries()
                            }
                        }.onFailure { Timber.e(it, "Failed to update perpetual market favorite") }
                    }
            }
        }

        fun toggleRecommendation(marketId: String) {
            val selectedIds = _uiState.value.selectedRecommendationIds
            _uiState.value =
                _uiState.value.copy(
                    selectedRecommendationIds =
                        if (marketId in selectedIds) {
                            selectedIds - marketId
                        } else {
                            selectedIds + marketId
                        },
                )
        }

        fun addSelectedRecommendations() {
            val selectedIds = _uiState.value.selectedRecommendationIds
            if (selectedIds.isEmpty() || _uiState.value.isAddingRecommendations) return
            val selectedMarkets =
                _uiState.value.perpetualRecommendations
                    .map(MarketListEntry.Perpetual::market)
                    .filter { it.marketId in selectedIds }
            if (selectedMarkets.isEmpty()) return

            _uiState.value = _uiState.value.copy(isAddingRecommendations = true)
            viewModelScope.launch {
                val addedMarkets =
                    withContext(Dispatchers.IO) {
                        selectedMarkets.filter { market ->
                            runCatching {
                                routeService.favoritePerpsMarket(market.marketId).isSuccess
                            }.onFailure {
                                Timber.e(it, "Failed to add perpetual market recommendation")
                            }.getOrDefault(false)
                        }
                    }
                favoritePerpetualMarkets =
                    (favoritePerpetualMarkets + addedMarkets).distinctBy(PerpsMarket::marketId)
                _uiState.value =
                    _uiState.value.copy(
                        selectedRecommendationIds = selectedIds - addedMarkets.map(PerpsMarket::marketId).toSet(),
                        isAddingRecommendations = false,
                    )
                rebuildEntries()
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

        private fun observeSpotFavorites() {
            viewModelScope.launch {
                tokenRepository.observeFavoredMarkets().collect { markets ->
                    favoriteSpotMarkets = markets
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
                val (marketsResponse, favoritesResponse) =
                    coroutineScope {
                        val markets = async(Dispatchers.IO) { routeService.getPerpsMarkets() }
                        val favorites = async(Dispatchers.IO) { routeService.getPerpsMarkets(CATEGORY_FAVORITE) }
                        markets.await() to favorites.await()
                    }
                if (favoritesResponse.isSuccess) {
                    favoritePerpetualMarkets = favoritesResponse.data.orEmpty().map(PerpsMarket::withDefaults)
                }
                val markets = marketsResponse.data
                if (marketsResponse.isSuccess && markets != null) {
                    withContext(Dispatchers.IO) {
                        perpsMarketDao.upsertList(markets.map(PerpsMarket::withDefaults))
                    }
                }
                rebuildEntries()
            }.onFailure { Timber.e(it, "Failed to refresh market page perpetual markets") }
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
                        MarketPageMapper.perpetualMarkets(
                            markets = perpetualMarkets,
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
                    perpetualRecommendations =
                        perpetualMarkets
                            .filterNot { it.marketId in favoritePerpetualMarketIds }
                            .take(8)
                            .map { MarketListEntry.Perpetual(it, false) },
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
