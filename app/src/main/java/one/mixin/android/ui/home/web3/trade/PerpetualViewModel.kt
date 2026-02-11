package one.mixin.android.ui.home.web3.trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.api.response.perps.CandleView
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.service.RouteService
import one.mixin.android.api.request.perps.OpenOrderRequest
import one.mixin.android.api.request.perps.OpenOrderResponse
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.db.perps.PerpsPositionDao
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.vo.safe.TokenItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PerpetualViewModel @Inject constructor(
    private val routeService: RouteService,
    private val tokenDao: one.mixin.android.db.TokenDao,
    private val perpsPositionDao: PerpsPositionDao,
    private val perpsMarketDao: PerpsMarketDao
) : ViewModel() {

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
                    Timber.d("Market detail loaded: ${data.symbol}")
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
        symbol: String,
        timeFrame: String,
        onSuccess: (List<CandleView>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    routeService.getPerpsCandles(symbol, timeFrame)
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

    fun loadUsdTokens(onSuccess: (List<TokenItem>) -> Unit) {
        viewModelScope.launch {
            try {
                val usdTokens = withContext(Dispatchers.IO) {
                    val usdIds = one.mixin.android.Constants.usdIds
                    tokenDao.findTokenItems(usdIds)
                        .sortedByDescending { 
                            it.balance.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO 
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
        productId: String,
        side: String,
        amount: String,
        leverage: Int,
        walletId: String,
        destination: String? = null,
        marketSymbol: String,
        entryPrice: String,
        liquidationPrice: String,
        onSuccess: (OpenOrderResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val request = OpenOrderRequest(
                    assetId = assetId,
                    productId = productId,
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
                    Timber.d("Perps order opened: ${data.orderId}")
                    
                    val position = PerpsPosition(
                        positionId = data.orderId,
                        walletId = walletId,
                        marketId = productId,
                        marketSymbol = marketSymbol,
                        side = side,
                        quantity = amount,
                        entryPrice = entryPrice,
                        margin = amount,
                        leverage = leverage,
                        state = "open",
                        markPrice = entryPrice,
                        unrealizedPnl = "0",
                        roe = "0",
                        liquidationPrice = liquidationPrice,
                        createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
                        updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
                    )
                    
                    withContext(Dispatchers.IO) {
                        perpsPositionDao.insert(position)
                    }
                    
                    onSuccess(data)
                } else {
                    val error = "Failed to open perps order: ${response.errorDescription}"
                    Timber.e(error)
                    onError(error)
                }
            } catch (e: Exception) {
                val error = "Error opening perps order: ${e.message}"
                Timber.e(e, error)
                onError(error)
            }
        }
    }

    fun getOpenPositions(walletId: String, onSuccess: (List<PerpsPosition>) -> Unit) {
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
}
