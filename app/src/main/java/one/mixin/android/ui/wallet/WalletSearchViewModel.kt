package one.mixin.android.ui.wallet

import android.app.Application
import android.content.SharedPreferences
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Account.PREF_WALLET_RECEIVE
import one.mixin.android.Constants.Account.PREF_WALLET_SEARCH_RECENT
import one.mixin.android.Constants.Account.PREF_WALLET_SEND
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.db.perps.PerpsPositionDao
import one.mixin.android.extension.addToList
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.escapeSql
import one.mixin.android.extension.getStringList
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.putString
import one.mixin.android.repository.PerpsMarketRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.session.Session
import one.mixin.android.ui.wallet.home.WalletHomeEarnAccount
import one.mixin.android.ui.wallet.home.WalletHomeType
import one.mixin.android.ui.wallet.home.getWalletHomeCacheState
import one.mixin.android.ui.wallet.home.walletHomeCacheKey
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType
import timber.log.Timber
import javax.inject.Inject

internal fun SharedPreferences.walletSearchSharedHistory(key: String): List<String> =
    if (getBoolean("${PREF_WALLET_SEARCH_RECENT}_cleared", false)) emptyList() else getStringList(key).orEmpty()

internal fun SharedPreferences.clearWalletSearchHistory() {
    edit().remove(PREF_WALLET_SEARCH_RECENT).putBoolean("${PREF_WALLET_SEARCH_RECENT}_cleared", true).apply()
}

internal fun matchesWalletPosition(query: String, symbol: String?, tags: List<String>): Boolean =
    symbol.orEmpty().contains(query, ignoreCase = true) || tags.any { it.contains(query, ignoreCase = true) }

@HiltViewModel
internal class WalletSearchViewModel @Inject constructor(
    application: Application,
    private val tokenRepository: TokenRepository,
    private val perpsMarketRepository: PerpsMarketRepository,
    private val positionDao: PerpsPositionDao,
) : ViewModel() {
    private val preferences = application.defaultSharedPreferences
    private val _uiState = MutableStateFlow(WalletSearchUiState())
    val uiState = _uiState.asStateFlow()
    private val _recentSearches = MutableStateFlow<List<WalletSearchResult>>(emptyList())
    val recentSearches = _recentSearches.asStateFlow()
    private var searchJob: Job? = null
    private var recentJob: Job? = null

    fun updateQuery(query: String) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = query,
                selectedTab = walletSearchTabForQuery(it.query, query, it.selectedTab),
                results = if (query.isBlank()) emptyList() else it.results,
                isSearching = query.isNotBlank(),
            )
        }
        if (query.isBlank()) return
        searchJob = viewModelScope.launch {
            delay(200)
            val results = withContext(Dispatchers.IO) {
                try {
                    searchLocal(query.trim())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "Wallet local search failed")
                    emptyList()
                }
            }
            if (_uiState.value.query != query) return@launch
            _uiState.update {
                it.copy(
                    results = results,
                    selectedTab = selectedWalletSearchTab(it.selectedTab, walletSearchTabs(results.map { result -> result.tab })),
                    isSearching = false,
                )
            }
        }
    }

    fun selectTab(tab: WalletSearchTab) {
        _uiState.update { it.copy(selectedTab = selectedWalletSearchTab(tab, walletSearchTabs(it.results.map { result -> result.tab }))) }
    }

    fun loadRecentSearches() {
        recentJob?.cancel()
        recentJob = viewModelScope.launch {
            val resolved = withContext(Dispatchers.IO) {
                val searches = readRecentSearches()
                val send = preferences.walletSearchSharedHistory(PREF_WALLET_SEND)
                val receive = preferences.walletSearchSharedHistory(PREF_WALLET_RECEIVE)
                val assetIds = searches.filter { it.type == RecentSearchType.ASSET }.mapNotNull { it.primaryKey } + send + receive
                val assets = tokenRepository.findAssetsByIds(assetIds.distinct()).associateBy { it.assetId }
                val collectibleHashes = searches.filter { it.type == RecentSearchType.COLLECTIBLE }.mapNotNull { it.primaryKey }
                val collectibles = tokenRepository.findCollectiblesByHashes(collectibleHashes).associateBy { it.inscriptionHash }
                val earnAccounts = preferences.getWalletHomeCacheState(walletHomeCacheKey(WalletHomeType.PRIVACY, Session.getAccountId().orEmpty()))
                    ?.earnAccounts.orEmpty().associateBy { it.assetId }
                searches.mergeWalletRecentSearches(send, receive) {
                    when (it.type) {
                        RecentSearchType.ASSET -> it.primaryKey in assets
                        RecentSearchType.COLLECTIBLE -> it.primaryKey in collectibles
                        RecentSearchType.EARN -> it.primaryKey in earnAccounts
                        else -> false
                    }
                }.mapNotNull {
                    when (it.type) {
                        RecentSearchType.ASSET -> assets[it.primaryKey]?.let { token -> WalletSearchResult.Asset(token) }
                        RecentSearchType.COLLECTIBLE -> collectibles[it.primaryKey]?.let { collectible -> WalletSearchResult.Collectible(collectible) }
                        RecentSearchType.EARN -> earnAccounts[it.primaryKey]?.let { account -> earnResult(account) }
                        else -> null
                    }
                }
            }
            _recentSearches.value = resolved
        }
    }

    fun saveRecentSearch(result: WalletSearchResult) {
        val search = when (result) {
            is WalletSearchResult.Asset -> RecentSearch(RecentSearchType.ASSET, primaryKey = result.token.assetId)
            is WalletSearchResult.Position -> return
            is WalletSearchResult.Collectible -> RecentSearch(RecentSearchType.COLLECTIBLE, primaryKey = result.collectible.inscriptionHash)
            is WalletSearchResult.Earn -> RecentSearch(RecentSearchType.EARN, primaryKey = result.account.assetId)
        }
        val searches = readRecentSearches().addWalletRecentSearch(search)
        preferences.putString(PREF_WALLET_SEARCH_RECENT, GsonHelper.customGson.toJson(searches))
        if (result is WalletSearchResult.Asset) preferences.addToList(PREF_WALLET_SEND, result.token.assetId)
        loadRecentSearches()
    }

    fun clearRecentSearches() {
        recentJob?.cancel()
        preferences.clearWalletSearchHistory()
        _recentSearches.value = emptyList()
    }

    suspend fun findOrSyncAsset(assetId: String) = withContext(Dispatchers.IO) { tokenRepository.findOrSyncAsset(assetId) }

    suspend fun findPerpetualMarket(marketId: String) = withContext(Dispatchers.IO) { perpsMarketRepository.getMarket(marketId) }

    private fun readRecentSearches(): List<RecentSearch> =
        runCatching {
            preferences.getString(PREF_WALLET_SEARCH_RECENT, null)?.let {
                GsonHelper.customGson.fromJson(it, Array<RecentSearch>::class.java).toList()
            }.orEmpty()
        }.getOrDefault(emptyList())

    private suspend fun positionResult(position: PerpsPositionItem): WalletSearchResult.Position {
        val symbol = position.tokenSymbol.orEmpty()
        val name = if (symbol.isBlank()) null else tokenRepository.fuzzySearchAssetIgnoreAmount(symbol.escapeSql())
            .firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }?.name
        return WalletSearchResult.Position(position, name?.takeIf { it.isNotBlank() } ?: symbol)
    }

    private suspend fun earnResult(account: WalletHomeEarnAccount): WalletSearchResult.Earn {
        val price = tokenRepository.findAssetItemById(account.assetId)?.priceUsd?.toBigDecimalOrNull()
        val amount = walletSearchEarnAmount(account.balanceUsd, price)?.let {
            "${it.numberFormat8()} ${account.assetSymbol}"
        } ?: account.balanceAmountText
        return WalletSearchResult.Earn(account, amount)
    }

    private suspend fun searchLocal(query: String): List<WalletSearchResult> {
        val escaped = query.escapeSql()
        val assets = sortQueryAsset(query, tokenRepository.fuzzySearchAssetIgnoreAmount(escaped), emptyList())
            .map { WalletSearchResult.Asset(it) }
        val openPositions = positionDao.getOpenPositions(Session.getAccountId().orEmpty())
        val markets = perpsMarketRepository.getMarkets(openPositions.map { it.marketId }.distinct()).associateBy { it.marketId }
        val positions = openPositions
            .filter { matchesWalletPosition(query, it.tokenSymbol, markets[it.marketId]?.tags.orEmpty()) }
            .map { positionResult(it) }
        val earn = preferences.getWalletHomeCacheState(walletHomeCacheKey(WalletHomeType.PRIVACY, Session.getAccountId().orEmpty()))
            ?.earnAccounts.orEmpty()
            .filter { it.assetSymbol.contains(query, true) }
            .map { account -> earnResult(account) }
        val signal = CancellationSignal()
        val collectibles = try {
            tokenRepository.fuzzyInscription(escaped, signal).map { WalletSearchResult.Collectible(it) }
        } finally {
            signal.cancel()
        }
        return assets + positions + earn + collectibles
    }
}
