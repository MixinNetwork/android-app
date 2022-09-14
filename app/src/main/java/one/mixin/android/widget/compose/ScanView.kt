
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import timber.log.Timber

@Composable
fun ScanView() {

    val infiniteTransition = rememberInfiniteTransition()
    val percent by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(

            animation = tween(
                1500, easing = FastOutSlowInEasing
            ), repeatMode = RepeatMode.Restart
        )
    )
    val frameVector = ImageVector.vectorResource(id = R.drawable.scan_frame)
    val framePainter = rememberVectorPainter(image = frameVector)

    val gridVector = ImageVector.vectorResource(id = R.drawable.scan_grid)
    val gridPainter = rememberVectorPainter(image = gridVector)
    val lastDrawTime = remember { mutableStateOf(System.currentTimeMillis()) }
    val count = remember { mutableStateOf(0) }
    Canvas(
        modifier = Modifier
            .width(236.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(32.dp))
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val offsetY = 3.dp.toPx()

        val colorList: List<Color> =
            listOf(Color.Transparent, Color(0x99aaffe0))
        val startY = percent * canvasHeight
        val brushHeight = canvasHeight / 4
        val brush = Brush.verticalGradient(
            colors = colorList,
            startY = startY,
            endY = startY + brushHeight,
            tileMode = TileMode.Decal
        )
        if (System.currentTimeMillis() >= lastDrawTime.value + 1000L) {
            Timber.e("Frame rate:${count.value}")
            count.value = 0
            lastDrawTime.value = System.currentTimeMillis()
        } else {
            count.value = count.value + 1
        }

        drawRect(
            brush = brush,
            topLeft = Offset(offsetY, startY),
            size = Size(canvasWidth - offsetY * 2, brushHeight)
        )
        translate(top = startY - brushHeight - offsetY, left = offsetY) {
            with(gridPainter) {
                draw(intrinsicSize)
            }
        }

        with(framePainter) {
            draw(intrinsicSize)
        }
    }
}

@Preview
@Composable
fun ScanViewPreview() {
    ScanView()
}