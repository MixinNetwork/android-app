package one.mixin.android.ui.home.web3.market

import android.content.SharedPreferences
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.Constants.Account.PREF_MARKET_RECENT_SEARCH
import one.mixin.android.Constants.Account.PREF_RECENT_SEARCH
import one.mixin.android.extension.escapeSql
import one.mixin.android.extension.mergeLocalAndRefreshed
import one.mixin.android.extension.putString
import one.mixin.android.extension.remove
import one.mixin.android.repository.PerpsMarketRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketCategory
import one.mixin.android.vo.market.MarketItem
import javax.inject.Inject

@HiltViewModel
internal class MarketSearchViewModel
    @Inject
    constructor(
        private val tokenRepository: TokenRepository,
        private val perpsMarketRepository: PerpsMarketRepository,
    ) : ViewModel() {
    private val _uiState = MutableStateFlow(MarketSearchUiState())
    val uiState: StateFlow<MarketSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var recentSearchJob: Job? = null
    private val _recentSearches = MutableStateFlow<List<MarketRecentSearch>>(emptyList())
    val recentSearches: StateFlow<List<MarketRecentSearch>> = _recentSearches.asStateFlow()

    private val initialPerpetualSyncJob =
        viewModelScope.launch(Dispatchers.IO) {
            if (perpsMarketRepository.getAllMarkets().isEmpty()) {
                runCatching { perpsMarketRepository.syncAllMarkets() }
            }
        }

    init {
        viewModelScope.launch {
            combine(
                tokenRepository.observeMarketsByCategory(MarketCategory.TRENDING),
                perpsMarketRepository.observeAllMarkets(),
            ) { spotMarkets, perpetualMarkets ->
                spotMarkets to perpetualMarkets.sortedForTrendingSearch()
            }.collect { (spotMarkets, perpetualMarkets) ->
                _uiState.update {
                    it.copy(
                        spotTrending = spotMarkets,
                        perpetualTrending = perpetualMarkets,
                    )
                }
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                selectedTab = selectedMarketSearchTab(it.query, query, it.selectedTab),
                spotResults = if (query.isBlank()) emptyList() else it.spotResults,
                perpetualResults = if (query.isBlank()) emptyList() else it.perpetualResults,
                isSearching = query.isNotBlank(),
            )
        }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(isSearching = false) }
            return
        }

        searchJob =
            viewModelScope.launch {
                delay(200)
                val expectedQuery = query.trim()
                val (spotMarkets, perpetualMarkets) =
                    coroutineScope {
                        val spot = async(Dispatchers.IO) { searchSpotMarkets(expectedQuery) }
                        val perpetual = async(Dispatchers.IO) { searchPerpetualMarkets(expectedQuery) }
                        spot.await() to perpetual.await()
                    }
                if (_uiState.value.query.trim() != expectedQuery) return@launch
                _uiState.update {
                    it.copy(
                        spotResults = spotMarkets,
                        perpetualResults = perpetualMarkets,
                        isSearching = false,
                    )
                }
            }
    }

    fun selectTab(tab: MarketSearchTab) {
        if (tab !in marketSearchTabs(_uiState.value.query)) return
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun loadRecentSearches(sp: SharedPreferences) {
        recentSearchJob?.cancel()
        recentSearchJob =
            viewModelScope.launch(Dispatchers.IO) {
                publishRecentSearches(readRecentSearches(sp))
            }
    }

    fun saveRecentSearch(
        sp: SharedPreferences,
        search: RecentSearch,
    ) {
        if (search.type != RecentSearchType.MARKET && search.type != RecentSearchType.PERPETUAL) return
        recentSearchJob?.cancel()
        recentSearchJob =
            viewModelScope.launch(Dispatchers.IO) {
                val searches = readRecentSearches(sp).addMarketRecentSearch(search)
                sp.putString(PREF_MARKET_RECENT_SEARCH, GsonHelper.customGson.toJson(searches))
            }
    }

    fun removeRecentSearch(sp: SharedPreferences) {
        recentSearchJob?.cancel()
        sp.remove(PREF_MARKET_RECENT_SEARCH)
        _recentSearches.value = emptyList()
    }

    suspend fun findSpotMarket(coinId: String): MarketItem? =
        withContext(Dispatchers.IO) {
            try {
                tokenRepository.checkMarketById(coinId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }

    suspend fun findPerpetualMarket(marketId: String) =
        perpsMarketRepository.getOrRefreshMarket(marketId)

    private suspend fun publishRecentSearches(searches: List<RecentSearch>) {
        val previousSearches = _recentSearches.value
        _recentSearches.value = searches.map { search ->
            val change = previousSearches.firstOrNull {
                it.search.type == search.type && it.search.primaryKey == search.primaryKey
            }?.change
            MarketRecentSearch(search, change)
        }
        val resolvedSearches = mutableListOf<MarketRecentSearch>()
        for (search in searches) {
            resolvedSearches += resolveRecentSearch(search)
        }
        _recentSearches.value = resolvedSearches
    }

    private suspend fun resolveRecentSearch(search: RecentSearch): MarketRecentSearch {
        return try {
            val change =
                when (search.type) {
                    RecentSearchType.MARKET ->
                        search.primaryKey
                            ?.let { findSpotMarket(it) }
                            ?.priceChangePercentage24H
                            ?.toBigDecimalOrNull()
                    RecentSearchType.PERPETUAL ->
                        search.primaryKey
                            ?.let { perpsMarketRepository.getOrRefreshMarket(it) }
                            ?.changePercentValue()
                    else -> null
                }
            MarketRecentSearch(search, change)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            MarketRecentSearch(search)
        }
    }

    private fun readRecentSearches(sp: SharedPreferences): List<RecentSearch> {
        val value = sp.getString(PREF_MARKET_RECENT_SEARCH, null)
        if (!value.isNullOrBlank()) {
            return parseRecentSearches(value)
        }
        val legacySearches = parseRecentSearches(sp.getString(PREF_RECENT_SEARCH, null))
        if (legacySearches.isNotEmpty()) {
            sp.putString(
                PREF_MARKET_RECENT_SEARCH,
                GsonHelper.customGson.toJson(legacySearches),
            )
        }
        return legacySearches
    }

    private fun parseRecentSearches(value: String?): List<RecentSearch> =
        if (value.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                GsonHelper.customGson.fromJson(value, Array<RecentSearch>::class.java).toList()
            }.getOrDefault(emptyList()).marketRecentSearches().take(MAX_MARKET_RECENT_SEARCHES)
        }

    private suspend fun searchSpotMarkets(query: String): List<MarketItem> =
        try {
            searchSpotMarketsOnlineFirst(
                query = query,
                searchLocalMarkets = { escapedQuery ->
                    tokenRepository.fuzzyMarkets(escapedQuery, CancellationSignal())
                },
                refreshOnlineMarkets = { normalizedQuery ->
                    tokenRepository.searchMarket(normalizedQuery)
                },
                resolveMarketItem = { market ->
                    tokenRepository.findMarketItemByCoinId(market.coinId) ?: MarketItem.fromMarket(market)
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }

    private suspend fun searchPerpetualMarkets(query: String) =
        try {
            perpsMarketRepository.searchMarketsOnlineFirst(query).sortedForMarketSearch(
                query = query,
                symbol = PerpsMarket::tokenSymbol,
                name = PerpsMarket::displaySymbol,
                volume = PerpsMarket::volume,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
}

internal suspend fun searchSpotMarketsOnlineFirst(
    query: String,
    searchLocalMarkets: suspend (escapedQuery: String) -> List<Market>,
    refreshOnlineMarkets: suspend (query: String) -> Unit,
    resolveMarketItem: suspend (Market) -> MarketItem,
): List<MarketItem> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return emptyList()

    val escapedQuery = normalizedQuery.escapeSql()
    val localMatches = searchLocalMarkets(escapedQuery)

    val markets =
        try {
            refreshOnlineMarkets(normalizedQuery)
            val refreshedMatches = searchLocalMarkets(escapedQuery)
            mergeLocalAndRefreshed(localMatches, refreshedMatches, Market::coinId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            localMatches
        }

    return markets
        .map { market -> resolveMarketItem(market) }
        .sortedForMarketSearch(
            query = normalizedQuery,
            symbol = MarketItem::symbol,
            name = MarketItem::name,
            volume = MarketItem::totalVolume,
        )
}
