package one.mixin.android.ui.home.web3.trade

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tradingview.lightweightcharts.api.chart.models.color.surface.SolidColor
import com.tradingview.lightweightcharts.api.chart.models.color.toIntColor
import com.tradingview.lightweightcharts.api.interfaces.ChartApi
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.options.models.CandlestickSeriesOptions
import com.tradingview.lightweightcharts.api.options.models.PriceLineOptions
import com.tradingview.lightweightcharts.api.options.models.crosshairLineOptions
import com.tradingview.lightweightcharts.api.options.models.crosshairOptions
import com.tradingview.lightweightcharts.api.options.models.gridLineOptions
import com.tradingview.lightweightcharts.api.options.models.gridOptions
import com.tradingview.lightweightcharts.api.options.models.handleScaleOptions
import com.tradingview.lightweightcharts.api.options.models.handleScrollOptions
import com.tradingview.lightweightcharts.api.options.models.layoutOptions
import com.tradingview.lightweightcharts.api.options.models.priceScaleOptions
import com.tradingview.lightweightcharts.api.options.models.timeScaleOptions
import com.tradingview.lightweightcharts.api.series.common.PriceLine
import com.tradingview.lightweightcharts.api.series.enums.LineStyle
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.MouseEventParams
import com.tradingview.lightweightcharts.api.series.models.PriceFormat
import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.runtime.plugins.DateTimeFormat
import com.tradingview.lightweightcharts.view.ChartsView
import com.tradingview.lightweightcharts.view.gesture.TouchDelegate
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.CandleItem
import one.mixin.android.api.response.perps.CandleView
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

@Composable
fun TradingViewCandleChart(
    candles: List<CandleView>,
    marketPrice: String?,
    priceScale: Int,
    timeFrame: String,
) {
    val context = LocalContext.current
    val quoteColorReversed =
        context.defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val upColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val downColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val backgroundColor = MixinAppTheme.colors.background
    val accentColor = MixinAppTheme.colors.accent
    val textColor = MixinAppTheme.colors.textAssist
    val gridColor = MixinAppTheme.colors.borderColor
    val crosshairColor = MixinAppTheme.colors.textAssist
    val dateTimeFormat = remember(timeFrame) { tradingViewDateTimeFormat(timeFrame) }
    val zoneId = remember { ZoneId.systemDefault() }
    val chartPriceFormat =
        remember(priceScale) {
            val precision = priceScale.coerceAtLeast(0)
            PriceFormat.priceFormatBuiltIn(
                type = PriceFormat.Type.PRICE,
                precision = precision,
                minMove = BigDecimal.ONE.movePointLeft(precision).toFloat(),
            )
        }
    val candleData =
        remember(candles) {
            candles
                .firstOrNull()
                ?.items
                .orEmpty()
                .mapNotNull { item ->
                    val open = item.open.toFloatOrNull() ?: return@mapNotNull null
                    val high = item.high.toFloatOrNull() ?: return@mapNotNull null
                    val low = item.low.toFloatOrNull() ?: return@mapNotNull null
                    val close = item.close.toFloatOrNull() ?: return@mapNotNull null
                    CandlestickData(
                        time = Time.Utc(tradingViewLocalTimestamp(item.timestamp, zoneId)),
                        open = open,
                        high = high,
                        low = low,
                        close = close,
                    )
                }
        }
    val candlesByTimestamp =
        remember(candles) {
            candles.firstOrNull()?.items.orEmpty().associateBy { item ->
                tradingViewLocalTimestamp(item.timestamp, zoneId)
            }
        }
    val latestCandlesByTimestamp = rememberUpdatedState(candlesByTimestamp)
    val currentPrice = marketPrice?.toFloatOrNull()
    val latestOpen = candleData.lastOrNull()?.open
    val priceLineColor = if (currentPrice != null && latestOpen != null && currentPrice < latestOpen) downColor else upColor
    var chartApi by remember { mutableStateOf<ChartApi?>(null) }
    var seriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var marketPriceLine by remember { mutableStateOf<PriceLine?>(null) }
    var hasInitialPosition by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCandle by remember { mutableStateOf<SelectedCandle?>(null) }
    var isLongPressActive by remember { mutableStateOf(false) }
    val onCrosshairMove =
        remember {
            { params: MouseEventParams ->
                if (isLongPressActive) {
                    val timestamp = (params.time as? Time.Utc)?.timestamp
                    val candle = timestamp?.let(latestCandlesByTimestamp.value::get)
                    selectedCandle =
                        if (candle == null) {
                            null
                        } else {
                            SelectedCandle(candle, params.point?.x ?: 0f)
                        }
                }
            }
        }

    LaunchedEffect(seriesApi, candleData) {
        val series = seriesApi ?: return@LaunchedEffect
        series.setData(candleData)
        if (!hasInitialPosition && candleData.isNotEmpty()) {
            chartApi?.timeScale?.scrollToPosition(INITIAL_RIGHT_OFFSET, false)
            hasInitialPosition = true
        }
    }

    LaunchedEffect(chartApi, priceLineColor, accentColor, crosshairColor) {
        chartApi?.applyOptions {
            crosshair = crosshairOptions {
                vertLine = crosshairLineOptions {
                    color = crosshairColor.toArgb().toIntColor()
                    labelBackgroundColor = accentColor.toArgb().toIntColor()
                }
                horzLine = crosshairLineOptions {
                    color = crosshairColor.toArgb().toIntColor()
                    labelBackgroundColor = priceLineColor.toArgb().toIntColor()
                }
            }
        }
    }

    DisposableEffect(chartApi, onCrosshairMove) {
        val api = chartApi
        if (api == null) {
            onDispose { }
        } else {
            api.subscribeCrosshairMove(onCrosshairMove)
            onDispose {
                api.unsubscribeCrosshairMove(onCrosshairMove)
            }
        }
    }

    LaunchedEffect(seriesApi, marketPrice, priceLineColor) {
        val series = seriesApi ?: return@LaunchedEffect
        val price = marketPrice?.toFloatOrNull() ?: return@LaunchedEffect
        val options =
            PriceLineOptions(
                price = price,
                color = priceLineColor.toArgb().toIntColor(),
                lineStyle = LineStyle.DASHED,
                lineVisible = true,
                axisLabelVisible = true,
            )
        marketPriceLine?.applyOptions(options) ?: run {
            marketPriceLine = series.createPriceLine(options)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { chartContext ->
                ChartsView(chartContext).apply {
                    addTouchDelegate(
                        ChartTouchDelegate(
                            context = chartContext,
                            onLongPress = {
                                isLongPressActive = true
                                api.setCrosshairVisible(true)
                            },
                            onTouchEnd = {
                                isLongPressActive = false
                                selectedCandle = null
                                api.setCrosshairVisible(false)
                            },
                        ),
                    )
                    subscribeOnChartStateChange { state ->
                        errorMessage = (state as? ChartsView.State.Error)?.exception?.localizedMessage
                    }
                    api.applyOptions {
                        layout = layoutOptions {
                            background = SolidColor(backgroundColor.toArgb())
                            this.textColor = textColor.toArgb().toIntColor()
                        }
                        timeScale = timeScaleOptions {
                            rightOffset = INITIAL_RIGHT_OFFSET
                            barSpacing = INITIAL_BAR_SPACING
                            minBarSpacing = MIN_BAR_SPACING
                            rightBarStaysOnScroll = true
                            timeVisible = dateTimeFormat == DateTimeFormat.TIME
                            secondsVisible = false
                            borderColor = gridColor.toArgb().toIntColor()
                        }
                        rightPriceScale = priceScaleOptions {
                            borderColor = gridColor.toArgb().toIntColor()
                        }
                        grid = gridOptions {
                            horzLines = gridLineOptions {
                                color = gridColor.toArgb().toIntColor()
                            }
                            vertLines = gridLineOptions {
                                color = gridColor.toArgb().toIntColor()
                            }
                        }
                        handleScroll = handleScrollOptions {
                            horzTouchDrag = true
                            vertTouchDrag = false
                        }
                        handleScale = handleScaleOptions {
                            pinch = true
                        }
                    }
                    chartApi = api
                    api.addCandlestickSeries(
                        options =
                            CandlestickSeriesOptions(
                                upColor = upColor.toArgb().toIntColor(),
                                downColor = downColor.toArgb().toIntColor(),
                                wickUpColor = upColor.toArgb().toIntColor(),
                                wickDownColor = downColor.toArgb().toIntColor(),
                                borderVisible = false,
                                lastValueVisible = false,
                                priceLineVisible = false,
                                priceFormat = chartPriceFormat,
                            ),
                        onSeriesCreated = { seriesApi = it },
                    )
                }
            },
        )
        selectedCandle?.let { selection ->
            val alignment =
                if (selection.pointX < maxWidth.value / 2f) {
                    Alignment.TopEnd
                } else {
                    Alignment.TopStart
                }
            CandleDetailsTooltip(
                candle = selection.candle,
                timeFrame = timeFrame,
                priceScale = priceScale,
                upColor = upColor,
                downColor = downColor,
                modifier =
                    Modifier
                        .align(alignment)
                        .padding(8.dp),
            )
        }
        errorMessage?.let { error ->
            Text(
                text = error,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
            )
        }
    }
}

@Composable
private fun CandleDetailsTooltip(
    candle: CandleItem,
    timeFrame: String,
    priceScale: Int,
    upColor: Color,
    downColor: Color,
    modifier: Modifier = Modifier,
) {
    val open = candle.open.toBigDecimalOrNull()
    val close = candle.close.toBigDecimalOrNull()
    val change = if (open != null && close != null) close - open else null
    val changePercent =
        if (change != null && open != null && open.compareTo(BigDecimal.ZERO) != 0) {
            change
                .divide(open, CHANGE_PERCENT_CALCULATION_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else {
            null
        }
    val changeColor = if (change == null || change >= BigDecimal.ZERO) upColor else downColor

    Surface(
        modifier = modifier.width(140.dp),
        shape = RoundedCornerShape(4.dp),
        color = MixinAppTheme.colors.background,
        elevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            CandleDetailsRow(
                label = stringResource(R.string.Candle_Time),
                value = formatCandleTime(candle.timestamp, timeFrame),
            )
            CandleDetailsRow(
                label = stringResource(R.string.Candle_Open),
                value = formatCandlePrice(candle.open, priceScale),
            )
            CandleDetailsRow(
                label = stringResource(R.string.Candle_High),
                value = formatCandlePrice(candle.high, priceScale),
            )
            CandleDetailsRow(
                label = stringResource(R.string.Candle_Low),
                value = formatCandlePrice(candle.low, priceScale),
            )
            CandleDetailsRow(
                label = stringResource(R.string.Candle_Close),
                value = formatCandlePrice(candle.close, priceScale),
            )
            CandleDetailsRow(
                label = stringResource(R.string.Candle_Change),
                value = formatSignedDecimal(change, priceScale),
                valueColor = changeColor,
            )
            CandleDetailsRow(
                label = stringResource(R.string.Candle_Change_Percent),
                value =
                    changePercent?.let {
                        "${formatSignedDecimal(it, CHANGE_PERCENT_DISPLAY_SCALE)}%"
                    } ?: "--",
                valueColor = changeColor,
            )
        }
    }
}

@Composable
private fun CandleDetailsRow(
    label: String,
    value: String,
    valueColor: Color = MixinAppTheme.colors.textPrimary,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "$label:",
            color = MixinAppTheme.colors.textAssist,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            modifier = Modifier.width(40.dp),
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun ChartApi.setCrosshairVisible(isVisible: Boolean) {
    applyOptions {
        crosshair = crosshairOptions {
            vertLine = crosshairLineOptions {
                visible = isVisible
                labelVisible = isVisible
            }
            horzLine = crosshairLineOptions {
                visible = isVisible
                labelVisible = isVisible
            }
        }
    }
}

private fun formatCandlePrice(
    value: String,
    priceScale: Int,
): String =
    value.toBigDecimalOrNull()
        ?.setScale(priceScale.coerceAtLeast(0), RoundingMode.HALF_UP)
        ?.toPlainString()
        ?: value

private fun formatSignedDecimal(
    value: BigDecimal?,
    scale: Int,
): String {
    if (value == null) return "--"
    val normalized = value.setScale(scale.coerceAtLeast(0), RoundingMode.HALF_UP)
    val sign = if (normalized > BigDecimal.ZERO) "+" else ""
    return "$sign${normalized.toPlainString()}"
}

private fun formatCandleTime(
    timestamp: Long,
    timeFrame: String,
): String {
    val millis = if (timestamp < MILLIS_TIMESTAMP_THRESHOLD) timestamp * 1000 else timestamp
    val zoneId = ZoneId.systemDefault()
    val pattern = if (timeFrame.lowercase() in setOf("1d", "1w")) "yyyy-MM-dd" else "yyyy-MM-dd HH:mm"
    return runCatching {
        Instant.ofEpochMilli(millis)
            .atZone(zoneId)
            .format(DateTimeFormatter.ofPattern(pattern).withZone(zoneId))
    }.getOrDefault("--")
}

private data class SelectedCandle(
    val candle: CandleItem,
    val pointX: Float,
)

internal fun normalizeTradingViewTimestamp(timestamp: Long): Long =
    if (timestamp >= MILLIS_TIMESTAMP_THRESHOLD) timestamp / 1000 else timestamp

internal fun tradingViewLocalTimestamp(
    timestamp: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Long {
    val utcTimestamp = normalizeTradingViewTimestamp(timestamp)
    val offsetSeconds = Instant.ofEpochSecond(utcTimestamp).atZone(zoneId).offset.totalSeconds
    return utcTimestamp + offsetSeconds
}

internal fun tradingViewDateTimeFormat(timeFrame: String): DateTimeFormat =
    when (timeFrame.lowercase()) {
        "1d", "1w" -> DateTimeFormat.DATE
        else -> DateTimeFormat.TIME
    }

private class ChartTouchDelegate(
    context: Context,
    private val onLongPress: () -> Unit,
    private val onTouchEnd: () -> Unit,
) : TouchDelegate {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var longPressActive = false
    private var targetView: ViewGroup? = null
    private val gestureDetector =
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onLongPress(e: MotionEvent) {
                    longPressActive = true
                    targetView?.requestDisallowInterceptTouchEvent(true)
                    onLongPress()
                }
            },
        )

    override fun beforeTouchEvent(view: ViewGroup) {
        targetView = view
    }

    override fun onTouchEvent(
        view: ViewGroup,
        event: MotionEvent,
    ): Boolean {
        gestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                longPressActive = false
                view.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_POINTER_DOWN -> view.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount > 1 || longPressActive) {
                    view.requestDisallowInterceptTouchEvent(true)
                } else {
                    val deltaX = abs(event.x - downX)
                    val deltaY = abs(event.y - downY)
                    if (deltaY > touchSlop && deltaY > deltaX) {
                        view.requestDisallowInterceptTouchEvent(false)
                    } else if (deltaX > touchSlop) {
                        view.requestDisallowInterceptTouchEvent(true)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onTouchEnd()
                view.requestDisallowInterceptTouchEvent(false)
                longPressActive = false
                targetView = null
            }
        }
        return false
    }
}

private const val INITIAL_RIGHT_OFFSET = 3f
private const val INITIAL_BAR_SPACING = 10f
private const val MIN_BAR_SPACING = 4f
private const val CHANGE_PERCENT_CALCULATION_SCALE = 8
private const val CHANGE_PERCENT_DISPLAY_SCALE = 2
private const val MILLIS_TIMESTAMP_THRESHOLD = 1_000_000_000_000L
