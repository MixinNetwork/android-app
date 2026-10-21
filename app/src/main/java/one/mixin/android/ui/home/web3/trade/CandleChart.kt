package one.mixin.android.ui.home.web3.trade

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import one.mixin.android.R
import one.mixin.android.api.response.perps.CandleView
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.web3.trade.perps.PerpetualViewModel

private const val CANDLE_REFRESH_INTERVAL_MS = 5_000L

@Composable
fun CandleChart(
    marketId: String,
    timeFrame: String,
    marketPrice: String? = null,
    priceScale: Int? = null,
) {
    val dataError = stringResource(R.string.Data_error)
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val lifecycleOwner = LocalLifecycleOwner.current
    var candles by remember { mutableStateOf<List<CandleView>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(marketId, timeFrame, lifecycleOwner) {
        candles = emptyList()
        isLoading = true
        errorMessage = null
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                viewModel.loadCandles(
                    marketId = marketId,
                    timeFrame = timeFrame,
                    onSuccess = { data ->
                        candles = data
                        errorMessage = null
                        isLoading = false
                    },
                    onError = { error ->
                        if (candles.isEmpty()) {
                            errorMessage = error
                            isLoading = false
                        }
                    }
                )
                delay(CANDLE_REFRESH_INTERVAL_MS)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MixinAppTheme.colors.accent
                )
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage ?: dataError,
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            }
            candles.firstOrNull()?.items.isNullOrEmpty() -> {
                Text(
                    text = dataError,
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            }
            else -> {
                val candleView = candles.first()
                ViewportCandleChart(
                    items = candleView.items,
                    timeFrame = candleView.timeFrame,
                    marketPriceText = marketPrice,
                    priceScale = priceScale,
                )
            }
        }
    }
}
