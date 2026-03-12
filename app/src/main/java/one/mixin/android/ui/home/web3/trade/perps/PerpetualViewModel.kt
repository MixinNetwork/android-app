package one.mixin.android.ui.home.web3.trade.perps

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.api.request.perps.CloseOrderRequest
import one.mixin.android.api.request.perps.OpenOrderRequest
import one.mixin.android.api.request.perps.OpenOrderResponse
import one.mixin.android.api.response.perps.CandleView
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.service.RouteService
import one.mixin.android.db.TokenDao
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.db.perps.PerpsPositionDao
import one.mixin.android.db.perps.PerpsPositionHistoryDao
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshPerpsPositionsJob
import one.mixin.android.job.RefreshTokensJob
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.safe.TokenItem
import timber.log.Timber
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PerpetualViewModel @Inject constructor(
    private val routeService: RouteService,
    private val tokenDao: TokenDao,
    private val perpsPositionDao: PerpsPositionDao,
    private val perpsPositionHistoryDao: PerpsPositionHistoryDao,
    private val perpsMarketDao: PerpsMarketDao,
    private val jobManager: MixinJobManager
) : ViewModel() {

    fun refreshPositions(walletId: String) {
        jobManager.addJobInBackground(RefreshPerpsPositionsJob(walletId))
    }

    fun refreshPositionHistory(walletId: String, limit: Int = 100) {
        viewModelScope.launch {
            try {
                val latestClosedAt = withContext(Dispatchers.IO) {
                    perpsPositionHistoryDao.getLatestClosedAt(walletId)
                }
                val response = withContext(Dispatchers.IO) {
                    routeService.getPerpsPositionHistory(
                        walletId = walletId,
                        limit = limit,
                        offset = latestClosedAt
                    )
                }

                val data = response.data
                if (response.isSuccess && data != null) {
                    val histories = data.map { it.copy(walletId = walletId) }
                    withContext(Dispatchers.IO) {
                        perpsPositionHistoryDao.insertAll(histories)
                    }
                    Timber.d("Perps position history refreshed: ${histories.size} items")
                } else {
                    Timber.e("Failed to refresh position history: ${response.errorDescription}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing position history")
            }
        }
    }

    fun loadMarkets(
        onSuccess: (List<PerpsMarket>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val cachedMarkets = withContext(Dispatchers.IO) {
                    perpsMarketDao.getAllMarkets()
                }
                
                if (cachedMarkets.isNotEmpty()) {
                    onSuccess(cachedMarkets)
                }
                
                val response = withContext(Dispatchers.IO) {
                    routeService.getPerpsMarkets(offset = 0, limit = 100)
                }
                
                val data = response.data
                if (response.isSuccess && data != null) {
                    Timber.d("Perps markets loaded: ${data.size} markets")
                    
                    withContext(Dispatchers.IO) {
                        perpsMarketDao.insertAll(data)
                    }
                    
                    onSuccess(data)
                } else {
                    val error = "Failed to load markets: ${response.errorDescription}"
                    Timber.e(error)
                    if (cachedMarkets.isEmpty()) {
                        onError(error)
                    }
                }
            } catch (e: Exception) {
                val error = "Error loading markets: ${e.message}"
                Timber.e(e, error)
                
                val cachedMarkets = withContext(Dispatchers.IO) {
                    perpsMarketDao.getAllMarkets()
                }
                if (cachedMarkets.isEmpty()) {
                    onError(error)
                }
            }
        }
    }

    fun loadMarketDetail(
        marketId: String,
        onSuccess: (PerpsMarket) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    routeService.getPerpsMarket(marketId)
                }
                
                val data = response.data
                if (response.isSuccess && data != null) {
                    Timber.d("Market detail loaded: ${data.displaySymbol}")
                    onSuccess(data)
                } else {
                    val error = "Failed to load market detail: ${response.errorDescription}"
                    Timber.e(error)
                    onError(error)
                }
            } catch (e: Exception) {
                val error = "Error loading market detail: ${e.message}"
                Timber.e(e, error)
                onError(error)
            }
        }
    }

    fun loadCandles(
        marketId: String,
        timeFrame: String,
        onSuccess: (List<CandleView>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    routeService.getPerpsCandles(marketId, timeFrame)
                }
                
                val data = response.data
                if (response.isSuccess && data != null) {
                    Timber.d("Candles loaded: ${data.items.size} items")
                    onSuccess(listOf(data))
                } else {
                    val error = "Failed to load candles: ${response.errorDescription}"
                    Timber.e(error)
                    onError(error)
                }
            } catch (e: Exception) {
                val error = "Error loading candles: ${e.message}"
                Timber.e(e, error)
                onError(error)
            }
        }
    }

    fun loadAcceptedAssets(
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    routeService.getAcceptedAssets()
                }

                val data = response.data
                if (response.isSuccess && data != null) {
                    val acceptedAssetIds = data
                        .filter { it.isNotBlank() }
                        .distinct()

                    val missingAssetIds = withContext(Dispatchers.IO) {
                        val localIds = tokenDao.findTokenItems(acceptedAssetIds).map { it.assetId }.toSet()
                        acceptedAssetIds.filter { it !in localIds }
                    }

                    missingAssetIds.forEach { assetId ->
                        jobManager.addJobInBackground(RefreshTokensJob(assetId))
                    }

                    onSuccess(acceptedAssetIds)
                } else {
                    val error = "Failed to load accepted assets: ${response.errorDescription}"
                    Timber.e(error)
                    onError(error)
                }
            } catch (e: Exception) {
                val error = "Error loading accepted assets: ${e.message}"
                Timber.e(e, error)
                onError(error)
            }
        }
    }

    fun loadUsdTokens(onSuccess: (List<TokenItem>) -> Unit) {
        viewModelScope.launch {
            try {
                val usdTokens = withContext(Dispatchers.IO) {
                    val usdIds = Constants.usdIds
                    tokenDao.findTokenItems(usdIds)
                        .sortedByDescending { 
                            it.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        }
                }
                onSuccess(usdTokens)
            } catch (e: Exception) {
                Timber.e(e, "Error loading USD tokens")
                onSuccess(emptyList())
            }
        }
    }

    fun openPerpsOrder(
        assetId: String,
        marketId: String,
        side: String,
        amount: String,
        leverage: Int,
        walletId: String,
        destination: String? = null,
        entryPrice: String,
        onSuccess: (OpenOrderResponse) -> Unit,
        onError: (Int, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val request = OpenOrderRequest(
                    assetId = assetId,
                    marketId = marketId,
                    side = side,
                    amount = amount,
                    leverage = leverage,
                    walletId = walletId,
                    destination = destination
                )
                
                val response = withContext(Dispatchers.IO) {
                    routeService.openPerpsOrder(request)
                }
                
                val data = response.data
                if (response.isSuccess && data != null) {
                    Timber.d("Perps order opened: ${data.orderId}, payUrl: ${data.paymentUrl}")
                    
                    val position = PerpsPosition(
                        positionId = data.orderId,
                        marketId = marketId,
                        side = side,
                        quantity = amount,
                        settleAssetId = assetId,
                        botId = "",
                        entryPrice = entryPrice,
                        margin = amount,
                        openPayAmount = data.payAmount,
                        openPayAssetId = assetId,
                        leverage = leverage,
                        state = "pending",
                        markPrice = entryPrice,
                        unrealizedPnl = "0",
                        roe = "0",
                        walletId = walletId,
                        createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(
                            Date()
                        ),
                        updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(
                            Date()
                        )
                    )
                    
                    withContext(Dispatchers.IO) {
                        perpsPositionDao.insert(position)
                    }
                    
                    onSuccess(data)
                } else {
                    val error = response.errorDescription
                    Timber.e("Failed to open perps order: code=${response.errorCode}, description=$error")
                    onError(response.errorCode, error)
                }
            } catch (e: Exception) {
                val error = "Error opening perps order: ${e.message}"
                Timber.e(e, error)
                onError(-1, error)
            }
        }
    }

    fun getOpenPositions(walletId: String, onSuccess: (List<PerpsPositionItem>) -> Unit) {
        viewModelScope.launch {
            try {
                val positions = withContext(Dispatchers.IO) {
                    perpsPositionDao.getOpenPositions(walletId)
                }
                onSuccess(positions)
            } catch (e: Exception) {
                Timber.e(e, "Error loading open positions")
                onSuccess(emptyList())
            }
        }
    }

    fun observeOpenPositions(walletId: String): Flow<List<PerpsPositionItem>> {
        return perpsPositionDao.observeOpenPositions(walletId)
    }

    fun observePosition(positionId: String): Flow<PerpsPositionItem?> {
        return perpsPositionDao.observePosition(positionId)
    }

    fun observeTokenByChainAndSymbol(chainId: String, symbol: String): Flow<TokenItem?> {
        return tokenDao.assetItemFlowByChainAndSymbol(chainId, symbol)
    }

    fun observeTokenByAssetId(assetId: String): Flow<TokenItem?> {
        return tokenDao.assetItemFlow(assetId)
    }

    fun refreshSinglePosition(positionId: String, walletId: String? = null) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    routeService.getPerpsPosition(positionId)
                }

                if (response.isSuccess) {
                    val remotePosition = response.data
                    withContext(Dispatchers.IO) {
                        if (remotePosition != null) {
                            perpsPositionDao.insert(
                                remotePosition.copy(
                                    walletId = walletId ?: remotePosition.walletId
                                )
                            )
                        } else {
                            Timber.d("Skip deleting local position when remote detail is null: $positionId")
                        }
                    }
                } else if (response.errorCode == ErrorHandler.NOT_FOUND) {
                    Timber.d("Skip deleting local position on NOT_FOUND during refresh: $positionId")
                } else {
                    Timber.e("Failed to refresh position detail: ${response.errorDescription}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing position detail: ${e.message}")
            }
        }
    }

    fun observeClosedPositions(walletId: String, limit: Int): Flow<List<PerpsPositionHistoryItem>> {
        return perpsPositionHistoryDao.observeHistories(walletId, limit)
    }

    suspend fun getOpenPositionsFromDb(walletId: String): List<PerpsPositionItem> {
        return withContext(Dispatchers.IO) {
            try {
                perpsPositionDao.getOpenPositions(walletId)
            } catch (e: Exception) {
                Timber.e(e, "Error loading open positions from db")
                emptyList()
            }
        }
    }

    suspend fun getClosedPositionsFromDb(walletId: String, limit: Int): List<PerpsPositionHistoryItem> {
        return withContext(Dispatchers.IO) {
            try {
                perpsPositionHistoryDao.getHistories(walletId, limit)
            } catch (e: Exception) {
                Timber.e(e, "Error loading closed positions from db")
                emptyList()
            }
        }
    }

    fun getTotalUnrealizedPnl(walletId: String, onSuccess: (Double) -> Unit) {
        viewModelScope.launch {
            try {
                val pnl = withContext(Dispatchers.IO) {
                    perpsPositionDao.getTotalUnrealizedPnl(walletId) ?: 0.0
                }
                onSuccess(pnl)
            } catch (e: Exception) {
                Timber.e(e, "Error loading total PnL")
                onSuccess(0.0)
            }
        }
    }

    fun observeTotalUnrealizedPnl(walletId: String): Flow<Double> {
        return perpsPositionDao.observeTotalUnrealizedPnl(walletId)
    }

    fun observeTotalOpenPositionValue(walletId: String): Flow<Double> {
        return perpsPositionDao.observeTotalOpenPositionValue(walletId)
    }

    suspend fun getTotalUnrealizedPnlFromDb(walletId: String): Double {
        return withContext(Dispatchers.IO) {
            try {
                perpsPositionDao.getTotalUnrealizedPnl(walletId) ?: 0.0
            } catch (e: Exception) {
                Timber.e(e, "Error loading total unrealized PnL from db")
                0.0
            }
        }
    }

    suspend fun getTotalOpenPositionValueFromDb(walletId: String): Double {
        return withContext(Dispatchers.IO) {
            try {
                perpsPositionDao.getTotalOpenPositionValue(walletId) ?: 0.0
            } catch (e: Exception) {
                Timber.e(e, "Error loading total open position value from db")
                0.0
            }
        }
    }

    suspend fun getTotalRealizedPnlFromDb(walletId: String): Double {
        return withContext(Dispatchers.IO) {
            try {
                perpsPositionHistoryDao.getTotalRealizedPnl(walletId) ?: 0.0
            } catch (e: Exception) {
                Timber.e(e, "Error loading total realized PnL from db")
                0.0
            }
        }
    }

    fun observeTotalRealizedPnl(walletId: String): Flow<Double> {
        return perpsPositionHistoryDao.observeTotalRealizedPnl(walletId)
    }

    suspend fun getTotalClosedEntryValueFromDb(walletId: String): Double {
        return withContext(Dispatchers.IO) {
            try {
                perpsPositionHistoryDao.getTotalClosedEntryValue(walletId) ?: 0.0
            } catch (e: Exception) {
                Timber.e(e, "Error loading total closed entry value from db")
                0.0
            }
        }
    }

    fun observeTotalClosedEntryValue(walletId: String): Flow<Double> {
        return perpsPositionHistoryDao.observeTotalClosedEntryValue(walletId)
    }

    fun getOpenPositionsPaged(walletId: String, initialLoadKey: Int? = 0): LiveData<PagedList<PerpsPositionItem>> {
        val config = PagedList.Config.Builder()
            .setPrefetchDistance(Constants.PAGE_SIZE * 2)
            .setPageSize(Constants.PAGE_SIZE)
            .setEnablePlaceholders(false)
            .build()
        return LivePagedListBuilder(perpsPositionDao.getOpenPositionsPaged(walletId), config)
            .setInitialLoadKey(initialLoadKey)
            .build()
    }

    fun getClosedPositionsPaged(
        walletId: String,
        initialLoadKey: Int? = 0
    ): LiveData<PagedList<PerpsPositionHistoryItem>> {
        val config = PagedList.Config.Builder()
            .setPrefetchDistance(Constants.PAGE_SIZE * 2)
            .setPageSize(Constants.PAGE_SIZE)
            .setEnablePlaceholders(false)
            .build()
        return LivePagedListBuilder(perpsPositionHistoryDao.getHistoriesPaged(walletId), config)
            .setInitialLoadKey(initialLoadKey)
            .build()
    }

    fun loadOpenPositions(walletId: String, onSuccess: (List<PerpsPositionItem>) -> Unit) {
        viewModelScope.launch {
            try {
                val positions = withContext(Dispatchers.IO) {
                    perpsPositionDao.getOpenPositions(walletId)
                }
                onSuccess(positions)
            } catch (e: Exception) {
                Timber.e(e, "Error loading open positions")
                onSuccess(emptyList())
            }
        }
    }

    fun getPositionByMarket(walletId: String, marketId: String, onSuccess: (PerpsPositionItem?) -> Unit) {
        viewModelScope.launch {
            try {
                val positions = withContext(Dispatchers.IO) {
                    perpsPositionDao.getOpenPositions(walletId)
                }
                val position = positions.firstOrNull { it.marketId == marketId }
                onSuccess(position)
            } catch (e: Exception) {
                Timber.e(e, "Error loading position by market")
                onSuccess(null)
            }
        }
    }

    fun getClosedPositionsByMarket(walletId: String, marketId: String, onSuccess: (List<PerpsPositionHistoryItem>) -> Unit) {
        viewModelScope.launch {
            try {
                val allHistories = withContext(Dispatchers.IO) {
                    perpsPositionHistoryDao.getHistories(walletId, 100)
                }
                val filteredHistories = allHistories.filter { it.marketId == marketId }
                onSuccess(filteredHistories)
            } catch (e: Exception) {
                Timber.e(e, "Error loading closed positions by market")
                onSuccess(emptyList())
            }
        }
    }

    fun closePerpsOrder(
        positionId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val request = CloseOrderRequest(
                    positionId = positionId
                )
                
                val response = withContext(Dispatchers.IO) {
                    routeService.closePerpsOrder(request)
                }
                
                if (response.isSuccess) {
                    withContext(Dispatchers.IO) {
                        perpsPositionDao.deleteById(positionId)
                    }
                    Timber.d("Perps order closed: $positionId")
                    onSuccess()
                } else {
                    val error = "Failed to close perps order: ${response.errorDescription}"
                    Timber.e(error)
                    onError(error)
                }
            } catch (e: Exception) {
                val error = "Error closing perps order: ${e.message}"
                Timber.e(e, error)
                onError(error)
            }
        }
    }

    fun deletePosition(positionId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    perpsPositionDao.deleteById(
                        positionId
                    )
                }
                Timber.d("Position deleted: $positionId")
            } catch (e: Exception) {
                Timber.e(e, "Error deleting position: ${e.message}")
            }
        }
    }

    suspend fun getPositionFromDb(positionId: String): PerpsPositionItem? {
        return withContext(Dispatchers.IO) {
            perpsPositionDao.getPosition(positionId)
        }
    }

    suspend fun getMarketFromDb(marketId: String): PerpsMarket? {
        return withContext(Dispatchers.IO) {
            perpsMarketDao.getMarket(marketId)
        }
    }

    fun loadPositionDetail(
        positionId: String,
        onSuccess: (PerpsPosition) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val localBefore = withContext(Dispatchers.IO) {
                    perpsPositionDao.getPosition(positionId)
                }

                val response = withContext(Dispatchers.IO) {
                    routeService.getPerpsPosition(positionId)
                }
                
                val data = response.data
                if (response.isSuccess && data != null) {
                    val resolvedWalletId = data.walletId ?: localBefore?.walletId
                    val positionForDb = data.copy(walletId = resolvedWalletId)
                    
                    withContext(Dispatchers.IO) {
                        perpsPositionDao.insert(positionForDb)
                    }
                    
                    onSuccess(positionForDb)
                } else {
                    val error = "Failed to load position detail: ${response.errorDescription}"
                    Timber.e(error)
                    onError(error)
                }
            } catch (e: Exception) {
                val error = "Error loading position detail: ${e.message}"
                Timber.e(e, error)
                onError(error)
            }
        }
    }

    fun loadPositionHistory(
        walletId: String,
        limit: Int = 10,
        offset: String? = null,
        onSuccess: (List<PerpsPositionHistoryItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val cachedHistories = withContext(Dispatchers.IO) {
                    perpsPositionHistoryDao.getHistories(walletId, limit, offset)
                }
                
                if (cachedHistories.isNotEmpty()) {
                    onSuccess(cachedHistories)
                }
                
                val response = withContext(Dispatchers.IO) {
                    routeService.getPerpsPositionHistory(
                        walletId = walletId,
                        limit = limit,
                        offset = offset
                    )
                }
                
                val data = response.data
                if (response.isSuccess && data != null) {
                    Timber.d("Position history loaded: ${data.size} items")
                    
                    val histories = data.map { it.copy(walletId = walletId) }
                    
                    withContext(Dispatchers.IO) {
                        perpsPositionHistoryDao.insertAll(histories)
                    }
                    
                    val updatedHistories = withContext(Dispatchers.IO) {
                        perpsPositionHistoryDao.getHistories(walletId, limit, offset)
                    }
                    onSuccess(updatedHistories)
                } else {
                    val error = "Failed to load position history: ${response.errorDescription}"
                    Timber.e(error)
                    if (cachedHistories.isEmpty()) {
                        onError(error)
                    }
                }
            } catch (e: Exception) {
                val error = "Error loading position history: ${e.message}"
                Timber.e(e, error)
                
                val cachedHistories = withContext(Dispatchers.IO) {
                    perpsPositionHistoryDao.getHistories(walletId, limit, offset)
                }
                if (cachedHistories.isEmpty()) {
                    onError(error)
                }
            }
        }
    }
}
