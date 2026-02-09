package one.mixin.android.ui.home.web3.trade

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import one.mixin.android.api.response.perps.CandleView
import one.mixin.android.compose.theme.MixinAppTheme
import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandleChart(
    marketId: String,
    timeFrame: String
) {
    val viewModel = hiltViewModel<PerpetualViewModel>()
    var candles by remember { mutableStateOf<List<CandleView>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(marketId, timeFrame) {
        isLoading = true
        errorMessage = null
        viewModel.loadCandles(
            marketId = marketId,
            timeFrame = timeFrame,
            onSuccess = { data ->
                candles = data
                isLoading = false
            },
            onError = { error ->
                errorMessage = error
                isLoading = false
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
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
                    text = errorMessage ?: "Error loading chart",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.red
                )
            }
            candles.isEmpty() -> {
                Text(
                    text = "No data available",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            }
            else -> {
                PerpsCandleChartCanvas(candles = candles)
            }
        }
    }
}

@Composable
private fun PerpsCandleChartCanvas(candles: List<CandleView>) {
    val greenColor = Color(0xFF4CAF50)
    val redColor = Color(0xFFF44336)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val items = candles.firstOrNull()?.items ?: emptyList()
        if (items.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 40f
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        val candleCount = items.size
        if (candleCount == 0) return@Canvas

        val candleWidth = (chartWidth / candleCount) * 0.6f
        val spacing = (chartWidth / candleCount) * 0.4f

        val prices = mutableListOf<BigDecimal>()
        items.forEach { item ->
            item.high.toBigDecimalOrNull()?.let { prices.add(it) }
            item.low.toBigDecimalOrNull()?.let { prices.add(it) }
        }

        if (prices.isEmpty()) return@Canvas

        val maxPrice = prices.maxOrNull() ?: BigDecimal.ZERO
        val minPrice = prices.minOrNull() ?: BigDecimal.ZERO
        val priceRange = maxPrice - minPrice

        if (priceRange == BigDecimal.ZERO) return@Canvas

        items.forEachIndexed { index, item ->
            val open = item.open.toBigDecimalOrNull() ?: return@forEachIndexed
            val close = item.close.toBigDecimalOrNull() ?: return@forEachIndexed
            val high = item.high.toBigDecimalOrNull() ?: return@forEachIndexed
            val low = item.low.toBigDecimalOrNull() ?: return@forEachIndexed

            val isGreen = close >= open
            val color = if (isGreen) greenColor else redColor

            val x = padding + index * (candleWidth + spacing) + candleWidth / 2

            val highY = padding + chartHeight - ((high - minPrice).toFloat() / priceRange.toFloat() * chartHeight)
            val lowY = padding + chartHeight - ((low - minPrice).toFloat() / priceRange.toFloat() * chartHeight)
            val openY = padding + chartHeight - ((open - minPrice).toFloat() / priceRange.toFloat() * chartHeight)
            val closeY = padding + chartHeight - ((close - minPrice).toFloat() / priceRange.toFloat() * chartHeight)

            drawLine(
                color = color,
                start = Offset(x, highY),
                end = Offset(x, lowY),
                strokeWidth = 2f
            )

            val top = min(openY, closeY)
            val bottom = max(openY, closeY)
            val bodyHeight = max(bottom - top, 2f)

            drawRect(
                color = color,
                topLeft = Offset(x - candleWidth / 2, top),
                size = Size(candleWidth, bodyHeight)
            )
        }
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? {
    return try {
        BigDecimal(this)
    } catch (e: Exception) {
        null
    }
}
