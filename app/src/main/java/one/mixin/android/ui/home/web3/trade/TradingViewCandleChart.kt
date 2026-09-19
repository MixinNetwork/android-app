package one.mixin.android.ui.home.web3.trade

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tradingview.lightweightcharts.api.chart.models.color.surface.SolidColor
import com.tradingview.lightweightcharts.api.chart.models.color.toIntColor
import com.tradingview.lightweightcharts.api.interfaces.ChartApi
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.options.enums.TrackingModeExitMode
import com.tradingview.lightweightcharts.api.options.models.AreaSeriesOptions
import com.tradingview.lightweightcharts.api.options.models.CandlestickSeriesOptions
import com.tradingview.lightweightcharts.api.options.models.TrackingModeOptions
import com.tradingview.lightweightcharts.api.options.models.crosshairLineOptions
import com.tradingview.lightweightcharts.api.options.models.crosshairOptions
import com.tradingview.lightweightcharts.api.options.models.gridLineOptions
import com.tradingview.lightweightcharts.api.options.models.gridOptions
import com.tradingview.lightweightcharts.api.options.models.handleScaleOptions
import com.tradingview.lightweightcharts.api.options.models.handleScrollOptions
import com.tradingview.lightweightcharts.api.options.models.layoutOptions
import com.tradingview.lightweightcharts.api.options.models.localizationOptions
import com.tradingview.lightweightcharts.api.options.models.priceScaleOptions
import com.tradingview.lightweightcharts.api.options.models.timeScaleOptions
import com.tradingview.lightweightcharts.api.series.common.SeriesData
import com.tradingview.lightweightcharts.api.series.enums.LineStyle
import com.tradingview.lightweightcharts.api.series.enums.LineWidth
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.LineData
import com.tradingview.lightweightcharts.api.series.models.MouseEventParams
import com.tradingview.lightweightcharts.api.series.models.PriceFormat
import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.api.series.models.TimeRange
import com.tradingview.lightweightcharts.runtime.plugins.DateTimeFormat
import com.tradingview.lightweightcharts.runtime.plugins.TimeFormatter
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
import java.util.Locale
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
    lineMode: Boolean = false,
    trendUp: Boolean = true,
    onSelectionChange: (ChartSelection?) -> Unit = {},
) {
    val context = LocalContext.current
    val quoteColorReversed =
        context.defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val upColor = if (quoteColorReversed) CANDLE_DOWN_COLOR else CANDLE_UP_COLOR
    val downColor = if (quoteColorReversed) CANDLE_UP_COLOR else CANDLE_DOWN_COLOR
    val backgroundColor = MixinAppTheme.colors.background
    val textColor = MixinAppTheme.colors.textAssist
    val crosshairColor = MixinAppTheme.colors.textAssist.copy(alpha = CANDLE_CROSSHAIR_ALPHA)
    val animationsEnabled =
        remember(context) {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        }
    val dateTimeFormat = remember(timeFrame) { tradingViewDateTimeFormat(timeFrame) }
    val locale = remember { Locale.getDefault().toLanguageTag() }
    val chartTimeFormatter =
        remember(locale, dateTimeFormat) {
            TimeFormatter(
                locale = locale,
                dateTimeFormat =
                    if (dateTimeFormat == DateTimeFormat.TIME) {
                        DateTimeFormat.DATE_TIME
                    } else {
                        DateTimeFormat.DATE
                    },
            )
        }
    val dataErrorMessage = stringResource(R.string.Data_error)
    val chartPriceScale = remember(priceScale) { tradingViewPriceScale(priceScale) }
    val chartPriceFormat =
        remember(chartPriceScale) {
            PriceFormat.priceFormatBuiltIn(
                type = PriceFormat.Type.PRICE,
                precision = chartPriceScale,
                minMove = BigDecimal.ONE.movePointLeft(chartPriceScale).toFloat(),
            )
        }
    val chartCandles =
        remember(candles) {
            tradingViewCandles(candles.firstOrNull()?.items.orEmpty())
        }
    // Keep a stable list instance across recompositions so LaunchedEffect's key comparison
    // stays cheap and an incidental allocation cannot re-trigger setData.
    val candleData =
        remember(chartCandles) {
            chartCandles.map { it.data }
        }
    val candlesByTimestamp =
        remember(chartCandles) {
            chartCandles.associate { candle ->
                (candle.data.time as Time.Utc).timestamp to candle.item
            }
        }
    val latestCandlesByTimestamp = rememberUpdatedState(candlesByTimestamp)
    // Read at touch time rather than at factory time, so a theme change reaches the overlay.
    val latestCrosshairColor = rememberUpdatedState(crosshairColor)
    // The header follows the finger, so the selection has to leave this composable.
    val latestOnSelectionChange = rememberUpdatedState(onSelectionChange)
    // The reference measures the inspected candle against the first candle of the series.
    val firstCandleClose = chartCandles.firstOrNull()?.item?.close
    val currentPrice = tradingViewPrice(marketPrice)
    val latestOpen = candleData.lastOrNull()?.open
    val priceLineColor = if (currentPrice != null && latestOpen != null && currentPrice < latestOpen) downColor else upColor
    var chartApi by remember { mutableStateOf<ChartApi?>(null) }
    var seriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    // Vertical position (dp, chart coordinates) of the live price, for the Compose price line.
    var marketPriceY by remember { mutableStateOf<Float?>(null) }
    var hasInitialPosition by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCandle by remember { mutableStateOf<SelectedCandle?>(null) }
    var isLongPressActive by remember { mutableStateOf(false) }
    // The page draws the scrub overlay itself - it fades the plot to the right of the touch and
    // rules a line down it - so the Compose layer keeps a handle on the WebView to drive it.
    // Deliberately not a snapshot state: it is only ever read from touch callbacks.
    val scrubWebView = remember { arrayOfNulls<WebView>(1) }
    var liveMarkerCenter by remember { mutableStateOf<Offset?>(null) }
    var visibleRangeToken by remember { mutableStateOf(0) }
    // Which chart style the series currently in the chart was created for; null until the
    // AndroidView factory has built one.
    var seriesLineMode by remember { mutableStateOf<Boolean?>(null) }
    // A line series paints its whole range in one colour, so it must not follow the live price
    // (that flips inside a single bar). The reference takes the colour from the market's 24h
    // change - the same figure the header shows - so the line and the header always agree, and the
    // colour holds steady while the price moves within a bar.
    val trendColor = if (trendUp) upColor else downColor
    // The reference paints the line, the live dot, the price line and its badge in a single
    // colour, so in line mode the live price colour has to follow the line rather than the bar.
    val liveColor = if (lineMode) trendColor else priceLineColor
    val onCrosshairMove =
        remember {
            { params: MouseEventParams ->
                if (isLongPressActive) {
                    val candle = candleForTradingViewTime(params.time, latestCandlesByTimestamp.value)
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
        val data: List<SeriesData> = if (lineMode) tradingViewLineData(candleData) else candleData
        series.setData(data)
        if (!hasInitialPosition && candleData.isNotEmpty()) {
            chartApi?.timeScale?.scrollToPosition(INITIAL_RIGHT_OFFSET, false)
            hasInitialPosition = true
        }
    }

    val latestCandle = candleData.lastOrNull()
    val latestCandleTime = latestCandle?.time
    // The dot marks the end of the series, so it has to sit on that point: the live price ticks
    // between candle refreshes and would leave the dot floating off the end of the line.
    val latestCandleClose = latestCandle?.close
    LaunchedEffect(seriesApi, chartApi, latestCandleClose, latestCandleTime, visibleRangeToken) {
        val series = seriesApi
        val api = chartApi
        val price = latestCandleClose
        val time = latestCandleTime
        if (series == null || api == null || price == null || time == null) {
            liveMarkerCenter = null
            return@LaunchedEffect
        }
        // Chart coordinates are density independent: the floating tooltip sample in
        // tradingview/lightweight-charts-android converts them with dpToPx before use.
        series.priceToCoordinate(price) { y ->
            if (y == null) {
                liveMarkerCenter = null
            } else {
                api.timeScale.timeToCoordinate(time) { x ->
                    liveMarkerCenter = x?.let { Offset(it, y) }
                }
            }
        }
    }

    DisposableEffect(chartApi) {
        val api = chartApi
        if (api == null) {
            onDispose { }
        } else {
            // Panning and zooming move the live candle, so re-resolve the marker position.
            val onVisibleRangeChange: (TimeRange?) -> Unit = {
                visibleRangeToken++
            }
            api.timeScale.subscribeVisibleTimeRangeChange(onVisibleRangeChange)
            onDispose {
                api.timeScale.unsubscribeVisibleTimeRangeChange(onVisibleRangeChange)
            }
        }
    }

    // The scrub overlay is drawn by the page, so the library's own crosshair would only double
    // it up. The move subscription stays - the tooltip still resolves the touched candle - but
    // neither line is ever painted.
    LaunchedEffect(chartApi) {
        chartApi?.applyOptions {
            crosshair = crosshairOptions {
                vertLine = crosshairLineOptions { visible = false }
                horzLine = crosshairLineOptions { visible = false }
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

    // Hand the header what the finger is on; null once it lifts, so it goes back to the live
    // market.
    LaunchedEffect(selectedCandle, firstCandleClose) {
        latestOnSelectionChange.value(selectedCandle?.toChartSelection(firstCandleClose))
    }

    LaunchedEffect(seriesApi, currentPrice, visibleRangeToken) {
        val series = seriesApi
        val price = currentPrice
        if (series == null || price == null) {
            marketPriceY = null
            return@LaunchedEffect
        }
        series.priceToCoordinate(price) { marketPriceY = it }
    }

    // The page outlives the window, but the WebView's canvas compositing layers do not: they
    // are discarded when the window is hidden (screen off or app backgrounded), and nothing
    // brings them back - not a repaint, not chart.resize(), not a chart option change, not
    // even a page reload. Only freshly created canvases reach the screen again.
    // Rebuilding the ChartsView is therefore the only reliable repair: a new instance gets a
    // new WebSession, a new page and a new message channel, and the AndroidView factory below
    // re-issues applyOptions/addCandlestickSeries so the new series actually receives data.
    // (A bare webView.loadUrl() is not enough: the library's WebMessageChannel stays bound to
    // the page it was created for, so the reloaded chart is created but never drawn into.)
    var chartReloadToken by remember { mutableStateOf(0) }
    var chartGeneration by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var windowWasHidden = false
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> windowWasHidden = true
                    Lifecycle.Event.ON_RESUME ->
                        if (windowWasHidden) {
                            windowWasHidden = false
                            chartReloadToken++
                        }

                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(chartReloadToken) {
        if (chartReloadToken == 0) return@LaunchedEffect
        // Let the window come back and draw before rebuilding, so the new chart starts
        // against a visible surface instead of repeating the original first-paint race.
        withFrameNanos { }
        // The rebuilt series needs its own initial position.
        hasInitialPosition = false
        chartGeneration++
    }

    // A series' type is fixed when it is created, so switching between candles and the line
    // chart means replacing it. Both the AndroidView factory (first build) and the effect
    // below (every later switch) go through this one function.
    fun attachSeries(api: ChartApi) {
        val onCreated: (SeriesApi) -> Unit = { series ->
            seriesApi = series
            seriesLineMode = lineMode
        }
        if (lineMode) {
            api.addAreaSeries(
                options =
                    AreaSeriesOptions(
                        lineColor = trendColor.toArgb().toIntColor(),
                        topColor = trendColor.copy(alpha = AREA_FILL_ALPHA).toArgb().toIntColor(),
                        bottomColor = trendColor.copy(alpha = 0f).toArgb().toIntColor(),
                        lineWidth = LineWidth.THREE,
                        crosshairMarkerVisible = false,
                        lastValueVisible = false,
                        priceLineVisible = false,
                        priceFormat = chartPriceFormat,
                    ),
                onSeriesCreated = onCreated,
            )
        } else {
            api.addCandlestickSeries(
                options =
                    CandlestickSeriesOptions(
                        upColor = upColor.toArgb().toIntColor(),
                        downColor = downColor.toArgb().toIntColor(),
                        wickVisible = true,
                        wickUpColor = upColor.toArgb().toIntColor(),
                        wickDownColor = downColor.toArgb().toIntColor(),
                        borderVisible = false,
                        lastValueVisible = false,
                        priceLineVisible = false,
                        priceFormat = chartPriceFormat,
                    ),
                onSeriesCreated = onCreated,
            )
        }
    }

    // The series only takes its colours when it is created, so push them again whenever the
    // market's direction flips. The line and its live dot have to stay the same colour.
    LaunchedEffect(seriesApi, lineMode, trendColor) {
        val series = seriesApi ?: return@LaunchedEffect
        if (!lineMode) return@LaunchedEffect
        series.applyOptions(
            AreaSeriesOptions(
                lineColor = trendColor.toArgb().toIntColor(),
                topColor = trendColor.copy(alpha = AREA_FILL_ALPHA).toArgb().toIntColor(),
                bottomColor = trendColor.copy(alpha = 0f).toArgb().toIntColor(),
            ),
        )
    }

    LaunchedEffect(chartApi, lineMode) {
        val api = chartApi ?: return@LaunchedEffect
        val current = seriesApi ?: return@LaunchedEffect
        if (seriesLineMode == lineMode) return@LaunchedEffect
        seriesApi = null
        api.removeSeries(current) { }
        attachSeries(api)
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Deliberately NOT wrapped in Modifier.alpha(): an alpha layer puts the WebView into an
        // offscreen graphicsLayer, and on some devices that stops its canvas layers from ever
        // being composited - the chart renders inside the WebView but never reaches the screen.
        key(chartGeneration) {
            AndroidView(
                // The badge gets its own gutter so it never sits on top of the newest bars.
                modifier = Modifier.fillMaxSize().padding(end = PRICE_GUTTER_DP.dp),
                factory = { chartContext ->
                    ChartsView(chartContext).apply {
                        // WebSession keeps its WebView INVISIBLE until onPageFinished, so the page -
                        // and the chart it builds at `load` - is painted while the view is still not
                        // drawn. On some devices the compositor then never rasterises the chart's
                        // canvas layers and the pane stays blank until the WebView is recreated.
                        // Reveal it up front so the first frame is produced against a visible surface.
                        scrubWebView[0] =
                            (0 until childCount)
                                .map(::getChildAt)
                                .filterIsInstance<WebView>()
                                .onEach { it.visibility = View.VISIBLE }
                                .firstOrNull()
                        // The touch delegate reports MotionEvent coordinates in pixels, while the
                        // chart API and MouseEventParams.point are density independent.
                        val density = chartContext.resources.displayMetrics.density
                        val selectCandleAt: (Float) -> Unit = { touchX ->
                            val pointX = touchX / density
                            scrubWebView[0]?.scrubTo(pointX, latestCrosshairColor.value)
                            api.timeScale.coordinateToTime(pointX) { time ->
                                if (isLongPressActive) {
                                    candleForTradingViewTime(time, latestCandlesByTimestamp.value)?.let { candle ->
                                        selectedCandle = SelectedCandle(candle, pointX)
                                    }
                                }
                            }
                        }
                        addTouchDelegate(
                            ChartTouchDelegate(
                                context = chartContext,
                                onTouchStart = {
                                    isLongPressActive = false
                                    selectedCandle = null
                                    scrubWebView[0]?.scrubTo(null, latestCrosshairColor.value)
                                },
                                onLongPress = { pointX ->
                                    isLongPressActive = true
                                    selectCandleAt(pointX)
                                },
                                onLongPressMove = selectCandleAt,
                                onTouchEnd = {
                                    isLongPressActive = false
                                    selectedCandle = null
                                    scrubWebView[0]?.scrubTo(null, latestCrosshairColor.value)
                                },
                            ),
                        )
                        subscribeOnChartStateChange { state ->
                            errorMessage =
                                tradingViewErrorMessage(
                                    (state as? ChartsView.State.Error)?.exception,
                                    dataErrorMessage,
                                )
                        }
                        api.applyOptions {
                            layout = layoutOptions {
                                background = SolidColor(backgroundColor.toArgb())
                                this.textColor = textColor.toArgb().toIntColor()
                            }
                            localization = localizationOptions {
                                this.locale = locale
                                timeFormatter = chartTimeFormatter
                            }
                            // The reference chart carries no chrome of its own: no grid, no time
                            // scale, no price scale. Everything it needs to say is on the line and
                            // on the price badge the Compose layer draws over the pane.
                            timeScale = timeScaleOptions {
                                rightOffset = INITIAL_RIGHT_OFFSET
                                barSpacing = INITIAL_BAR_SPACING
                                minBarSpacing = MIN_BAR_SPACING
                                rightBarStaysOnScroll = true
                                visible = false
                            }
                            rightPriceScale = priceScaleOptions {
                                visible = false
                            }
                            grid = gridOptions {
                                horzLines = gridLineOptions { visible = false }
                                vertLines = gridLineOptions { visible = false }
                            }
                            handleScroll = handleScrollOptions {
                                horzTouchDrag = true
                                vertTouchDrag = false
                            }
                            handleScale = handleScaleOptions {
                                pinch = true
                            }
                            // The library leaves crosshair tracking armed until the next tap by
                            // default. That strands the chart: a scrub turns tracking on, and every
                            // drag after it is read as a crosshair move rather than a pan, so it
                            // never pans again - only a tap with no movement gets out. Release
                            // tracking on touch up instead, so a scrub is a one-off inspection.
                            trackingMode =
                                TrackingModeOptions(exitMode = TrackingModeExitMode.ON_TOUCH_END)
                        }
                        chartApi = api
                        attachSeries(api)
                    }
                },
            )
        }
        LivePriceLine(
            y = marketPriceY,
            priceText = marketPrice,
            color = liveColor,
            maxY = maxHeight,
        )
        // The reference shows the pulsing live dot on the line chart only; candles keep their
        // original look.
        if (animationsEnabled && lineMode) {
            LivePriceMarker(
                center = liveMarkerCenter,
                color = liveColor,
            )
        }
        selectedCandle?.let { selection ->
            // Centre the card on the touched candle and keep it inside the chart. pointX is
            // density independent, so it is comparable with the layout width.
            val cardWidth = CANDLE_TOOLTIP_WIDTH_DP.dp
            val margin = CANDLE_TOOLTIP_MARGIN_DP.dp
            val maxOffset =
                (maxWidth - PRICE_GUTTER_DP.dp - cardWidth - margin).coerceAtLeast(margin)
            val offsetX = (selection.pointX.dp - cardWidth / 2).coerceIn(margin, maxOffset)
            CandleDetailsTooltip(
                candle = selection.candle,
                timeFrame = timeFrame,
                priceScale = chartPriceScale,
                upColor = upColor,
                downColor = downColor,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(top = margin)
                        .offset(x = offsetX),
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

// The reference chart carries no price scale, so the live price is the only price on screen: a
// dashed line across the pane with a rounded badge in the gutter beside it.
@Composable
private fun LivePriceLine(
    y: Float?,
    priceText: String?,
    color: Color,
    maxY: Dp,
) {
    val lineY = y ?: return
    val text = priceText?.takeIf(String::isNotBlank) ?: return
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val yPx = lineY.dp.toPx()
            if (yPx < 0f || yPx > size.height) return@Canvas
            drawLine(
                color = color.copy(alpha = LIVE_PRICE_LINE_ALPHA),
                start = Offset(0f, yPx),
                // Runs under the badge rather than stopping short of the gutter: the badge is
                // drawn on top and opaque, so the line meets it whatever width its text needs.
                end = Offset(size.width, yPx),
                strokeWidth = LIVE_PRICE_LINE_STROKE_DP.dp.toPx(),
                pathEffect =
                    PathEffect.dashPathEffect(
                        floatArrayOf(
                            LIVE_PRICE_LINE_DASH_DP.dp.toPx(),
                            LIVE_PRICE_LINE_GAP_DP.dp.toPx(),
                        ),
                    ),
            )
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        y =
                            (lineY - PRICE_BADGE_HEIGHT_DP / 2f)
                                .coerceIn(0f, (maxY - PRICE_BADGE_HEIGHT_DP.dp).value)
                                .dp,
                    )
                    // Sized by its own text instead of a fixed plate, so a short price does not
                    // float in a wide empty box. Not capped at the gutter either: a long price has
                    // to stay readable, so the plate is allowed to grow past it, as the reference
                    // lets it.
                    .clip(PRICE_BADGE_SHAPE)
                    .background(color = color)
                    .padding(
                        horizontal = PRICE_BADGE_H_PADDING_DP.dp,
                        vertical = PRICE_BADGE_V_PADDING_DP.dp,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = PRICE_BADGE_FONT_SP.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Marks the live price with a ring that keeps expanding and fading out.
@Composable
private fun LivePriceMarker(
    center: Offset?,
    color: Color,
) {
    val transition = rememberInfiniteTransition(label = "livePriceMarker")
    val progress by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(LIVE_MARKER_DURATION_MS, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "livePriceMarkerProgress",
        )
    val markerCenter = center ?: return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerPx = Offset(markerCenter.x.dp.toPx(), markerCenter.y.dp.toPx())
        val ringRadius =
            (
                LIVE_MARKER_MIN_RADIUS_DP +
                    (LIVE_MARKER_MAX_RADIUS_DP - LIVE_MARKER_MIN_RADIUS_DP) * progress
            ).dp.toPx()
        drawCircle(
            color = color.copy(alpha = LIVE_MARKER_ALPHA * (1f - progress)),
            radius = ringRadius,
            center = centerPx,
            style = Stroke(width = LIVE_MARKER_STROKE_DP.dp.toPx()),
        )
        drawCircle(
            color = color,
            radius = LIVE_MARKER_MIN_RADIUS_DP.dp.toPx(),
            center = centerPx,
        )
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

    // androidx.compose.material.Surface installs an empty pointerInput node, which turns the
    // card into a hit target and swallows every touch inside its bounds: the chart underneath
    // stops responding to drags as soon as the card becomes visible. A plain Box keeps the card
    // purely visual so the gesture reaches the chart.
    Box(
        modifier =
            modifier
                .width(CANDLE_TOOLTIP_WIDTH_DP.dp)
                .shadow(elevation = CANDLE_TOOLTIP_ELEVATION_DP.dp, shape = CANDLE_TOOLTIP_SHAPE)
                .background(color = MixinAppTheme.colors.background, shape = CANDLE_TOOLTIP_SHAPE),
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

private fun formatCandlePrice(
    value: String,
    priceScale: Int,
): String =
    value.toBigDecimalOrNull()
        ?.setScale(tradingViewPriceScale(priceScale), RoundingMode.HALF_UP)
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

// The header follows the finger, so hand it the inspected candle in the shape it needs: the
// candle's own price, and how far it has moved from the first candle of the series.
private fun SelectedCandle.toChartSelection(firstClose: String?): ChartSelection? {
    val first = firstClose?.toBigDecimalOrNull() ?: return null
    val close = candle.close.toBigDecimalOrNull() ?: return null
    if (first.compareTo(BigDecimal.ZERO) == 0) return null
    val percent =
        close
            .divide(first, CHANGE_PERCENT_CALCULATION_SCALE, RoundingMode.HALF_UP)
            .subtract(BigDecimal.ONE)
            .multiply(BigDecimal(100))
    return ChartSelection(price = candle.close, changePercent = percent)
}

internal fun normalizeTradingViewTimestamp(timestamp: Long): Long =
    if (timestamp >= MILLIS_TIMESTAMP_THRESHOLD) timestamp / 1000 else timestamp

// The chart runs in a WebView, so a value it cannot render aborts the chart script and
// leaves the pane blank. Reject anything the price scale cannot represent.
internal fun tradingViewPrice(raw: String?): Float? =
    raw?.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f }

// A precision beyond this would collapse minMove to zero and overflow price formatting.
internal fun tradingViewPriceScale(priceScale: Int): Int =
    priceScale.coerceIn(0, MAX_TRADING_VIEW_PRICE_SCALE)

internal fun tradingViewTimestamp(timestamp: Long): Long? =
    normalizeTradingViewTimestamp(timestamp)
        .takeIf { it in MIN_TRADING_VIEW_TIMESTAMP..MAX_TRADING_VIEW_TIMESTAMP }

internal data class TradingViewCandle(
    val item: CandleItem,
    val data: CandlestickData,
)

internal fun tradingViewCandles(
    items: List<CandleItem>,
): List<TradingViewCandle> =
    items.mapNotNull { item ->
        val open = tradingViewPrice(item.open) ?: return@mapNotNull null
        val high = tradingViewPrice(item.high) ?: return@mapNotNull null
        val low = tradingViewPrice(item.low) ?: return@mapNotNull null
        val close = tradingViewPrice(item.close) ?: return@mapNotNull null
        // An inverted range is corrupt rather than a rounding artefact, so drop the bar.
        if (high < low) return@mapNotNull null
        val timestamp = tradingViewTimestamp(item.timestamp) ?: return@mapNotNull null
        TradingViewCandle(
            item = item,
            data =
                CandlestickData(
                    time = Time.Utc(timestamp),
                    open = open,
                    // Widen the range so open and close always sit inside high and low.
                    high = maxOf(high, open, close),
                    low = minOf(low, open, close),
                    close = close,
                ),
        )
    }.associateBy { candle ->
        (candle.data.time as Time.Utc).timestamp
    }.values.sortedBy { candle ->
        (candle.data.time as Time.Utc).timestamp
    }

// An area series plots `value`, not OHLC, so candlestick points cannot be reused as-is - the
// library reads `item.value` and would plot NaN for every bar, leaving the chart empty.
internal fun tradingViewLineData(candles: List<CandlestickData>): List<LineData> =
    candles.map { candle -> LineData(time = candle.time, value = candle.close) }

internal fun tradingViewDateTimeFormat(timeFrame: String): DateTimeFormat =
    when (timeFrame.lowercase()) {
        "1d", "1w" -> DateTimeFormat.DATE
        else -> DateTimeFormat.TIME
    }

internal fun candleForTradingViewTime(
    time: Time?,
    candlesByTimestamp: Map<Long, CandleItem>,
): CandleItem? = (time as? Time.Utc)?.timestamp?.let(candlesByTimestamp::get)

internal fun tradingViewErrorMessage(
    error: Throwable?,
    fallback: String,
): String? = error?.localizedMessage?.takeIf(String::isNotBlank) ?: error?.let { fallback }

// The reference fades the plot to the right of the point being inspected and rules a line down
// it, so the part you are reading stands out from the part you are not. The plot is a canvas
// inside the page, so the fade has to be a mask on that canvas: laying a colour over it would
// dull the live price line too, and that one the Compose layer draws on top of the WebView.
private fun WebView.scrubTo(
    pointX: Float?,
    color: Color,
) {
    evaluateJavascript(scrubScript(pointX, color), null)
}

private fun scrubScript(
    pointX: Float?,
    color: Color,
): String {
    val argb = color.toArgb()
    val css =
        "rgba(${(argb shr 16) and 0xFF}, ${(argb shr 8) and 0xFF}, ${argb and 0xFF}, " +
            "${(argb ushr 24) / 255f})"
    return "$SCRUB_SCRIPT(${pointX ?: "null"}, '$css')"
}

private class ChartTouchDelegate(
    context: Context,
    private val onTouchStart: () -> Unit,
    private val onLongPress: (Float) -> Unit,
    private val onLongPressMove: (Float) -> Unit,
    private val onTouchEnd: () -> Unit,
) : TouchDelegate {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var lastLongPressX = 0f
    private var longPressActive = false
    private var targetView: ViewGroup? = null
    private val gestureDetector =
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onLongPress(e: MotionEvent) {
                    longPressActive = true
                    lastLongPressX = e.x
                    targetView?.requestDisallowInterceptTouchEvent(true)
                    targetView?.performHapticFeedback(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            HapticFeedbackConstants.GESTURE_START
                        } else {
                            HapticFeedbackConstants.LONG_PRESS
                        },
                    )
                    onLongPress(e.x)
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
                onTouchStart()
                downX = event.x
                downY = event.y
                longPressActive = false
                view.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_POINTER_DOWN -> view.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount > 1) {
                    view.requestDisallowInterceptTouchEvent(true)
                } else if (longPressActive) {
                    view.requestDisallowInterceptTouchEvent(true)
                    if (abs(event.x - lastLongPressX) >= touchSlop / 2f) {
                        lastLongPressX = event.x
                        onLongPressMove(event.x)
                    }
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

// Installed into the page the first time a scrub starts. The WebView is rebuilt whenever the
// chart is, so the script re-installs itself; window.__mixinScrub keeps that idempotent. The mask
// mirrors the reference - full opacity up to the touch, 27% to its right - and is scoped to the
// chart container, so the cursor, which lives on the body, is left alone.
private val SCRUB_SCRIPT =
    """
    (function (x, color) {
      var el = document.querySelector('.tv-lightweight-charts');
      if (!el) { return; }
      if (!window.__mixinScrub) {
        var style = document.createElement('style');
        style.textContent =
          'html.mixin-scrub .tv-lightweight-charts{' +
          '-webkit-mask-image:linear-gradient(to right,#000 var(--mixin-scrub-x),rgba(0,0,0,0.267) var(--mixin-scrub-x));' +
          'mask-image:linear-gradient(to right,#000 var(--mixin-scrub-x),rgba(0,0,0,0.267) var(--mixin-scrub-x));}' +
          '#mixin-scrub-cursor{position:fixed;width:0;border-left:1px solid var(--mixin-scrub-color);' +
          'pointer-events:none;display:none;}' +
          'html.mixin-scrub #mixin-scrub-cursor{display:block;}';
        document.head.appendChild(style);
        var cursor = document.createElement('div');
        cursor.id = 'mixin-scrub-cursor';
        document.body.appendChild(cursor);
        window.__mixinScrub = true;
      }
      var root = document.documentElement;
      if (x === null) {
        root.classList.remove('mixin-scrub');
        return;
      }
      root.style.setProperty('--mixin-scrub-color', color);
      el.style.setProperty('--mixin-scrub-x', x + 'px');
      var rect = el.getBoundingClientRect();
      var cursor = document.getElementById('mixin-scrub-cursor');
      cursor.style.left = (rect.left + x) + 'px';
      cursor.style.top = rect.top + 'px';
      cursor.style.height = rect.height + 'px';
      root.classList.add('mixin-scrub');
    })
    """.trimIndent()

private const val INITIAL_RIGHT_OFFSET = 3f
private const val INITIAL_BAR_SPACING = 10f
private const val MIN_BAR_SPACING = 4f
private const val CHANGE_PERCENT_CALCULATION_SCALE = 8
private const val CHANGE_PERCENT_DISPLAY_SCALE = 2
private const val MILLIS_TIMESTAMP_THRESHOLD = 1_000_000_000_000L
private const val MAX_TRADING_VIEW_PRICE_SCALE = 18

// 2000-01-01T00:00:00Z and 2100-01-01T00:00:00Z, guards the chart against corrupt timestamps.
private const val MIN_TRADING_VIEW_TIMESTAMP = 946_684_800L
private const val MAX_TRADING_VIEW_TIMESTAMP = 4_102_444_800L

// Candle chart styling and motion.
private val CANDLE_UP_COLOR = Color(0xFF04C249)
private val CANDLE_DOWN_COLOR = Color(0xFFFF5121)
private const val CANDLE_CROSSHAIR_ALPHA = 0.31f
// Room on the right for the live price badge, so it clears the newest bars.
private const val PRICE_GUTTER_DP = 58f
private const val PRICE_BADGE_HEIGHT_DP = 17f
private const val PRICE_BADGE_FONT_SP = 10f
private const val PRICE_BADGE_H_PADDING_DP = 7f
private const val PRICE_BADGE_V_PADDING_DP = 2f
private val PRICE_BADGE_SHAPE = RoundedCornerShape(4.dp)
private const val LIVE_PRICE_LINE_ALPHA = 0.45f
private const val LIVE_PRICE_LINE_STROKE_DP = 1f
private const val LIVE_PRICE_LINE_DASH_DP = 2f
private const val LIVE_PRICE_LINE_GAP_DP = 3f
// How opaque the top of the line chart's gradient fill is (fades to fully transparent).
private const val AREA_FILL_ALPHA = 0.2f
private val CANDLE_TOOLTIP_SHAPE = RoundedCornerShape(6.dp)
private const val CANDLE_TOOLTIP_ELEVATION_DP = 2f
private const val CANDLE_TOOLTIP_WIDTH_DP = 140f
private const val CANDLE_TOOLTIP_MARGIN_DP = 8f
private const val LIVE_MARKER_MIN_RADIUS_DP = 6f
private const val LIVE_MARKER_MAX_RADIUS_DP = 24f
private const val LIVE_MARKER_STROKE_DP = 3f
private const val LIVE_MARKER_ALPHA = 0.4f
private const val LIVE_MARKER_DURATION_MS = 1000
