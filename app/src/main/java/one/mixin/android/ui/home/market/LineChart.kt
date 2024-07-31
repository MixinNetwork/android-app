package one.mixin.android.ui.home.market

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.unit.dp

fun normalizeValues(values: List<Float>, minRange: Float = 0.2f, maxRange: Float = 0.9f): List<Float> {
    if (values.isEmpty()) return emptyList()

    val minValue = values.minOrNull() ?: 0f
    val maxValue = values.maxOrNull() ?: 0f

    return values.map { value ->
        val normalizedValue = (value - minValue) / (maxValue - minValue) * (maxRange - minRange) + minRange
        1f - normalizedValue
    }
}

@Composable
fun LineChart(dataPointsData: List<Float>, color: Color, enableGestures: Boolean) {
    val dataPoints = normalizeValues(dataPointsData)
    var highlightPointIndex by remember { mutableIntStateOf(-1) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .then(
                if (enableGestures) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                highlightPointIndex = -1
                            }
                        ) { change, _ ->
                            val spacing = canvasSize.width / (dataPoints.size - 1)
                            val draggedIndex = (change.position.x.div(spacing)).toInt()
                            highlightPointIndex = if (draggedIndex in dataPoints.indices) draggedIndex else -1
                        }
                    }
                } else {
                    Modifier
                }
            )
        ) {
            canvasSize = size
            val path = Path()
            val gradientPath = Path()
            val spacing = size.width / (dataPoints.size - 1)
            val points = dataPoints.mapIndexed { index, value ->
                index * spacing to size.height * value
            }

            path.moveTo(points.first().first, points.first().second)
            gradientPath.moveTo(points.first().first, points.first().second)

            for (i in 0 until points.size - 1) {
                val x1 = points[i].first
                val y1 = points[i].second
                val x2 = points[i + 1].first
                val y2 = points[i + 1].second
                val controlPoint1X = (x1 + x2) / 2
                val controlPoint1Y = y1
                val controlPoint2X = (x1 + x2) / 2
                val controlPoint2Y = y2

                path.cubicTo(controlPoint1X, controlPoint1Y, controlPoint2X, controlPoint2Y, x2, y2)
                gradientPath.cubicTo(controlPoint1X, controlPoint1Y, controlPoint2X, controlPoint2Y, x2, y2)
            }

            gradientPath.lineTo(size.width, size.height)
            gradientPath.lineTo(0f, size.height)
            gradientPath.close()

            drawPath(
                path = gradientPath,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.4f), Color.Transparent),
                    endY = size.height
                )
            )

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 4f)
            )

            if (highlightPointIndex != -1) {
                val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                drawLine(
                    color = Color.Black,
                    start = Offset(highlightPointIndex * spacing, 0f),
                    end = Offset(highlightPointIndex * spacing, size.height),
                    strokeWidth = 2f,
                    pathEffect = dashPathEffect
                )
                val circleCenter = Offset(
                    highlightPointIndex * spacing,
                    size.height * dataPoints[highlightPointIndex]
                )

                drawCircle(
                    color = Color.White,
                    radius = 12f,
                    center = circleCenter
                )

                drawCircle(
                    color = color,
                    radius = 10f,
                    center = circleCenter
                )
            }
        }

        if (highlightPointIndex != -1) {
            val spacing = canvasSize.width / (dataPoints.size - 1)
            val xPosition = highlightPointIndex * spacing
            val yPosition = (dataPoints[highlightPointIndex] * canvasSize.height)

            SubcomposeLayout { constraints ->
                val text = "$${dataPointsData[highlightPointIndex]}"
                val horizontalPadding = with(density) { 2.dp.toPx() }
                val verticalPadding = with(density) { 4.dp.toPx() }

                val textPlaceable = subcompose("text") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = Color.White,
                        elevation = 4.dp,
                    ) {
                        Text(
                            text = text,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }.map { it.measure(constraints) }

                val textWidth = textPlaceable.maxByOrNull { it.width }?.width?.toFloat() ?: 0f
                val cardHeight = textPlaceable.maxByOrNull { it.height }?.height?.toFloat() ?: 0f

                // Adjust x, y position to ensure the Text is within bounds
                val xPositionAdjusted = (xPosition - textWidth / 2).coerceIn(horizontalPadding, canvasSize.width - textWidth - horizontalPadding)
                val yPositionAdjusted = (yPosition - cardHeight - verticalPadding).let {
                    if (it < 0) {
                        yPosition + verticalPadding
                    } else {
                        it
                    }
                }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    textPlaceable.forEach { placeable ->
                        placeable.place(
                            x = xPositionAdjusted.toInt(),
                            y = yPositionAdjusted.toInt()
                        )
                    }
                }
            }
        }
    }
}