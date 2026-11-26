package one.mixin.android.ui.home.market

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.marketPriceFormat
import one.mixin.android.vo.Fiats
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun lineChartGradientColors(color: Color): List<Color> =
    listOf(color.copy(alpha = 0.7f), color.copy(alpha = 0f))

@Composable
fun normalizeValues(values: List<Float>, minRange: Float = 0.2f, normalizedMaxRange: Float? = null): Triple<List<Float>, Int, Int> {
    if (values.isEmpty()) return Triple(emptyList(), -1, -1)
    val maxRange = normalizedMaxRange ?: maxRange()

    return remember(values, minRange, maxRange) {
        val minValue = values.minOrNull() ?: 0f
        val maxValue = values.maxOrNull() ?: 0f

        if (minValue == maxValue) {
            return@remember Triple(values.map { 0.5f }, 0, values.size - 1)
        }

        val minIndex = values.indexOf(minValue)
        val maxIndex = values.indexOf(maxValue)

        val normalizedValues = values.map { value ->
            val normalizedValue = (value - minValue) / (maxValue - minValue) * (maxRange - minRange) + minRange
            1f - normalizedValue
        }

        Triple(normalizedValues, minIndex, maxIndex)
    }
}

@Composable
private fun maxRange(): Float {
    val heightInDp = 204.dp
    val fontSizeInSp = 34.sp
    val fontSizeInDp = fontSizeInSp.let { with(LocalDensity.current) { it.toDp() } }
    return 1f - (fontSizeInDp.value / heightInDp.value)
}

@Composable
fun LineChart(
    dataPointsData: List<Float>,
    timePointsData: List<Long>? = null,
    type: String? = null,
    onHighlightChange: ((Int) -> Unit)? = null,
    showMinMaxMarkers: Boolean = onHighlightChange != null,
) {
    val (dataPoints, minIndex, maxIndex) = normalizeValues(dataPointsData, normalizedMaxRange = if (showMinMaxMarkers) null else 1f)
    MixinAppTheme {
        val textPrimary = MixinAppTheme.colors.textPrimary
        val background = MixinAppTheme.colors.background
        var highlightPointIndex by remember { mutableIntStateOf(-1) }
        val context = LocalContext.current
        val quoteColorPref = context.defaultSharedPreferences
            .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
        val isRising = dataPointsData.isEmpty() || dataPointsData.last() >= dataPointsData.first()
        val color = if (isRising != quoteColorPref) MixinAppTheme.colors.marketGreen else MixinAppTheme.colors.marketRed
        val textColor = if (isRising != quoteColorPref) MixinAppTheme.colors.marketGreen else MixinAppTheme.colors.marketRed
        val circleColor = MixinAppTheme.colors.background
        val lineWidth = Dp(1.5f)

        Box(modifier = Modifier.fillMaxSize()) {
            val pointCount by rememberUpdatedState(dataPoints.size)
            val onHighlight by rememberUpdatedState(onHighlightChange)
            Spacer(modifier = Modifier
                .fillMaxSize()
                .then(if (onHighlightChange != null) {
                    Modifier
                        .pointerInput(Unit) {
                            detectDragGestures(onDragEnd = {
                                highlightPointIndex = -1
                                onHighlight?.invoke(highlightPointIndex)
                            }) { change, _ ->
                                if (pointCount < 2 || size.width == 0) return@detectDragGestures
                                val spacing = size.width.toFloat() / (pointCount - 1)
                                val draggedIndex = (change.position.x.div(spacing)).toInt()
                                highlightPointIndex = if (draggedIndex in 0 until pointCount) draggedIndex else -1
                                onHighlight?.invoke(highlightPointIndex)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                if (pointCount < 2 || size.width == 0) return@detectTapGestures
                                context.heavyClickVibrate()
                                val spacing = size.width.toFloat() / (pointCount - 1)
                                val startIndex = (it.x / spacing).toInt()
                                highlightPointIndex = if (startIndex in 0 until pointCount) startIndex else -1
                                onHighlight?.invoke(highlightPointIndex)

                                tryAwaitRelease()
                                highlightPointIndex = -1
                                onHighlight?.invoke(highlightPointIndex)
                            })
                        }
                } else {
                    Modifier
                })
                .drawWithCache {
                    val pathBefore = Path()
                    val pathAfter = Path()
                    val gradientPath = Path()
                    val gradientBrush = Brush.verticalGradient(colors = lineChartGradientColors(color), endY = size.height)
                    val lineStroke = Stroke(width = lineWidth.toPx())
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    onDrawBehind {
                        if (dataPoints.size < 2) return@onDrawBehind
                        val index = highlightPointIndex.takeIf { it in dataPoints.indices } ?: -1
                        pathBefore.rewind()
                        pathAfter.rewind()
                        gradientPath.rewind()
                        val spacing = size.width / (dataPoints.size - 1)
                        val startY = size.height * dataPoints[0]
                        pathBefore.moveTo(0f, startY)
                        gradientPath.moveTo(0f, startY)

                        for (i in 0 until dataPoints.lastIndex) {
                            val x1 = i * spacing
                            val y1 = size.height * dataPoints[i]
                            val x2 = (i + 1) * spacing
                            val y2 = size.height * dataPoints[i + 1]
                            val controlX = (x1 + x2) / 2
                            if (index == -1 || i <= index) {
                                pathBefore.cubicTo(controlX, y1, controlX, y2, x2, y2)
                                gradientPath.cubicTo(controlX, y1, controlX, y2, x2, y2)
                                if (i == index) pathAfter.moveTo(x2, y2)
                            } else {
                                pathAfter.cubicTo(controlX, y1, controlX, y2, x2, y2)
                            }
                        }

                        val endIndex = if (index == -1) dataPoints.lastIndex else index
                        gradientPath.lineTo(endIndex * spacing, size.height)
                        gradientPath.lineTo(0f, size.height)
                        gradientPath.close()

                        drawPath(path = gradientPath, brush = gradientBrush)
                        drawPath(path = pathBefore, color = color, style = lineStroke)

                        if (index != -1) {
                            drawPath(
                                path = pathAfter, color = Color(0xFFD9D9D9), style = lineStroke
                            )
                            drawLine(
                                color = textPrimary, start = Offset(index * spacing, 0f), end = Offset(index * spacing, size.height), strokeWidth = lineStroke.width, pathEffect = dashPathEffect
                            )

                            drawLine(
                                color = textPrimary, start = Offset(0f, startY), end = Offset(size.width, startY), strokeWidth = lineStroke.width, pathEffect = dashPathEffect
                            )

                            val circleCenter = Offset(
                                index * spacing, size.height * dataPoints[index]
                            )

                            drawCircle(
                                color = circleColor, radius = 10f, center = circleCenter
                            )

                            drawCircle(
                                color = color, radius = 8f, center = circleCenter
                            )
                        }
                        if (showMinMaxMarkers && minIndex != -1 && maxIndex != -1) {
                            val minCircleCenter = Offset(
                                minIndex * spacing, size.height * dataPoints[minIndex]
                            )

                            val maxCircleCenter = Offset(
                                maxIndex * spacing, size.height * dataPoints[maxIndex]
                            )

                            if (index != minIndex) {
                                drawCircle(
                                    color = circleColor, radius = 10f, center = minCircleCenter
                                )
                                drawCircle(
                                    color = color, radius = 8f, center = minCircleCenter
                                )
                            }
                            if (index != maxIndex) {
                                drawCircle(
                                    color = circleColor, radius = 10f, center = maxCircleCenter
                                )
                                drawCircle(
                                    color = color, radius = 8f, center = maxCircleCenter
                                )
                            }
                        }
                    }
                })

            if (showMinMaxMarkers && minIndex != -1 && maxIndex != -1) {
                SubcomposeLayout { constraints ->
                    if (dataPoints.size < 2) return@SubcomposeLayout layout(constraints.maxWidth, constraints.maxHeight) {}
                    val chartWidth = constraints.maxWidth.toFloat()
                    val chartHeight = constraints.maxHeight.toFloat()
                    val spacing = chartWidth / (dataPoints.size - 1)
                    val minXPosition = minIndex * spacing
                    val minYPosition = chartHeight * dataPoints[minIndex]
                    val maxXPosition = maxIndex * spacing
                    val maxYPosition = chartHeight * dataPoints[maxIndex]
                    val minText = BigDecimal(dataPointsData[minIndex].toDouble()).multiply(BigDecimal(Fiats.getRate())).marketPriceFormat()
                    val maxText = BigDecimal(dataPointsData[maxIndex].toDouble()).multiply(BigDecimal(Fiats.getRate())).marketPriceFormat()
                    // Measure min text
                    val minTextPlaceable = subcompose("min-text") {
                        Text(
                            modifier = Modifier.padding(top = 2.dp),
                            text = "${Fiats.getSymbol()}$minText",
                            style = TextStyle(
                                fontSize = 12.sp,
                                lineHeight = 12.sp,
                            ),
                            color = textColor,
                        )
                    }.map { it.measure(constraints) }

                    // Measure max text
                    val maxTextPlaceable = subcompose("max-text") {
                        Text(
                            modifier = Modifier.padding(bottom = 2.dp),
                            text = "${Fiats.getSymbol()}$maxText",
                            style = TextStyle(
                                fontSize = 12.sp,
                                lineHeight = 12.sp,
                            ),
                            color = textColor,
                        )
                    }.map { it.measure(constraints) }

                    val minTextWidth = minTextPlaceable.maxByOrNull { it.width }?.width?.toFloat() ?: 0f
                    val minTextHeight = minTextPlaceable.maxByOrNull { it.height }?.height?.toFloat() ?: 0f

                    val maxTextWidth = maxTextPlaceable.maxByOrNull { it.width }?.width?.toFloat() ?: 0f
                    val maxTextHeight = maxTextPlaceable.maxByOrNull { it.height }?.height?.toFloat() ?: 0f

                    // Adjust positions
                    val minXPositionAdjusted = if (chartWidth > minTextWidth) {
                        val minBound = 0f
                        val maxBound = (chartWidth - minTextWidth).coerceAtLeast(minBound)
                        (minXPosition - minTextWidth / 2).coerceIn(minBound, maxBound)
                    } else {
                        0f
                    }
                    val minYPositionAdjusted = (minYPosition).let {
                        if (it + minTextHeight > chartHeight) {
                            minYPosition - minTextHeight
                        } else {
                            it
                        }
                    }

                    val maxXPositionAdjusted = if (chartWidth > maxTextWidth) {
                        val minBound = 0f
                        val maxBound = (chartWidth - maxTextWidth).coerceAtLeast(minBound)
                        (maxXPosition - maxTextWidth / 2).coerceIn(minBound, maxBound)
                    } else {
                        0f
                    }
                    val maxYPositionAdjusted = (maxYPosition - maxTextHeight).let {
                        if (it < 0) {
                            maxYPosition + maxTextHeight
                        } else {
                            it
                        }
                    }

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        // Place min text
                        minTextPlaceable.forEach { placeable ->
                            placeable.place(
                                x = minXPositionAdjusted.toInt(), y = minYPositionAdjusted.toInt()
                            )
                        }

                        // Place max text
                        maxTextPlaceable.forEach { placeable ->
                            placeable.place(
                                x = maxXPositionAdjusted.toInt(), y = maxYPositionAdjusted.toInt()
                            )
                        }
                    }
                }
            }

            val index = highlightPointIndex
            if (index in dataPoints.indices && timePointsData != null && index in timePointsData.indices) {
                SubcomposeLayout { constraints ->
                    if (dataPoints.size < 2) return@SubcomposeLayout layout(constraints.maxWidth, constraints.maxHeight) {}
                    val chartWidth = constraints.maxWidth.toFloat()
                    val spacing = chartWidth / (dataPoints.size - 1)
                    val xPosition = index * spacing
                    val text = formatTimestamp(timePointsData[index], type)
                    val horizontalPadding = with(density) { 2.dp.toPx() }

                    val textPlaceable = subcompose("time-text") {
                        Box(
                            modifier = Modifier.background(color = background)
                        ) {
                            Text(
                                text = text, style = TextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 14.sp,
                                ), color = textPrimary, modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }.map { it.measure(constraints) }

                    val textWidth = textPlaceable.maxByOrNull { it.width }?.width?.toFloat() ?: 0f

                    // Adjust x position to ensure the Text is within bounds
                    val xPositionAdjusted = if (chartWidth > textWidth + horizontalPadding * 2) {
                        val minBound = horizontalPadding
                        val maxBound = (chartWidth - textWidth - horizontalPadding).coerceAtLeast(minBound)
                        (xPosition - textWidth / 2).coerceIn(minBound, maxBound)
                    } else {
                        horizontalPadding
                    }

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        textPlaceable.forEach { placeable ->
                            placeable.place(
                                x = xPositionAdjusted.toInt(), y = 0
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(unix: Long, type: String?): String {
    val date = Date(unix)
    return when (type) {
        "1D" -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        "1W", "1M" -> SimpleDateFormat("M/d, HH:mm", Locale.getDefault()).format(date)
        "1Y", "ALL" -> SimpleDateFormat("M/d, yyyy", Locale.getDefault()).format(date)
        else -> "Invalid type"
    }
}
