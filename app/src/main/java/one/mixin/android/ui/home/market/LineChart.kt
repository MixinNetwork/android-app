package one.mixin.android.ui.home.market

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.heavyClickVibrate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun normalizeValues(values: List<Float>, minRange: Float = 0.18f, maxRange: Float = 0.82f): Triple<List<Float>, Int, Int> {
    if (values.isEmpty()) return Triple(emptyList(), -1, -1)

    val minValue = values.minOrNull() ?: 0f
    val maxValue = values.maxOrNull() ?: 0f

    val minIndex = values.indexOf(minValue)
    val maxIndex = values.indexOf(maxValue)

    val normalizedValues = values.map { value ->
        val normalizedValue = (value - minValue) / (maxValue - minValue) * (maxRange - minRange) + minRange
        1f - normalizedValue
    }

    return Triple(normalizedValues, minIndex, maxIndex)
}

@Composable
fun LineChart(dataPointsData: List<Float>, trend: Boolean, timePointsData: List<Long>? = null, type: String? = null, onHighlightChange: ((Int) -> Unit)? = null) {
    val (dataPoints, minIndex, maxIndex) = normalizeValues(dataPointsData)
    MixinAppTheme {
        val color = if (trend) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
        val textPrimary = MixinAppTheme.colors.textPrimary
        val background = MixinAppTheme.colors.background
        var highlightPointIndex by remember { mutableIntStateOf(-1) }
        var canvasSize by remember { mutableStateOf(Size.Zero) }
        val context = LocalContext.current
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .then(if (onHighlightChange != null) {
                    Modifier
                        .pointerInput(Unit) {
                            detectDragGestures(onDragEnd = {
                                highlightPointIndex = -1
                                onHighlightChange.invoke(highlightPointIndex)
                            }) { change, _ ->
                                val spacing = canvasSize.width / (dataPoints.size - 1)
                                val draggedIndex = (change.position.x.div(spacing)).toInt()
                                highlightPointIndex = if (draggedIndex in dataPoints.indices) draggedIndex else -1
                                onHighlightChange.invoke(highlightPointIndex)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                context.heavyClickVibrate()
                                val spacing = size.width / (dataPoints.size - 1)
                                val startIndex = (it.x / spacing).toInt()
                                highlightPointIndex = if (startIndex in dataPoints.indices) startIndex else -1
                                onHighlightChange.invoke(highlightPointIndex)

                                tryAwaitRelease()
                                highlightPointIndex = -1
                                onHighlightChange.invoke(highlightPointIndex)
                            })
                        }
                } else {
                    Modifier
                })) {
                val index = highlightPointIndex
                canvasSize = size
                val pathBefore = Path()
                val pathAfter = Path()
                val gradientPath = Path()
                val spacing = size.width / (dataPoints.size - 1)
                val points = dataPoints.mapIndexed { index, value ->
                    index * spacing to size.height * value
                }

                pathBefore.moveTo(points.first().first, points.first().second)
                gradientPath.moveTo(points.first().first, points.first().second)

                for (i in 0 until points.size - 1) {
                    val x1 = points[i].first
                    val y1 = points[i].second
                    val x2 = points[i + 1].first
                    val y2 = points[i + 1].second
                    val controlPoint1X = (x1 + x2) / 2
                    val controlPoint2X = (x1 + x2) / 2

                    if (index == -1 || i < index) {
                        pathBefore.cubicTo(controlPoint1X, y1, controlPoint2X, y2, x2, y2)
                        gradientPath.cubicTo(controlPoint1X, y1, controlPoint2X, y2, x2, y2)
                    } else if (i == index) {
                        pathBefore.cubicTo(controlPoint1X, y1, controlPoint2X, y2, x2, y2)
                        gradientPath.cubicTo(controlPoint1X, y1, controlPoint2X, y2, x2, y2)
                        pathAfter.moveTo(x2, y2)
                    } else {
                        pathAfter.cubicTo(controlPoint1X, y1, controlPoint2X, y2, x2, y2)
                    }
                }

                if (index != -1) {
                    gradientPath.lineTo(points[index.coerceAtLeast(0)].first, size.height)
                } else {
                    gradientPath.lineTo(points.last().first, size.height)
                }
                gradientPath.lineTo(0f, size.height)
                gradientPath.close()

                drawPath(
                    path = gradientPath, brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.4f), Color.Transparent), endY = size.height
                    )
                )

                drawPath(
                    path = pathBefore, color = color, style = Stroke(width = 4f)
                )

                if (index != -1) {
                    drawPath(
                        path = pathAfter, color = Color(0xFFD9D9D9), style = Stroke(width = 4f)
                    )
                }

                if (index != -1) {
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    drawLine(
                        color = textPrimary, start = Offset(index * spacing, 0f), end = Offset(index * spacing, size.height), strokeWidth = 2f, pathEffect = dashPathEffect
                    )

                    drawLine(
                        color = textPrimary, start = Offset(0f, size.height * dataPoints[0]), end = Offset(size.width, size.height * dataPoints[0]), strokeWidth = 2f, pathEffect = dashPathEffect
                    )

                    val circleCenter = Offset(
                        index * spacing, size.height * dataPoints[index]
                    )

                    drawCircle(
                        color = Color.White, radius = 10f, center = circleCenter
                    )

                    drawCircle(
                        color = color, radius = 8f, center = circleCenter
                    )
                }
                if (onHighlightChange != null && minIndex != -1 && maxIndex != -1) {
                    val minCircleCenter = Offset(
                        minIndex * spacing, size.height * dataPoints[minIndex]
                    )

                    val maxCircleCenter = Offset(
                        maxIndex * spacing, size.height * dataPoints[maxIndex]
                    )

                    if (index != minIndex) {
                        drawCircle(
                            color = Color.White, radius = 10f, center = minCircleCenter
                        )
                        drawCircle(
                            color = color, radius = 8f, center = minCircleCenter
                        )
                    }
                    if (index != maxIndex) {
                        drawCircle(
                            color = Color.White, radius = 10f, center = maxCircleCenter
                        )
                        drawCircle(
                            color = color, radius = 8f, center = maxCircleCenter
                        )
                    }
                }
            }

            if (onHighlightChange != null && minIndex != -1 && maxIndex != -1) {
                val spacing = canvasSize.width / (dataPoints.size - 1)
                val minXPosition = minIndex * spacing
                val minYPosition = canvasSize.height * dataPoints[minIndex]
                val maxXPosition = maxIndex * spacing
                val maxYPosition = canvasSize.height * dataPoints[maxIndex]

                SubcomposeLayout { constraints ->
                    val minText = "${dataPointsData[minIndex]}"
                    val maxText = "${dataPointsData[maxIndex]}"

                    // Measure min text
                    val minTextPlaceable = subcompose("minText") {
                        Text(
                            text = minText,
                            fontSize = 12.sp,
                            color = color,
                        )
                    }.map { it.measure(constraints) }

                    // Measure max text
                    val maxTextPlaceable = subcompose("maxText") {
                        Text(
                            text = maxText,
                            fontSize = 12.sp,
                            color = color,
                        )
                    }.map { it.measure(constraints) }

                    val minTextWidth = minTextPlaceable.maxByOrNull { it.width }?.width?.toFloat() ?: 0f
                    val minTextHeight = minTextPlaceable.maxByOrNull { it.height }?.height?.toFloat() ?: 0f

                    val maxTextWidth = maxTextPlaceable.maxByOrNull { it.width }?.width?.toFloat() ?: 0f
                    val maxTextHeight = maxTextPlaceable.maxByOrNull { it.height }?.height?.toFloat() ?: 0f

                    // Adjust positions
                    val minXPositionAdjusted = if (canvasSize.width > minTextWidth) {
                        (minXPosition - minTextWidth / 2).coerceIn(0f, canvasSize.width - minTextWidth)
                    } else {
                        0f
                    }
                    val minYPositionAdjusted = (minYPosition).let {
                        if (it + minTextHeight > canvasSize.height) {
                            minYPosition - minTextHeight
                        } else {
                            it
                        }
                    }

                    val maxXPositionAdjusted = if (canvasSize.width > maxTextWidth) {
                        (maxXPosition - maxTextWidth / 2).coerceIn(0f, canvasSize.width - maxTextWidth)
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
            if (index >= 0 && index <= dataPoints.size && index <= dataPointsData.size && !timePointsData.isNullOrEmpty() && index <= timePointsData.size) {
                val spacing = canvasSize.width / (dataPoints.size - 1)
                val xPosition = index * spacing

                SubcomposeLayout { constraints ->
                    val text = formatTimestamp(timePointsData[index], type)
                    val horizontalPadding = with(density) { 2.dp.toPx() }

                    val textPlaceable = subcompose("text") {
                        Box(
                            modifier = Modifier.background(color = background)
                        ) {
                            Text(
                                text = text, fontSize = 12.sp, color = textPrimary, modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }.map { it.measure(constraints) }

                    val textWidth = textPlaceable.maxByOrNull { it.width }?.width?.toFloat() ?: 0f

                    // Adjust x position to ensure the Text is within bounds
                    val xPositionAdjusted = (xPosition - textWidth / 2).coerceIn(horizontalPadding, canvasSize.width - textWidth - horizontalPadding)

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
        "YTD", "ALL" -> SimpleDateFormat("M/d, yyyy", Locale.getDefault()).format(date)
        else -> "Invalid type"
    }
}