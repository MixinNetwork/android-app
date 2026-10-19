package one.mixin.android.ui.home.web3.trade

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.CandleItem
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.heavyClickVibrate
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private const val DEFAULT_VISIBLE_CANDLES = 30f
private const val MIN_VISIBLE_CANDLES = 10f
private const val MAX_VISIBLE_CANDLES = 200f

// Empty slots kept to the right of the latest candle, in candle widths.
private const val RIGHT_PADDING_CANDLES = 0.3f
private const val GRID_LINE_COUNT = 4
private const val BODY_WIDTH_RATIO = 0.7f
private const val WICK_WIDTH_RATIO = 0.3f
private const val LONG_PRESS_TIMEOUT_MS = 300L

@Immutable
internal data class ChartCandle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
)

@Immutable
private data class PriceRange(val min: Double, val max: Double)

/**
 * Viewport over a candle series. Positions are in candle units: candle `i` occupies
 * `[i, i + 1)`, and [end] is the position at the right edge of the plot.
 */
@Stable
internal class CandleChartState {
    var candles by mutableStateOf<List<ChartCandle>>(emptyList())
        private set
    var visibleCount by mutableFloatStateOf(DEFAULT_VISIBLE_CANDLES)
        private set
    var end by mutableFloatStateOf(0f)
        private set
    var selectedIndex by mutableStateOf<Int?>(null)
        private set

    /** True while a one-finger drag or its fling is moving the chart; the price axis holds still. */
    var isScrolling by mutableStateOf(false)
        private set

    private var followLatest = true
    private var flingJob: Job? = null
    internal var plotWidthPx = 1f

    private val maxEnd get() = candles.size + RIGHT_PADDING_CANDLES
    private val minEnd get() = min(visibleCount, maxEnd)
    private val maxVisibleCount get() = max(MIN_VISIBLE_CANDLES, min(MAX_VISIBLE_CANDLES, candles.size.toFloat()))
    private val step get() = plotWidthPx / visibleCount

    fun updateCandles(newCandles: List<ChartCandle>) {
        val old = candles
        val selectedTimestamp = selectedIndex?.let { old.getOrNull(it)?.timestamp }
        candles = newCandles
        visibleCount = visibleCount.coerceIn(MIN_VISIBLE_CANDLES, maxVisibleCount)
        end = if (followLatest || old.isEmpty()) {
            maxEnd
        } else {
            // Keep the same candles on screen when the window slides or grows.
            val anchor = old.last().timestamp
            val newIndex = newCandles.indexOfLast { it.timestamp == anchor }
            val shift = if (newIndex >= 0) newIndex - old.lastIndex else 0
            (end + shift).coerceIn(minEnd, maxEnd)
        }
        selectedIndex = selectedTimestamp?.let { timestamp ->
            newCandles.indexOfFirst { it.timestamp == timestamp }.takeIf { it >= 0 }
        }
    }

    fun xOf(position: Float): Float = plotWidthPx - (end - position) * step

    fun indexAt(x: Float): Int? {
        if (candles.isEmpty()) return null
        val position = end - (plotWidthPx - x) / step
        return floor(position).toInt().coerceIn(0, candles.lastIndex)
    }

    fun visibleIndices(): IntRange {
        if (candles.isEmpty()) return IntRange.EMPTY
        val first = floor(end - visibleCount).toInt().coerceAtLeast(0)
        val last = (ceil(end).toInt() - 1).coerceAtMost(candles.lastIndex)
        return first..last
    }

    /** Moves the candles by [deltaPx] with the finger: positive (rightward) reveals older candles. Returns false at a bound. */
    fun scrollByPx(deltaPx: Float): Boolean {
        val newEnd = (end - deltaPx / step).coerceIn(minEnd, maxEnd)
        val moved = newEnd != end
        end = newEnd
        followLatest = end >= maxEnd - 0.5f
        return moved
    }

    /** Zooms by [factor] (> 1 zooms in) keeping the position under [anchorX] fixed. */
    fun zoom(factor: Float, anchorX: Float) {
        val anchorPosition = end - (plotWidthPx - anchorX) / step
        val newCount = (visibleCount / factor).coerceIn(MIN_VISIBLE_CANDLES, maxVisibleCount)
        if (newCount == visibleCount) return
        visibleCount = newCount
        end = if (followLatest) {
            maxEnd
        } else {
            (anchorPosition + (plotWidthPx - anchorX) / step).coerceIn(minEnd, maxEnd)
        }
    }

    /** Selects the candle under [x]; returns true when the selection changed. */
    fun select(x: Float): Boolean {
        val index = indexAt(x)
        if (index == selectedIndex) return false
        selectedIndex = index
        return true
    }

    fun clearSelection() {
        selectedIndex = null
    }

    fun startScroll() {
        isScrolling = true
    }

    fun fling(scope: CoroutineScope, velocityPx: Float, decay: DecayAnimationSpec<Float>) {
        stopFling()
        isScrolling = true
        flingJob = scope.launch {
            try {
                var last = 0f
                AnimationState(initialValue = 0f, initialVelocity = velocityPx).animateDecay(decay) {
                    val delta = value - last
                    last = value
                    if (!scrollByPx(delta)) cancelAnimation()
                }
            } finally {
                isScrolling = false
            }
        }
    }

    fun stopFling() {
        flingJob?.cancel()
        flingJob = null
        isScrolling = false
    }
}

/**
 * Candles refresh less often than the market price, so fold the live price into the
 * current candle. Skipped once that candle's period is over, since the live price then
 * belongs to a candle the next refresh will add.
 */
private fun List<ChartCandle>.withLivePrice(price: Double?, timeFrame: String): List<ChartCandle> {
    val last = lastOrNull() ?: return this
    if (price == null) return this
    val period = timeFrameMillis(timeFrame) ?: return this
    val start = if (last.timestamp < 1_000_000_000_000L) last.timestamp * 1000 else last.timestamp
    if (System.currentTimeMillis() >= start + period) return this
    return dropLast(1) + last.copy(close = price, high = max(last.high, price), low = min(last.low, price))
}

private fun timeFrameMillis(timeFrame: String): Long? {
    val value = timeFrame.dropLast(1).toLongOrNull() ?: return null
    val unit = when (timeFrame.last().lowercaseChar()) {
        'm' -> 60_000L
        'h' -> 3_600_000L
        'd' -> 86_400_000L
        'w' -> 604_800_000L
        else -> return null
    }
    return value * unit
}

internal fun List<CandleItem>.toChartCandles(): List<ChartCandle> = mapNotNull { item ->
    ChartCandle(
        timestamp = item.timestamp,
        open = item.open.toDoubleOrNull() ?: return@mapNotNull null,
        high = item.high.toDoubleOrNull() ?: return@mapNotNull null,
        low = item.low.toDoubleOrNull() ?: return@mapNotNull null,
        close = item.close.toDoubleOrNull() ?: return@mapNotNull null,
    )
}

@Composable
internal fun ViewportCandleChart(
    items: List<CandleItem>,
    timeFrame: String,
    marketPriceText: String?,
    priceScale: Int?,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val decay = rememberSplineBasedDecay<Float>()
    val textMeasurer = rememberTextMeasurer()
    val state = remember { CandleChartState() }

    LaunchedEffect(items, marketPriceText, timeFrame) {
        val livePrice = marketPriceText?.toDoubleOrNull()
        state.updateCandles(items.toChartCandles().withLivePrice(livePrice, timeFrame))
    }

    val candles = state.candles
    if (candles.isEmpty()) return

    val priceDecimals = remember(items, priceScale) {
        priceScale?.coerceIn(0, 18)
            ?: items.maxOfOrNull { it.close.toBigDecimalOrNull()?.stripTrailingZeros()?.scale() ?: 0 }?.coerceIn(0, 10)
            ?: 2
    }
    val latestPrice = marketPriceText?.toDoubleOrNull() ?: candles.last().close
    val latestPriceText = formatPrice(latestPrice, priceDecimals)

    val visible = state.visibleIndices()
    val targetRange = visiblePriceRange(candles, visible, latestPrice)
    val range = animatedPriceRange(targetRange, hold = state.isScrolling)
    // Size the axis from all candles so scrolling never changes the plot width (and candle width).
    val axisTexts = remember(candles, latestPriceText, priceDecimals) {
        listOf(
            formatPrice(candles.maxOf { it.high }, priceDecimals),
            formatPrice(candles.minOf { it.low }, priceDecimals),
            latestPriceText,
        )
    }

    val quoteColorPref = context.defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val colors = ChartColors(
        up = if (quoteColorPref) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen,
        down = if (quoteColorPref) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed,
        grid = MixinAppTheme.colors.textAssist.copy(alpha = 0.3f),
        label = MixinAppTheme.colors.textAssist,
        crosshair = MixinAppTheme.colors.textAssist.copy(alpha = 0.7f),
        cardBackground = Color(0xF03A3A3C),
        cardLabel = Color(0xFFAEAEB2),
        cardValue = Color.White,
    )
    val labels = CandleLabels(
        open = stringResource(R.string.Candle_Open),
        close = stringResource(R.string.Candle_Close),
        high = stringResource(R.string.Candle_High),
        low = stringResource(R.string.Candle_Low),
        date = stringResource(R.string.Date),
    )
    val labelStyle = TextStyle(fontSize = 11.sp, color = colors.label)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val axisWidthPx = with(density) {
            val widest = axisTexts.maxOf { textMeasurer.measure(it, labelStyle).size.width }
            max(widest + 16.dp.toPx(), 48.dp.toPx())
        }
        state.plotWidthPx = (constraints.maxWidth - axisWidthPx).coerceAtLeast(1f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state) {
                    detectChartGestures(
                        state = state,
                        scope = scope,
                        decay = decay,
                        onLongPress = context::heavyClickVibrate,
                        onSelectionMove = context::clickVibrate,
                    )
                },
        ) {
            drawChart(
                state = state,
                range = range,
                timeFrame = timeFrame,
                priceDecimals = priceDecimals,
                latestPrice = latestPrice,
                latestPriceText = latestPriceText,
                colors = colors,
                labels = labels,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
            )
        }
    }
}

@Immutable
private data class ChartColors(
    val up: Color,
    val down: Color,
    val grid: Color,
    val label: Color,
    val crosshair: Color,
    val cardBackground: Color,
    val cardLabel: Color,
    val cardValue: Color,
)

@Immutable
private data class CandleLabels(
    val open: String,
    val close: String,
    val high: String,
    val low: String,
    val date: String,
)

private fun visiblePriceRange(candles: List<ChartCandle>, visible: IntRange, latestPrice: Double): PriceRange {
    var low = Double.MAX_VALUE
    var high = -Double.MAX_VALUE
    for (i in visible) {
        low = min(low, candles[i].low)
        high = max(high, candles[i].high)
    }
    // Only stretch the axis to the live price while the latest candle is on screen.
    if (visible.last == candles.lastIndex) {
        low = min(low, latestPrice)
        high = max(high, latestPrice)
    }
    if (low > high) return PriceRange(0.0, 1.0)
    if (high == low) {
        val pad = max(abs(high) * 0.001, 1e-8)
        return PriceRange(low - pad, high + pad)
    }
    return PriceRange(low, high)
}

@Composable
private fun animatedPriceRange(target: PriceRange, hold: Boolean): PriceRange {
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }
    // While held, keep the current range; once released, animate to the latest target.
    LaunchedEffect(target, hold) {
        if (hold || (target == to && progress.value == 1f)) return@LaunchedEffect
        from = lerp(from, to, progress.value)
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
    }
    return lerp(from, to, progress.value)
}

private fun lerp(from: PriceRange, to: PriceRange, fraction: Float): PriceRange = PriceRange(
    min = from.min + (to.min - from.min) * fraction,
    max = from.max + (to.max - from.max) * fraction,
)

private enum class GestureMode { Pending, Drag, Pinch, Select }

private suspend fun PointerInputScope.detectChartGestures(
    state: CandleChartState,
    scope: CoroutineScope,
    decay: DecayAnimationSpec<Float>,
    onLongPress: () -> Unit,
    onSelectionMove: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        state.stopFling()
        val tracker = VelocityTracker().apply { addPointerInputChange(down) }
        val longPressDeadline = down.uptimeMillis + min(viewConfiguration.longPressTimeoutMillis, LONG_PRESS_TIMEOUT_MS)
        var mode = GestureMode.Pending
        var lastUptime = down.uptimeMillis
        var lastPosition = down.position
        var totalDrag = Offset.Zero

        while (true) {
            val event = if (mode == GestureMode.Pending) {
                withTimeoutOrNull((longPressDeadline - lastUptime).coerceAtLeast(0L)) { awaitPointerEvent() }
            } else {
                awaitPointerEvent()
            }
            if (event == null) {
                // Long press: scrub through candles; the chart itself stays put until release.
                mode = GestureMode.Select
                state.stopFling()
                state.select(lastPosition.x)
                onLongPress()
                continue
            }

            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) {
                when (mode) {
                    GestureMode.Drag -> state.fling(scope, tracker.calculateVelocity().x, decay)
                    // The detail card is only shown while the finger is held down.
                    GestureMode.Select -> state.clearSelection()
                    GestureMode.Pending, GestureMode.Pinch -> Unit
                }
                break
            }
            val primary = pressed.first()
            lastUptime = primary.uptimeMillis
            lastPosition = primary.position

            when (mode) {
                GestureMode.Pending -> {
                    // A parent (the page's vertical scroll) claimed the gesture.
                    if (event.changes.any { it.isConsumed }) break
                    if (pressed.size > 1) {
                        mode = GestureMode.Pinch
                        state.clearSelection()
                    } else {
                        totalDrag += primary.positionChange()
                        if (totalDrag.getDistance() > viewConfiguration.touchSlop) {
                            if (abs(totalDrag.x) < abs(totalDrag.y)) break
                            mode = GestureMode.Drag
                            state.clearSelection()
                            state.startScroll()
                            tracker.addPointerInputChange(primary)
                            primary.consume()
                        }
                    }
                }
                GestureMode.Drag -> {
                    if (pressed.size > 1) {
                        mode = GestureMode.Pinch
                        state.stopFling()
                    } else {
                        tracker.addPointerInputChange(primary)
                        state.scrollByPx(primary.positionChange().x)
                        primary.consume()
                    }
                }
                GestureMode.Pinch -> {
                    val zoom = event.pressedZoom()
                    if (zoom != 1f) state.zoom(zoom, event.pressedCentroid().x)
                    state.scrollByPx(event.pressedPan().x)
                    pressed.forEach { it.consume() }
                }
                GestureMode.Select -> {
                    if (state.select(primary.position.x)) onSelectionMove()
                    pressed.forEach { it.consume() }
                }
            }
        }
    }
}

private fun PointerEvent.pressedChanges(): List<PointerInputChange> = changes.filter { it.pressed }

private fun PointerEvent.pressedCentroid(): Offset {
    val pressed = pressedChanges()
    if (pressed.isEmpty()) return Offset.Zero
    return Offset(
        pressed.sumOf { it.position.x.toDouble() }.toFloat() / pressed.size,
        pressed.sumOf { it.position.y.toDouble() }.toFloat() / pressed.size,
    )
}

private fun PointerEvent.previousPressedCentroid(): Offset {
    val pressed = pressedChanges()
    if (pressed.isEmpty()) return Offset.Zero
    return Offset(
        pressed.sumOf { it.previousPosition.x.toDouble() }.toFloat() / pressed.size,
        pressed.sumOf { it.previousPosition.y.toDouble() }.toFloat() / pressed.size,
    )
}

private fun PointerEvent.pressedPan(): Offset = pressedCentroid() - previousPressedCentroid()

private fun PointerEvent.pressedZoom(): Float {
    val pressed = pressedChanges()
    if (pressed.size < 2) return 1f
    val current = pressedCentroid()
    val previous = previousPressedCentroid()
    val currentSpan = pressed.map { hypot(it.position.x - current.x, it.position.y - current.y) }.average()
    val previousSpan = pressed.map { hypot(it.previousPosition.x - previous.x, it.previousPosition.y - previous.y) }.average()
    return if (previousSpan > 0) (currentSpan / previousSpan).toFloat() else 1f
}

private fun DrawScope.drawChart(
    state: CandleChartState,
    range: PriceRange,
    timeFrame: String,
    priceDecimals: Int,
    latestPrice: Double,
    latestPriceText: String,
    colors: ChartColors,
    labels: CandleLabels,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    val candles = state.candles
    val plotWidth = state.plotWidthPx
    val plotTop = 8.dp.toPx()
    val plotBottom = size.height - 24.dp.toPx()
    val plotHeight = plotBottom - plotTop
    val axisTextX = plotWidth + 8.dp.toPx()
    val priceSpan = range.max - range.min
    fun yOf(price: Double): Float = plotBottom - ((price - range.min) / priceSpan * plotHeight).toFloat()

    // Current price line and tag, drawn first so the grid labels can avoid the tag.
    val latestColor = if (latestPrice >= candles.last().open) colors.up else colors.down
    val latestY = yOf(latestPrice)
    val showLatest = latestY in plotTop..plotBottom
    val latestLayout = textMeasurer.measure(latestPriceText, labelStyle.copy(color = latestColor))
    val tagPadH = 6.dp.toPx()
    val tagPadV = 2.dp.toPx()
    val latestTagHeight = latestLayout.size.height + tagPadV * 2

    // Grid lines with price labels on the right.
    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
    for (i in 0 until GRID_LINE_COUNT) {
        val fraction = i.toFloat() / (GRID_LINE_COUNT - 1)
        val y = plotTop + plotHeight * fraction
        drawLine(colors.grid, Offset(0f, y), Offset(plotWidth, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
        val price = range.max - priceSpan * fraction
        val layout = textMeasurer.measure(formatPrice(price, priceDecimals), labelStyle)
        val labelHeight = layout.size.height.toFloat()
        var labelY = y
        if (showLatest) {
            // Nudge the label off the current price tag instead of hiding it.
            val minGap = (latestTagHeight + labelHeight) / 2 + 2.dp.toPx()
            if (abs(labelY - latestY) < minGap) {
                labelY = if (labelY < latestY) latestY - minGap else latestY + minGap
            }
        }
        if (labelY - labelHeight / 2 < 0f || labelY + labelHeight / 2 > size.height) continue
        drawText(layout, topLeft = Offset(axisTextX, labelY - labelHeight / 2))
    }

    // Candles.
    val step = plotWidth / state.visibleCount
    val bodyWidth = step * BODY_WIDTH_RATIO
    val wickWidth = (bodyWidth * WICK_WIDTH_RATIO).coerceIn(1.5.dp.toPx(), 3.dp.toPx())
    val visible = state.visibleIndices()
    clipRect(left = 0f, top = plotTop, right = plotWidth, bottom = plotBottom) {
        for (i in visible) {
            val candle = candles[i]
            val color = if (candle.close >= candle.open) colors.up else colors.down
            val centerX = state.xOf(i + 0.5f)
            val highY = yOf(candle.high)
            val lowY = yOf(candle.low)
            drawRoundRect(
                color = color,
                topLeft = Offset(centerX - wickWidth / 2, highY),
                size = Size(wickWidth, max(lowY - highY, wickWidth)),
                cornerRadius = CornerRadius(wickWidth / 2),
            )
            val bodyTop = min(yOf(candle.open), yOf(candle.close))
            val bodyHeight = max(abs(yOf(candle.open) - yOf(candle.close)), wickWidth)
            drawRoundRect(
                color = color,
                topLeft = Offset(centerX - bodyWidth / 2, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(min(bodyWidth / 3, bodyHeight / 2)),
            )
        }
    }

    if (showLatest) {
        drawLine(latestColor.copy(alpha = 0.5f), Offset(0f, latestY), Offset(plotWidth, latestY), strokeWidth = 1.dp.toPx())
        drawTag(latestLayout, Offset(axisTextX - tagPadH, latestY - latestTagHeight / 2), tagPadH, tagPadV, latestColor.copy(alpha = 0.12f))
    }

    // First and last visible times.
    val timeY = plotBottom + 6.dp.toPx()
    val first = visible.first.coerceAtLeast(ceil(state.end - state.visibleCount).toInt()).coerceAtMost(visible.last)
    val startLayout = textMeasurer.measure(formatCandleTime(candles[first].timestamp, timeFrame), labelStyle)
    val endLayout = textMeasurer.measure(formatCandleTime(candles[visible.last].timestamp, timeFrame), labelStyle)
    drawText(startLayout, topLeft = Offset(0f, timeY))
    drawText(endLayout, topLeft = Offset(max(startLayout.size.width + 8.dp.toPx(), plotWidth - endLayout.size.width), timeY))

    val selectedIndex = state.selectedIndex ?: return
    val selected = candles.getOrNull(selectedIndex) ?: return
    drawSelection(
        selected = selected,
        centerX = state.xOf(selectedIndex + 0.5f),
        closeY = yOf(selected.close).coerceIn(plotTop, plotBottom),
        plotWidth = plotWidth,
        plotTop = plotTop,
        plotBottom = plotBottom,
        priceDecimals = priceDecimals,
        timeFrame = timeFrame,
        colors = colors,
        labels = labels,
        textMeasurer = textMeasurer,
    )
}

private fun DrawScope.drawSelection(
    selected: ChartCandle,
    centerX: Float,
    closeY: Float,
    plotWidth: Float,
    plotTop: Float,
    plotBottom: Float,
    priceDecimals: Int,
    timeFrame: String,
    colors: ChartColors,
    labels: CandleLabels,
    textMeasurer: TextMeasurer,
) {
    drawLine(colors.crosshair, Offset(centerX, plotTop), Offset(centerX, plotBottom), strokeWidth = 1.dp.toPx())
    drawLine(colors.crosshair, Offset(0f, closeY), Offset(plotWidth, closeY), strokeWidth = 1.dp.toPx())

    // Detail card beside the crosshair, on the side with more room.
    val labelStyle = TextStyle(fontSize = 11.sp, color = colors.cardLabel)
    val valueStyle = TextStyle(fontSize = 11.sp, color = colors.cardValue)
    val change = if (selected.open != 0.0) (selected.close - selected.open) / selected.open * 100 else 0.0
    val changeColor = if (selected.close >= selected.open) colors.up else colors.down
    val changeText = (if (change >= 0) "\u2191" else "\u2193") +
        BigDecimal(abs(change)).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%"
    val rows = listOf(
        labels.open to formatPrice(selected.open, priceDecimals),
        labels.close to formatPrice(selected.close, priceDecimals),
        labels.high to formatPrice(selected.high, priceDecimals),
        labels.low to formatPrice(selected.low, priceDecimals),
        labels.date to formatCardDate(selected.timestamp, timeFrame),
    ).map { (label, value) -> textMeasurer.measure(label, labelStyle) to textMeasurer.measure(value, valueStyle) }
    val changeLayout = textMeasurer.measure(changeText, valueStyle.copy(color = changeColor))

    val padH = 8.dp.toPx()
    val padV = 6.dp.toPx()
    val columnGap = 8.dp.toPx()
    val changeGap = 4.dp.toPx()
    val rowGap = 1.dp.toPx()
    val labelWidth = rows.maxOf { it.first.size.width }.toFloat()
    val valueWidth = rows.mapIndexed { i, (_, value) ->
        value.size.width + if (i == 1) changeGap + changeLayout.size.width else 0f
    }.maxOf { it.toFloat() }
    val rowHeight = rows.maxOf { max(it.first.size.height, it.second.size.height) }.toFloat()
    val cardSize = Size(
        padH * 2 + labelWidth + columnGap + valueWidth,
        padV * 2 + rowHeight * rows.size + rowGap * (rows.size - 1),
    )
    // Place the card fully left or right of the vertical crosshair, never on top of it.
    val gap = 8.dp.toPx()
    val leftX = centerX - gap - cardSize.width
    val rightX = centerX + gap
    val fitsLeft = leftX >= 0f
    val fitsRight = rightX + cardSize.width <= size.width
    val preferLeft = centerX > plotWidth / 2
    val cardX = when {
        preferLeft && fitsLeft -> leftX
        !preferLeft && fitsRight -> rightX
        fitsLeft -> leftX
        fitsRight -> rightX
        // Too wide for either side: pin it to the edge farther from the finger.
        preferLeft -> 0f
        else -> max(0f, size.width - cardSize.width)
    }
    val cardY = plotTop
    drawRoundRect(colors.cardBackground, topLeft = Offset(cardX, cardY), size = cardSize, cornerRadius = CornerRadius(8.dp.toPx()))

    rows.forEachIndexed { i, (label, value) ->
        val y = cardY + padV + i * (rowHeight + rowGap)
        drawText(label, topLeft = Offset(cardX + padH + labelWidth - label.size.width, y))
        val valueX = cardX + padH + labelWidth + columnGap
        drawText(value, topLeft = Offset(valueX, y))
        if (i == 1) drawText(changeLayout, topLeft = Offset(valueX + value.size.width + changeGap, y))
    }
}

private fun DrawScope.drawTag(
    layout: androidx.compose.ui.text.TextLayoutResult,
    topLeft: Offset,
    padH: Float,
    padV: Float,
    background: Color,
) {
    val tagSize = Size(layout.size.width + padH * 2, layout.size.height + padV * 2)
    drawRoundRect(background, topLeft = topLeft, size = tagSize, cornerRadius = CornerRadius(tagSize.height / 2))
    drawText(layout, topLeft = Offset(topLeft.x + padH, topLeft.y + padV))
}

// Same fixed-scale, locale-grouped format as formatPerpsPrice, so the chart matches the rest of the page.
private fun formatPrice(price: Double, decimals: Int): String =
    runCatching {
        val pattern = if (decimals == 0) ",##0" else ",##0.${"0".repeat(decimals)}"
        DecimalFormat(pattern).format(BigDecimal(price).setScale(decimals, RoundingMode.HALF_UP))
    }.getOrDefault("--")

private fun formatCardDate(timestamp: Long, timeFrame: String): String {
    val millis = if (timestamp < 1_000_000_000_000L) timestamp * 1000 else timestamp
    val locale = Locale.getDefault()
    val skeleton = when (timeFrame.lowercase()) {
        "1d", "1w" -> "yMMMd"
        else -> "MMMdjmm"
    }
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return runCatching {
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern, locale))
    }.getOrDefault("--")
}

private fun formatCandleTime(timestamp: Long, timeFrame: String): String {
    val millis = if (timestamp < 1_000_000_000_000L) timestamp * 1000 else timestamp
    val zone = ZoneId.systemDefault()
    val pattern = when (timeFrame.lowercase()) {
        "1m", "5m", "15m", "1h", "4h" -> "MM-dd HH:mm"
        else -> "yyyy-MM-dd"
    }
    return runCatching {
        Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern(pattern))
    }.getOrDefault("--")
}
