package one.mixin.android.ui.home.market

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LineChartGradientTest {
    @Test
    fun gradientSupportsDisplayP3LineColor() {
        val color = Color(
            red = 0.314f,
            green = 0.741f,
            blue = 0.361f,
            alpha = 1f,
            colorSpace = ColorSpaces.DisplayP3,
        )

        LinearGradientShader(
            from = Offset.Zero,
            to = Offset(0f, 100f),
            colors = lineChartGradientColors(color),
        )
    }
}
