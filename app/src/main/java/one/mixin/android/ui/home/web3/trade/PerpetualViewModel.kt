package one.mixin.android.ui.home.web3.trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.api.response.perps.CandleView
import one.mixin.android.api.response.perps.MarketView
import one.mixin.android.api.service.RouteService
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PerpetualViewModel @Inject constructor(
    private val routeService: RouteService
) : ViewModel() {

    fun loadMarkets(
        onSuccess: (List<MarketView>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    routeService.getPerpsMarkets(offset = 0, limit = 100)
                }
                
                val data = response.data
                if (response.isSuccess && data != null) {
                    Timber.d("Perps markets loaded: ${data.size} markets")
                    onSuccess(data)
                } else {
                    val error = "Failed to load markets: ${response.errorDescription}"
                    Timber.e(error)
                    onError(error)
                }
            } catch (e: Exception) {
                val error = "Error loading markets: ${e.message}"
                Timber.e(e, error)
                onError(error)
            }
        }
    }

    fun loadMarketDetail(
        marketId: String,
        onSuccess: (MarketView) -> Unit,
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
}
