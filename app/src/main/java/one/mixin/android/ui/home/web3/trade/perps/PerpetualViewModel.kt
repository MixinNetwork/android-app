package one.mixin.android.ui.home.web3.trade.perps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.api.request.perps.CloseOrderRequest
import one.mixin.android.api.request.perps.IncreaseOrderRequest
import one.mixin.android.api.request.perps.OpenOrderRequest
import one.mixin.android.api.request.perps.OpenOrderResponse
import one.mixin.android.api.request.perps.PositionTpSlRequest
import one.mixin.android.api.response.perps.CandleView
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsOrder
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.toPosition
import one.mixin.android.api.response.perps.withDefaults
import one.mixin.android.api.service.RouteService
import one.mixin.android.db.PerpsDatabase
import one.mixin.android.db.TokenDao
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.db.perps.PerpsOrderDao
import one.mixin.android.db.perps.PerpsPositionDao
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshPerpsPositionsJob
import one.mixin.android.job.RefreshTokensJob
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
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
    private val perpsDatabase: PerpsDatabase,
    private val perpsPositionDao: PerpsPositionDao,
    private val perpsOrderDao: PerpsOrderDao,
    private val perpsMarketDao: PerpsMarketDao,
    private val jobManager: MixinJobManager
) : ViewModel() {
    data class BatchCloseResult(
        val failedPositions: List<PerpsPositionItem>,
        val errors: List<String>,
    )

    fun refreshPositions(walletId: String) {
        jobManager.addJobInBackground(RefreshPerpsPositionsJob(walletId))
    }

    suspend fun refreshPositionsForBatchClose(walletId: String): Result<List<PerpsPositionItem>> {
        return runCatching {
            val response = withContext(Dispatchers.IO) {
                routeService.getPerpsPositions(walletId = walletId)
            }
            val positions = response.data?.map { it.copy(walletId = walletId) }
            if (!response.isSuccess || positions == null) {
                error("Failed to refresh perps positions: ${response.errorDescription}")
            }

            withContext(Dispatchers.IO) {
                perpsDatabase.withTransaction {
                    if (positions.isEmpty()) {
                        perpsPositionDao.deleteOpenByWallet(walletId)
                    } else {
                        perpsPositionDao.deleteOpenByWalletAndNotIn(
                            walletId,
                            positions.map { it.positionId },
                        )
                        perpsPositionDao.insertAll(positions)
                    }
                }
                perpsPositionDao.getOpenPositions(walletId)
            }
        }.onFailure { error ->
            Timber.e(error, "Failed to refresh positions before batch close")
        }
    }

    private var refreshOrdersJob: kotlinx.coroutines.Job? = null

    fun startRefreshOrders(walletId: String, intervalMs: Long = 10_000L) {
        if (refreshOrdersJob?.isActive == true) return
        refreshOrdersJob = viewModelScope.launch {
            while (isActive) {
                refreshOrders(walletId)
                delay(intervalMs)
            }
        }
    }

    fun stopRefreshOrders() {
        refreshOrdersJob?.cancel()
    }

    fun refreshOrders(walletId: String, limit: Int = 100) {
        viewModelScope.launch {
            try {
                val latestUpdatedAt = withContext(Dispatchers.IO) {
                    perpsOrderDao.getLatestUpdatedAt()
                }
                val response = withContext(Dispatchers.IO) {
                    routeService.getPerpsOrders(
                        walletId = walletId,
                        limit = limit,
                        offset = latestUpdatedAt,
                    )
                }

                val data = response.data
                if (response.isSuccess && data != null) {
                    withContext(Dispatchers.IO) {
                        upsertSyncedOrders(data)
                    }
                    Timber.d("Perps orders refreshed: ${data.size} items")
                } else {
                    Timber.e("Failed to refresh orders: ${response.errorDescription}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing orders")
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
                    routeService.getPerpsMarkets()
                }
                
                val data = response.data
                if (response.isSuccess && data != null) {
                    Timber.d("Perps markets loaded: ${data.size} markets")
                    val normalizedMarkets = data.map(PerpsMarket::withDefaults)

                    val orderedMarkets = withContext(Dispatchers.IO) {
                        perpsMarketDao.upsertList(normalizedMarkets)
                        perpsMarketDao.getAllMarkets()
                    }

                    onSuccess(orderedMarkets)
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

    suspend fun getMarketById(marketId: String): PerpsMarket? {
        val cachedMarket = withContext(Dispatchers.IO) {
            perpsMarketDao.getMarket(marketId)
        }
        if (cachedMarket != null) return cachedMarket

        return try {
            val response = withContext(Dispatchers.IO) {
                routeService.getPerpsMarket(marketId)
            }
            val data = response.data
            if (response.isSuccess && data != null) {
                val normalizedMarket = data.withDefaults()
                withContext(Dispatchers.IO) {
                    perpsMarketDao.insert(normalizedMarket)
                }
                normalizedMarket
            } else {
                Timber.e("Failed to load perps market for $marketId: ${response.errorDescription}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading perps market for $marketId")
            null
        }
    }

    fun observeMarkets(): Flow<List<PerpsMarket>> {
        return perpsMarketDao.observeAllMarkets()
    }

    fun refreshMarkets(
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    routeService.getPerpsMarkets()
                }

                val data = response.data
                if (response.isSuccess && data != null) {
                    val normalizedMarkets = data.map(PerpsMarket::withDefaults)
                    withContext(Dispatchers.IO) {
                        perpsMarketDao.upsertList(normalizedMarkets)
                    }
                    Timber.d("Perps markets refreshed: ${normalizedMarkets.size} markets")
                } else {
                    val error = "Failed to refresh markets: ${response.errorDescription}"
                    Timber.e(error)
                    onError?.invoke(error)
                }
            } catch (e: Exception) {
                val error = "Error refreshing markets: ${e.message}"
                Timber.e(e, error)
                onError?.invoke(error)
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
                    val normalizedMarket = data.withDefaults()
                    Timber.d("Market detail loaded: ${normalizedMarket.displaySymbol}")
                    onSuccess(normalizedMarket)
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
                    onError(
                        MixinApplication.appContext.getMixinErrorStringByCode(
                            response.errorCode,
                            response.errorDescription
                        )
                    )
                }
            } catch (e: Exception) {
                val error = "Error loading candles: ${e.message}"
                Timber.e(e, error)
                onError(ErrorHandler.getErrorMessage(e))
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

    suspend fun estimateLiquidationPrice(
        amount: String,
        marketId: String? = null,
        side: String? = null,
        leverage: Int? = null,
        positionId: String? = null,
    ): String? {
        return try {
            val response = withContext(Dispatchers.IO) {
                routeService.getPerpsLiquidationPrice(
                    marketId = marketId,
                    amount = amount,
                    side = side,
                    leverage = leverage,
                    positionId = positionId,
                )
            }
            if (response.isSuccess) {
                response.data?.liquidationPrice?.takeIf { it.isNotBlank() }
            } else {
                Timber.e("Failed to estimate liquidation price: ${response.errorDescription}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error estimating liquidation price")
            null
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
        takeProfitPrice: String? = null,
        stopLossPrice: String? = null,
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
                    takeProfitPrice = takeProfitPrice,
                    stopLossPrice = stopLossPrice,
                    destination = destination
                )
                
                val response = withContext(Dispatchers.IO) {
                    routeService.openPerpsOrder(request)
                }
                
                val data = response.data
                if (response.isSuccess && data != null) {
                    Timber.d("Perps order opened: ${data.orderId}, payUrl: ${data.paymentUrl}")
                    
                    val entryPriceDecimal = entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val amountDecimal = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val quantityValue = if (entryPriceDecimal > BigDecimal.ZERO) {
                        amountDecimal
                            .multiply(BigDecimal(leverage))
                            .divide(entryPriceDecimal, 8, java.math.RoundingMode.HALF_UP)
                            .stripTrailingZeros()
                            .toPlainString()
                    } else {
                        "0"
                    }
                    
                    val position = PerpsPosition(
                        positionId = data.orderId,
                        marketId = marketId,
                        side = side,
                        quantity = quantityValue,
                        settleAssetId = assetId,
                        botId = "",
                        entryPrice = entryPrice,
                        margin = data.payAmount,
                        openPayAmount = data.payAmount,
                        openPayAssetId = assetId,
                        takeProfitPrice = takeProfitPrice,
                        stopLossPrice = stopLossPrice,
                        liquidationPrice = null,
                        leverage = leverage,
                        state = PerpsPosition.STATE_OPENING,
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
                        perpsPositionDao.upsertSuspend(position)
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

    fun increasePerpsPosition(
        positionId: String,
        assetId: String,
        amount: String,
        position: PerpsPositionItem? = null,
        destination: String? = null,
        price: String? = null,
        takeProfitPrice: String? = null,
        stopLossPrice: String? = null,
        onSuccess: (OpenOrderResponse) -> Unit,
        onError: (Int, String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val request = IncreaseOrderRequest(
                    assetId = assetId,
                    amount = amount,
                    destination = destination,
                    price = price,
                    takeProfitPrice = takeProfitPrice,
                    stopLossPrice = stopLossPrice,
                )

                val response = withContext(Dispatchers.IO) {
                    routeService.increasePerpsPosition(positionId, request)
                }

                val data = response.data
                if (response.isSuccess && data != null) {
                    val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
                    withContext(Dispatchers.IO) {
                        val localPosition = perpsPositionDao.getPosition(positionId)?.toPosition()
                            ?: position?.toPosition()
                        if (localPosition != null) {
                            perpsPositionDao.upsertSuspend(
                                localPosition.copy(
                                    state = PerpsPosition.STATE_ADDING,
                                    updatedAt = now,
                                )
                            )
                        } else {
                            perpsPositionDao.updateStatus(positionId, PerpsPosition.STATE_ADDING, now)
                        }
                    }
                    onSuccess(data)
                } else {
                    onError(response.errorCode, response.errorDescription)
                }
            } catch (e: Exception) {
                val error = "Error increasing perps position: ${e.message}"
                Timber.e(e, error)
                onError(-1, error)
            }
        }
    }

    fun setPositionTpSl(
        positionId: String,
        takeProfitPrice: String? = null,
        stopLossPrice: String? = null,
        onSuccess: (PerpsPosition) -> Unit,
        onError: (Int, String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    routeService.setPerpsPositionTpSl(
                        PositionTpSlRequest(
                            positionId = positionId,
                            takeProfitPrice = takeProfitPrice,
                            stopLossPrice = stopLossPrice,
                        )
                    )
                }

                val data = response.data
                if (response.isSuccess && data != null) {
                    withContext(Dispatchers.IO) {
                        perpsPositionDao.upsertSuspend(data)
                    }
                    onSuccess(data)
                } else {
                    onError(response.errorCode, response.errorDescription)
                }
            } catch (e: Exception) {
                val error = "Error setting position TP/SL: ${e.message}"
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
                            perpsPositionDao.upsertSuspend(
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

    fun observeOrders(walletId: String, limit: Int): Flow<List<PerpsOrderItem>> {
        return perpsOrderDao.observeOrders(limit)
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

    suspend fun getOrdersFromDb(walletId: String, limit: Int): List<PerpsOrderItem> {
        return withContext(Dispatchers.IO) {
            try {
                perpsOrderDao.getOrders(limit)
            } catch (e: Exception) {
                Timber.e(e, "Error loading orders from db")
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
                perpsOrderDao.getTotalRealizedPnl() ?: 0.0
            } catch (e: Exception) {
                Timber.e(e, "Error loading total realized PnL from db")
                0.0
            }
        }
    }

    fun observeTotalRealizedPnl(walletId: String): Flow<Double> {
        return perpsOrderDao.observeTotalRealizedPnl()
    }

    suspend fun getTotalClosedEntryValueFromDb(walletId: String): Double {
        return withContext(Dispatchers.IO) {
            try {
                perpsOrderDao.getTotalClosedEntryValue() ?: 0.0
            } catch (e: Exception) {
                Timber.e(e, "Error loading total closed entry value from db")
                0.0
            }
        }
    }

    fun observeTotalClosedEntryValue(walletId: String): Flow<Double> {
        return perpsOrderDao.observeTotalClosedEntryValue()
    }

    fun getOpenPositionsPaged(walletId: String): Flow<PagingData<PerpsPositionItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = Constants.PAGE_SIZE,
                prefetchDistance = Constants.PAGE_SIZE * 2,
                enablePlaceholders = false,
            ),
        ) {
            perpsPositionDao.getOpenPositionsPaged(walletId)
        }.flow.cachedIn(viewModelScope)
    }

    fun getOrdersPaged(walletId: String): Flow<PagingData<PerpsOrderItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = Constants.PAGE_SIZE,
                prefetchDistance = Constants.PAGE_SIZE * 2,
                enablePlaceholders = false,
            ),
        ) {
            perpsOrderDao.getOrdersPaged()
        }.flow.cachedIn(viewModelScope)
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

    fun getOrdersByMarket(walletId: String, marketId: String, onSuccess: (List<PerpsOrderItem>) -> Unit) {
        viewModelScope.launch {
            try {
                val orders = withContext(Dispatchers.IO) {
                    perpsOrderDao.getOrdersByMarket(marketId)
                }
                onSuccess(orders)
            } catch (e: Exception) {
                Timber.e(e, "Error loading orders by market")
                onSuccess(emptyList())
            }
        }
    }

    fun closePerpsOrder(
        positionId: String,
        leverage: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            closePerpsOrder(positionId, leverage)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message.orEmpty()) }
        }
    }

    fun closePerpsOrders(
        positions: List<PerpsPositionItem>,
        onComplete: (BatchCloseResult) -> Unit,
    ) {
        viewModelScope.launch {
            val failedPositions = mutableListOf<PerpsPositionItem>()
            val errors = mutableListOf<String>()

            positions.forEach { position ->
                closePerpsOrder(position.positionId, position.leverage)
                    .onFailure { error ->
                        failedPositions += position
                        errors += error.message.orEmpty()
                    }
            }

            onComplete(BatchCloseResult(failedPositions, errors))
        }
    }

    private suspend fun closePerpsOrder(
        positionId: String,
        leverage: Int,
    ): Result<Unit> {
        return runCatching {
            val response = withContext(Dispatchers.IO) {
                routeService.closePerpsOrder(CloseOrderRequest(positionId = positionId))
            }

            if (!response.isSuccess) {
                error("Failed to close perps order: ${response.errorDescription}")
            }

            withContext(Dispatchers.IO) {
                perpsPositionDao.getPosition(positionId)?.let { position ->
                    perpsOrderDao.insert(
                        createCachedClosedOrder(
                            position = position,
                            leverage = leverage.takeIf { it > 0 } ?: position.leverage,
                        )
                    )
                }
                perpsPositionDao.deleteById(positionId)
            }
            Timber.d("Perps order closed: $positionId")
        }.onFailure { error ->
            Timber.e(error, "Error closing perps order: $positionId")
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
                    val resolvedWalletId = data.walletId.ifBlank { localBefore?.walletId ?: "" }
                    val positionForDb = data.copy(walletId = resolvedWalletId)
                    
                    withContext(Dispatchers.IO) {
                        perpsPositionDao.upsertSuspend(positionForDb)
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

    fun loadOrders(
        walletId: String,
        limit: Int = 10,
        offset: String? = null,
        onSuccess: (List<PerpsOrderItem>) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val cached = withContext(Dispatchers.IO) {
                    perpsOrderDao.getOrders(limit, offset)
                }

                if (cached.isNotEmpty()) {
                    onSuccess(cached)
                }

                val response = withContext(Dispatchers.IO) {
                    routeService.getPerpsOrders(
                        walletId = walletId,
                        limit = limit,
                        offset = offset,
                    )
                }

                val data = response.data
                if (response.isSuccess && data != null) {
                    Timber.d("Perps orders loaded: ${data.size} items")
                    withContext(Dispatchers.IO) {
                        upsertSyncedOrders(data)
                    }

                    val updated = withContext(Dispatchers.IO) {
                        perpsOrderDao.getOrders(limit, offset)
                    }
                    onSuccess(updated)
                } else {
                    val error = "Failed to load orders: ${response.errorDescription}"
                    Timber.e(error)
                    if (cached.isEmpty()) {
                        onError(error)
                    }
                }
            } catch (e: Exception) {
                val error = "Error loading orders: ${e.message}"
                Timber.e(e, error)

                val cached = withContext(Dispatchers.IO) {
                    perpsOrderDao.getOrders(limit, offset)
                }
                if (cached.isEmpty()) {
                    onError(error)
                }
            }
        }
    }

    suspend fun getOrderFromDb(orderId: String): PerpsOrderItem? {
        return withContext(Dispatchers.IO) {
            perpsOrderDao.getOrder(orderId)
        }
    }

    suspend fun getCloseOrderFromDb(positionId: String): PerpsOrderItem? {
        return withContext(Dispatchers.IO) {
            perpsOrderDao.getCloseOrderByPositionId(positionId)
        }
    }

    private suspend fun upsertSyncedOrders(orders: List<PerpsOrder>) {
        if (orders.isEmpty()) return

        val updatedOrders = orders.map { order ->
            val cachedLeverage = perpsOrderDao.getCachedLeverage(order.positionId)
            if (order.leverage == 0 && cachedLeverage != null) {
                order.copy(leverage = cachedLeverage)
            } else {
                order
            }
        }

        perpsOrderDao.deleteLocalByPositionIds(updatedOrders.map { it.positionId }.distinct())
        perpsOrderDao.insertAll(updatedOrders)

        // Sync position status if it's a close order
        updatedOrders.filter { it.orderType == PerpsOrder.TYPE_CLOSE && it.status == PerpsOrder.STATUS_FILLED }
            .forEach { closeOrder ->
                perpsPositionDao.updateStatus(
                    closeOrder.positionId,
                    "closed",
                    closeOrder.updatedAt,
                )
            }
    }

    private fun createCachedClosedOrder(
        position: PerpsPositionItem,
        leverage: Int,
    ): PerpsOrder {
        val closedAt = position.updatedAt?.takeIf { it.isNotBlank() }
            ?: position.createdAt?.takeIf { it.isNotBlank() }
            ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val entryPrice = position.entryPrice
        val closePrice = position.markPrice?.takeIf { it.isNotBlank() } ?: entryPrice

        return PerpsOrder(
            orderId = "local_${position.positionId}",
            positionId = position.positionId,
            marketId = position.marketId,
            side = position.side,
            orderType = PerpsOrder.TYPE_CLOSE,
            status = PerpsOrder.STATUS_FILLED,
            leverage = leverage,
            quantity = position.quantity,
            entryPrice = entryPrice,
            closePrice = closePrice,
            realizedPnl = position.unrealizedPnl?.takeIf { it.isNotBlank() } ?: "0",
            roe = position.roe ?: "0",
            closeReason = null,
            triggerPrice = null,
            createdAt = position.createdAt?.takeIf { it.isNotBlank() } ?: closedAt,
            updatedAt = closedAt,
        )
    }
}
