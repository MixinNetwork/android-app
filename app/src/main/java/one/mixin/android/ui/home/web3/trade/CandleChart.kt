package one.mixin.android.ui.home.web3.trade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import one.mixin.android.Constants
import one.mixin.android.api.response.perps.CandleView
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandleChart(
    symbol: String,
    timeFrame: String
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<PerpetualViewModel>()
    var candles by remember { mutableStateOf<List<CandleView>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(symbol, timeFrame) {
        isLoading = true
        errorMessage = null
        viewModel.loadCandles(
            symbol = symbol,
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
        modifier = Modifier
            .fillMaxSize(),
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
                ScrollableCandleChart(candles = candles, context = context)
            }
        }
    }
}

@Composable
private fun ScrollableCandleChart(candles: List<CandleView>, context: android.content.Context) {
    val items = candles.firstOrNull()?.items ?: emptyList()
    if (items.isEmpty()) return

    val candleWidth = 6.dp
    val spacing = 2.dp
    val density = LocalDensity.current
    
    val scrollState = rememberScrollState()
    var visibleRange by remember { mutableStateOf(Pair(0, items.size)) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    var isTouching by remember { mutableStateOf(false) }
    
    LaunchedEffect(items.size) {
        if (items.size > 50) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }
    
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (items.isNotEmpty()) {
            val candleWidthPx = with(density) { (candleWidth + spacing).toPx() }
            val containerWidth = with(density) { 200.dp.toPx() }
            
            val startIndex = (scrollState.value / candleWidthPx).toInt().coerceIn(0, items.size - 1)
            val visibleCount = (containerWidth / candleWidthPx).toInt() + 2
            val endIndex = (startIndex + visibleCount).coerceIn(0, items.size)
            
            visibleRange = Pair(startIndex, endIndex)
        }
    }

    val visibleItems = items.subList(
        visibleRange.first.coerceIn(0, items.size),
        visibleRange.second.coerceIn(0, items.size)
    )

    val prices = mutableListOf<BigDecimal>()
    visibleItems.forEach { item ->
        item.high.toBigDecimalOrNull()?.let { prices.add(it) }
        item.low.toBigDecimalOrNull()?.let { prices.add(it) }
    }

    val maxPrice = prices.maxOrNull() ?: BigDecimal.ZERO
    val minPrice = prices.minOrNull() ?: BigDecimal.ZERO
    val avgPrice = (maxPrice + minPrice) / BigDecimal(2)
    
    val lastItem = items.lastOrNull()
    val currentPrice = if (visibleRange.second >= items.size) {
        lastItem?.close?.toBigDecimalOrNull()
    } else {
        null
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            isTouching = true
                            touchPosition = offset
                            tryAwaitRelease()
                            isTouching = false
                            touchPosition = null
                        }
                    )
                }
                .horizontalScroll(scrollState)
        ) {
            PerpsCandleChartCanvas(
                candles = candles,
                context = context,
                candleWidth = candleWidth,
                spacing = spacing,
                visibleRange = visibleRange,
                touchPosition = if (isTouching) touchPosition else null,
                scrollOffset = scrollState.value,
                maxPrice = maxPrice,
                minPrice = minPrice
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentSize()
                .padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
        ) {
            val containerHeight = maxHeight
            
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatPrice(maxPrice),
                    fontSize = 10.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
                Text(
                    text = formatPrice(avgPrice),
                    fontSize = 10.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
                Text(
                    text = formatPrice(minPrice),
                    fontSize = 10.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
            }

            if (currentPrice != null) {
                val priceRange = maxPrice - minPrice
                if (priceRange > BigDecimal.ZERO) {
                    val priceRatio = ((currentPrice - minPrice).toFloat() / priceRange.toFloat()).coerceIn(0f, 1f)
                    val offsetY = containerHeight * (1f - priceRatio)
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(y = offsetY)
                    ) {
                        Text(
                            text = formatPrice(currentPrice),
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF2196F3),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PerpsCandleChartCanvas(
    candles: List<CandleView>,
    context: android.content.Context,
    candleWidth: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp,
    visibleRange: Pair<Int, Int>,
    touchPosition: Offset?,
    scrollOffset: Int,
    maxPrice: BigDecimal,
    minPrice: BigDecimal
) {
    val quoteColorPref = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val textMeasurer = rememberTextMeasurer()
    val upColor = if (quoteColorPref) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val downColor = if (quoteColorPref) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .width(((candleWidth + spacing) * (candles.firstOrNull()?.items?.size ?: 0)))
    ) {
        val items = candles.firstOrNull()?.items ?: emptyList()
        if (items.isEmpty()) return@Canvas

        val height = size.height
        val paddingTop = 8f
        val paddingBottom = 8f
        val paddingLeft = 8f
        val chartHeight = height - paddingTop - paddingBottom

        val candleWidthPx = candleWidth.toPx()
        val spacingPx = spacing.toPx()

        val priceRange = maxPrice - minPrice
        if (priceRange == BigDecimal.ZERO) return@Canvas

        items.forEachIndexed { index, item ->
            val open = item.open.toBigDecimalOrNull() ?: return@forEachIndexed
            val close = item.close.toBigDecimalOrNull() ?: return@forEachIndexed
            val high = item.high.toBigDecimalOrNull() ?: return@forEachIndexed
            val low = item.low.toBigDecimalOrNull() ?: return@forEachIndexed

            val isUp = close >= open
            val color = if (isUp) upColor else downColor

            val x = paddingLeft + index * (candleWidthPx + spacingPx) + candleWidthPx / 2

            val highY = paddingTop + chartHeight - ((high - minPrice).toFloat() / priceRange.toFloat() * chartHeight)
            val lowY = paddingTop + chartHeight - ((low - minPrice).toFloat() / priceRange.toFloat() * chartHeight)
            val openY = paddingTop + chartHeight - ((open - minPrice).toFloat() / priceRange.toFloat() * chartHeight)
            val closeY = paddingTop + chartHeight - ((close - minPrice).toFloat() / priceRange.toFloat() * chartHeight)

            drawLine(
                color = color,
                start = Offset(x, highY),
                end = Offset(x, lowY),
                strokeWidth = 2f
            )

            val top = min(openY, closeY)
            val bottom = max(openY, closeY)
            val bodyHeight = max(bottom - top, 2f)

            drawRoundRect(
                color = color,
                topLeft = Offset(x - candleWidthPx / 2, top),
                size = Size(candleWidthPx, bodyHeight),
                cornerRadius = CornerRadius(1f, 1f)
            )
        }

        val lastItem = items.lastOrNull()
        if (lastItem != null && visibleRange.second >= items.size) {
            val lastClose = lastItem.close.toBigDecimalOrNull()
            if (lastClose != null) {
                drawCurrentPriceLine(
                    price = lastClose,
                    minPrice = minPrice,
                    priceRange = priceRange,
                    paddingTop = paddingTop,
                    chartHeight = chartHeight,
                    paddingLeft = paddingLeft
                )
            }
        }

        touchPosition?.let { touch ->
            val adjustedX = touch.x + scrollOffset
            val candleIndex = ((adjustedX - paddingLeft) / (candleWidthPx + spacingPx)).toInt()
            if (candleIndex in items.indices) {
                val item = items[candleIndex]
                val close = item.close.toBigDecimalOrNull()
                if (close != null) {
                    val priceY = paddingTop + chartHeight - ((close - minPrice).toFloat() / priceRange.toFloat() * chartHeight)
                    drawTouchCrosshair(
                        x = paddingLeft + candleIndex * (candleWidthPx + spacingPx) + candleWidthPx / 2,
                        y = priceY,
                        price = close,
                        width = size.width,
                        paddingTop = paddingTop,
                        paddingBottom = paddingBottom,
                        paddingLeft = paddingLeft,
                        textMeasurer = textMeasurer
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCurrentPriceLine(
    price: BigDecimal,
    minPrice: BigDecimal,
    priceRange: BigDecimal,
    paddingTop: Float,
    chartHeight: Float,
    paddingLeft: Float
) {
    val y = paddingTop + chartHeight - ((price - minPrice).toFloat() / priceRange.toFloat() * chartHeight)
    
    drawLine(
        color = Color(0xFF2196F3),
        start = Offset(paddingLeft, y),
        end = Offset(size.width, y),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
    )
}

private fun DrawScope.drawTouchCrosshair(
    x: Float,
    y: Float,
    price: BigDecimal,
    width: Float,
    paddingTop: Float,
    paddingBottom: Float,
    paddingLeft: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val lineColor = Color(0xFF9E9E9E)
    
    drawLine(
        color = lineColor,
        start = Offset(paddingLeft, y),
        end = Offset(width, y),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
    )
    
    drawLine(
        color = lineColor,
        start = Offset(x, paddingTop),
        end = Offset(x, size.height - paddingBottom),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
    )
    
    val priceText = formatPrice(price)
    val textLayoutResult = textMeasurer.measure(
        text = priceText,
        style = TextStyle(fontSize = 10.sp, color = Color.White)
    )
    
    drawRoundRect(
        color = Color(0xFF2196F3),
        topLeft = Offset(width - textLayoutResult.size.width - 8f, y - textLayoutResult.size.height / 2 - 2f),
        size = Size(textLayoutResult.size.width + 8f, textLayoutResult.size.height + 4f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(width - textLayoutResult.size.width - 4f, y - textLayoutResult.size.height / 2)
    )
}

private fun formatPrice(price: BigDecimal): String {
    return when {
        price >= BigDecimal("100") -> String.format("%.0f", price)
        price >= BigDecimal("1") -> String.format("%.2f", price)
        else -> String.format("%.6f", price)
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? {
    return try {
        BigDecimal(this)
    } catch (e: Exception) {
        null
    }
}
