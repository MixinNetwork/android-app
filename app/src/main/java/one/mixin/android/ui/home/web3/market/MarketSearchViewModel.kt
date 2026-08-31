package one.mixin.android.ui.home.web3.market

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
import one.mixin.android.extension.escapeSql
import one.mixin.android.repository.PerpsMarketRepository
import one.mixin.android.repository.TokenRepository
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

    private suspend fun searchSpotMarkets(query: String): List<MarketItem> =
        try {
            val escapedQuery = query.escapeSql()
            var markets = tokenRepository.fuzzyMarkets(escapedQuery, CancellationSignal())
            if (markets.isEmpty()) {
                tokenRepository.searchMarket(query)
                markets = tokenRepository.fuzzyMarkets(escapedQuery, CancellationSignal())
            }
            markets.map { market ->
                tokenRepository.findMarketItemByCoinId(market.coinId) ?: MarketItem.fromMarket(market)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }

    private suspend fun searchPerpetualMarkets(query: String) =
        try {
            initialPerpetualSyncJob.join()
            perpsMarketRepository.searchMarkets(query)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }
