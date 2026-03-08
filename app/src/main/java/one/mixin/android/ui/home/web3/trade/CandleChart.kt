package one.mixin.android.ui.home.web3.trade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import one.mixin.android.Constants
import one.mixin.android.api.response.perps.CandleItem
import one.mixin.android.api.response.perps.CandleView
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.home.web3.trade.perps.PerpetualViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val candleView = candles.firstOrNull() ?: return
    val items = candleView.items
    if (items.isEmpty()) return

    val candleWidth = 6.dp
    val spacing = 2.dp
    val density = LocalDensity.current

    val scrollState = rememberScrollState()
    var touchXOnChart by remember { mutableStateOf<Float?>(null) }
    var isTouching by remember { mutableStateOf(false) }

    val candleStepPx = with(density) { (candleWidth + spacing).toPx() }
    val candleWidthPx = with(density) { candleWidth.toPx() }
    val chartStartPaddingPx = with(density) { 8.dp.toPx() }
    val totalChartWidthPx = with(density) {
        (8.dp + (candleWidth * items.size) + (spacing * (items.size - 1).coerceAtLeast(0))).toPx()
    }

    LaunchedEffect(items.size) {
        if (items.size > 50) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    val selectedIndex = touchXOnChart?.let { x ->
        ((x - chartStartPaddingPx) / candleStepPx).toInt().coerceIn(0, items.lastIndex)
    }
    val selectedItem = selectedIndex?.let { index -> items.getOrNull(index) }
    val latestPrice = items.lastOrNull()?.close?.toBigDecimalOrNull()
    val axisPanelWidth = 52.dp

    Row(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clipToBounds()
        ) {
            val axisPanelWidthPx = with(density) { axisPanelWidth.toPx() }
            val viewportWidthPx = (with(density) { maxWidth.toPx() } - axisPanelWidthPx).coerceAtLeast(1f)
            val viewportLeft = scrollState.value.toFloat()
            val viewportRight = viewportLeft + viewportWidthPx
            val startIndex = ((((viewportLeft - chartStartPaddingPx) - candleWidthPx) / candleStepPx).toInt() - 1)
                .coerceAtLeast(0)
            val endIndex = ((((viewportRight - chartStartPaddingPx) + candleWidthPx) / candleStepPx).toInt() + 2)
                .coerceAtMost(items.size)
            val visibleItems = if (startIndex < endIndex) {
                items.subList(startIndex, endIndex)
            } else {
                items
            }

            val prices = mutableListOf<BigDecimal>()
            visibleItems.forEach { item ->
                item.high.toBigDecimalOrNull()?.let { prices.add(it) }
                item.low.toBigDecimalOrNull()?.let { prices.add(it) }
            }
            val maxPrice = prices.maxOrNull() ?: BigDecimal.ZERO
            val minPrice = prices.minOrNull() ?: BigDecimal.ZERO
            val midPrice = (maxPrice + minPrice) / BigDecimal(2)
            val maxPriceText = formatPrice(maxPrice)
            val midPriceText = formatPrice(midPrice)
            val minPriceText = formatPrice(minPrice)

            val selectedPrice = selectedItem?.close?.toBigDecimalOrNull()
            val showCurrentPrice = selectedPrice == null && latestPrice != null
            val currentPriceText = latestPrice?.let { formatPrice(it) }
            val isCurrentPriceInRange = latestPrice?.let { it >= minPrice && it <= maxPrice } == true
            val isCurrentPriceOverlapping = currentPriceText != null &&
                currentPriceText in setOf(maxPriceText, midPriceText, minPriceText)
            val showCurrentPriceLine = showCurrentPrice && isCurrentPriceInRange
            val showCurrentPriceTag = showCurrentPrice && isCurrentPriceInRange && !isCurrentPriceOverlapping

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = axisPanelWidth)
                    .pointerInput(items.size, scrollState.value) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                isTouching = true
                                touchXOnChart = (offset.x + scrollState.value)
                                    .coerceIn(chartStartPaddingPx, max(totalChartWidthPx, chartStartPaddingPx))
                            },
                            onDrag = { change, _ ->
                                touchXOnChart = (change.position.x + scrollState.value)
                                    .coerceIn(chartStartPaddingPx, max(totalChartWidthPx, chartStartPaddingPx))
                                change.consume()
                            },
                            onDragEnd = {
                                isTouching = false
                                touchXOnChart = null
                            },
                            onDragCancel = {
                                isTouching = false
                                touchXOnChart = null
                            }
                        )
                    }
                    .horizontalScroll(scrollState, enabled = !isTouching)
                    .clipToBounds()
            ) {
                PerpsCandleChartCanvas(
                    items = items,
                    timeFrame = candleView.timeFrame,
                    context = context,
                    candleWidth = candleWidth,
                    spacing = spacing,
                    touchXOnChart = if (isTouching) touchXOnChart else null,
                    scrollOffset = scrollState.value.toFloat(),
                    viewportWidth = viewportWidthPx,
                    maxPrice = maxPrice,
                    minPrice = minPrice,
                    showCurrentPriceLine = showCurrentPriceLine,
                    currentPriceForLine = latestPrice,
                    currentPriceLineColor = MixinAppTheme.colors.textPrimary,
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(axisPanelWidth)
                    .padding(start = 2.dp, top = 8.dp, bottom = 8.dp, end = 2.dp)
            ) {
                val containerHeight = maxHeight
                val priceRange = maxPrice - minPrice
                val currentPrice = latestPrice
                val currentPriceRatio = if (priceRange > BigDecimal.ZERO && currentPrice != null) {
                    ((currentPrice - minPrice).toFloat() / priceRange.toFloat()).coerceIn(0f, 1f)
                } else {
                    null
                }

                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = maxPriceText,
                        fontSize = 10.sp,
                        color = MixinAppTheme.colors.textPrimary,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = midPriceText,
                        fontSize = 10.sp,
                        color = MixinAppTheme.colors.textPrimary,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = minPriceText,
                        fontSize = 10.sp,
                        color = MixinAppTheme.colors.textPrimary,
                        textAlign = TextAlign.End
                    )
                }

                if (showCurrentPriceTag && currentPriceRatio != null) {
                    val offsetY = containerHeight * (1f - currentPriceRatio)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = offsetY)
                    ) {
                        Text(
                            text = formatPrice(currentPrice),
                            fontSize = 10.sp,
                            color = MixinAppTheme.colors.textPrimary,
                            modifier = Modifier
                                .background(
                                    color = MixinAppTheme.colors.background,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MixinAppTheme.colors.borderColor,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 2.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PerpsCandleChartCanvas(
    items: List<CandleItem>,
    timeFrame: String,
    context: android.content.Context,
    candleWidth: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp,
    touchXOnChart: Float?,
    scrollOffset: Float,
    viewportWidth: Float,
    maxPrice: BigDecimal,
    minPrice: BigDecimal,
    showCurrentPriceLine: Boolean,
    currentPriceForLine: BigDecimal?,
    currentPriceLineColor: Color,
) {
    val quoteColorPref = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val textMeasurer = rememberTextMeasurer()
    val upColor = if (quoteColorPref) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val downColor = if (quoteColorPref) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .width(8.dp + (candleWidth * items.size) + (spacing * (items.size - 1).coerceAtLeast(0)))
    ) {
        if (items.isEmpty()) return@Canvas

        val height = size.height
        val paddingTop = 8f
        val paddingBottom = 8f
        val paddingLeft = 8f
        val chartHeight = height - paddingTop - paddingBottom

        val candleWidthPx = candleWidth.toPx()
        val spacingPx = spacing.toPx()
        val viewportLeft = scrollOffset
        val viewportRight = scrollOffset + viewportWidth

        val priceRange = maxPrice - minPrice
        if (priceRange == BigDecimal.ZERO) return@Canvas

        clipRect(
            left = viewportLeft,
            top = paddingTop,
            right = viewportRight,
            bottom = size.height - paddingBottom
        ) {
            items.forEachIndexed { index, item ->
                val open = item.open.toBigDecimalOrNull() ?: return@forEachIndexed
                val close = item.close.toBigDecimalOrNull() ?: return@forEachIndexed
                val high = item.high.toBigDecimalOrNull() ?: return@forEachIndexed
                val low = item.low.toBigDecimalOrNull() ?: return@forEachIndexed

                val x = paddingLeft + index * (candleWidthPx + spacingPx) + candleWidthPx / 2
                if (x + candleWidthPx < viewportLeft || x - candleWidthPx > viewportRight) {
                    return@forEachIndexed
                }

                val isUp = close >= open
                val color = if (isUp) upColor else downColor
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
        }

        if (showCurrentPriceLine && currentPriceForLine != null) {
            drawCurrentPriceLine(
                price = currentPriceForLine,
                minPrice = minPrice,
                priceRange = priceRange,
                paddingTop = paddingTop,
                chartHeight = chartHeight,
                paddingLeft = paddingLeft,
                viewportLeft = viewportLeft,
                viewportRight = viewportRight,
                paddingBottom = paddingBottom,
                lineColor = currentPriceLineColor,
            )
        }

        touchXOnChart?.let { selectedX ->
            val candleIndex = ((selectedX - paddingLeft) / (candleWidthPx + spacingPx)).toInt()
            if (candleIndex in items.indices) {
                val item = items[candleIndex]
                val close = item.close.toBigDecimalOrNull()
                if (close != null) {
                    val priceY = paddingTop + chartHeight - ((close - minPrice).toFloat() / priceRange.toFloat() * chartHeight)
                    drawTouchCrosshair(
                        x = paddingLeft + candleIndex * (candleWidthPx + spacingPx) + candleWidthPx / 2,
                        y = priceY,
                        price = close,
                        timeText = formatCandleTime(item.timestamp, timeFrame),
                        paddingTop = paddingTop,
                        paddingBottom = paddingBottom,
                        paddingLeft = paddingLeft,
                        textMeasurer = textMeasurer,
                        viewportLeft = viewportLeft,
                        viewportRight = viewportRight,
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
    paddingLeft: Float,
    viewportLeft: Float,
    viewportRight: Float,
    paddingBottom: Float,
    lineColor: Color,
) {
    val y = (paddingTop + chartHeight - ((price - minPrice).toFloat() / priceRange.toFloat() * chartHeight))
        .coerceIn(paddingTop, size.height - paddingBottom)

    drawLine(
        color = lineColor,
        start = Offset(max(paddingLeft, viewportLeft), y),
        end = Offset(min(size.width, viewportRight), y),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
    )
}

private fun DrawScope.drawTouchCrosshair(
    x: Float,
    y: Float,
    price: BigDecimal,
    timeText: String,
    paddingTop: Float,
    paddingBottom: Float,
    paddingLeft: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    viewportLeft: Float,
    viewportRight: Float,
) {
    val lineColor = Color(0xFF9E9E9E)

    drawLine(
        color = lineColor,
        start = Offset(paddingLeft, y),
        end = Offset(size.width, y),
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
    val priceTextLayout = textMeasurer.measure(
        text = priceText,
        style = TextStyle(fontSize = 11.sp, color = Color.White)
    )
    val priceTagHorizontalPadding = 10f
    val priceTagVerticalPadding = 5f
    val priceTagWidth = priceTextLayout.size.width + priceTagHorizontalPadding * 2
    val priceTagHeight = priceTextLayout.size.height + priceTagVerticalPadding * 2
    val priceTagMinX = viewportLeft + 4f
    val priceTagMaxX = max(priceTagMinX, viewportRight - priceTagWidth - 4f)
    val priceTagX = (viewportRight - priceTagWidth - 4f).coerceIn(priceTagMinX, priceTagMaxX)
    val priceTagY = (y - priceTagHeight / 2).coerceIn(paddingTop, size.height - paddingBottom - priceTagHeight)

    drawRoundRect(
        color = Color(0xFF1F2533),
        topLeft = Offset(priceTagX, priceTagY),
        size = Size(priceTagWidth, priceTagHeight),
        cornerRadius = CornerRadius(12f, 12f)
    )

    drawText(
        textLayoutResult = priceTextLayout,
        topLeft = Offset(priceTagX + priceTagHorizontalPadding, priceTagY + priceTagVerticalPadding)
    )

    val timeTextLayout = textMeasurer.measure(
        text = timeText,
        style = TextStyle(fontSize = 11.sp, color = Color.White)
    )
    val timeTagHorizontalPadding = 10f
    val timeTagVerticalPadding = 5f
    val timeTagWidth = timeTextLayout.size.width + timeTagHorizontalPadding * 2
    val timeTagHeight = timeTextLayout.size.height + timeTagVerticalPadding * 2
    val timeTagMinX = viewportLeft + 4f
    val timeTagMaxX = max(timeTagMinX, viewportRight - timeTagWidth - 4f)
    val timeTagX = (x - timeTagWidth / 2).coerceIn(timeTagMinX, timeTagMaxX)
    val timeTagY = paddingTop + 2f

    drawRoundRect(
        color = Color(0xFF1F2533),
        topLeft = Offset(timeTagX, timeTagY),
        size = Size(timeTagWidth, timeTagHeight),
        cornerRadius = CornerRadius(12f, 12f)
    )

    drawText(
        textLayoutResult = timeTextLayout,
        topLeft = Offset(timeTagX + timeTagHorizontalPadding, timeTagY + timeTagVerticalPadding)
    )
}

private fun formatPrice(price: BigDecimal): String {
    return when {
        price >= BigDecimal("100") -> String.format("%.0f", price)
        price >= BigDecimal("1") -> String.format("%.2f", price)
        else -> String.format("%.6f", price)
    }
}

private fun formatCandleTime(timestamp: Long, timeFrame: String): String {
    val millis = if (timestamp < 1_000_000_000_000L) timestamp * 1000 else timestamp
    val pattern = when (timeFrame.lowercase()) {
        "1h" -> "MM-dd HH:mm"
        "1d" -> "yyyy-MM-dd HH:mm"
        "1w" -> "yyyy-MM-dd"
        "1m" -> "yyyy-MM"
        else -> "MM-dd HH:mm"
    }
    return runCatching {
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }.getOrDefault("--")
}

private fun String.toBigDecimalOrNull(): BigDecimal? {
    return try {
        BigDecimal(this)
    } catch (e: Exception) {
        null
    }
}
