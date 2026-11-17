package one.mixin.android.ui.home.web3.trade

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.CandleView
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.home.web3.trade.perps.PerpetualViewModel
import java.math.BigDecimal

private const val CANDLE_REFRESH_INTERVAL_MS = 5_000L

// What the chart is showing under the finger while it is being scrubbed, so the header can follow
// it. The change is measured against the first candle of the series, the way the reference does it.
data class ChartSelection(
    val price: String,
    val changePercent: BigDecimal,
)

internal class PerpsChartStyle(
    val useTradingView: Boolean,
    val lineMode: Boolean,
    val setLineMode: (Boolean) -> Unit,
)

@Composable
internal fun rememberPerpsChartStyle(): PerpsChartStyle {
    val preferences = LocalContext.current.defaultSharedPreferences
    var useTradingView by remember {
        mutableStateOf(preferences.getBoolean(Constants.Account.PREF_USE_TRADING_VIEW_CANDLES, false))
    }
    var lineMode by remember {
        mutableStateOf(preferences.getBoolean(Constants.Account.PREF_PERPS_LINE_CHART, false))
    }

    DisposableEffect(preferences) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == Constants.Account.PREF_USE_TRADING_VIEW_CANDLES) {
                    useTradingView = sharedPreferences.getBoolean(key, false)
                }
                if (key == Constants.Account.PREF_PERPS_LINE_CHART) {
                    lineMode = sharedPreferences.getBoolean(key, false)
                }
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // Move the state as well as the preference so the tap lands on this frame; the listener above
    // is for changes made anywhere else, such as the debug screen.
    val setLineMode: (Boolean) -> Unit = { next ->
        lineMode = next
        preferences.edit()
            .putBoolean(Constants.Account.PREF_PERPS_LINE_CHART, next)
            .apply()
    }
    return remember(preferences, useTradingView, lineMode) {
        PerpsChartStyle(
            useTradingView = useTradingView,
            lineMode = lineMode,
            setLineMode = setLineMode,
        )
    }
}

@Composable
fun CandleChart(
    marketId: String,
    timeFrame: String,
    marketPrice: String? = null,
    priceScale: Int? = null,
    interactive: Boolean = true,
    onLoading: (Boolean) -> Unit = {},
    useTradingView: Boolean = false,
    lineMode: Boolean = false,
    trendUp: Boolean = true,
    onSelectionChange: (ChartSelection?) -> Unit = {},
) {
    val dataError = stringResource(R.string.Data_error)
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val lifecycleOwner = LocalLifecycleOwner.current
    var candles by remember { mutableStateOf<List<CandleView>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isLoading) {
        onLoading(isLoading)
    }

    LaunchedEffect(marketId, timeFrame, useTradingView) {
        onSelectionChange(null)
    }

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
                if (useTradingView && interactive && priceScale != null) {
                    TradingViewCandleChart(
                        candles = candles,
                        marketPrice = marketPrice,
                        priceScale = priceScale,
                        timeFrame = timeFrame,
                        lineMode = lineMode,
                        trendUp = trendUp,
                        onSelectionChange = onSelectionChange,
                    )
                } else {
                    ViewportCandleChart(
                        items = candleView.items,
                        timeFrame = candleView.timeFrame,
                        marketPriceText = marketPrice,
                        priceScale = priceScale,
                        interactive = interactive,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ChartStyleToggle(
    lineMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            painter =
                painterResource(
                    if (lineMode) R.drawable.ic_chart_candle else R.drawable.ic_chart_line,
                ),
            contentDescription =
                stringResource(
                    if (lineMode) {
                        R.string.perps_chart_style_candle
                    } else {
                        R.string.perps_chart_style_line
                    },
                ),
            tint = if (lineMode) Color.Unspecified else MixinAppTheme.colors.textAssist,
            modifier = Modifier.size(24.dp),
        )
    }
}
