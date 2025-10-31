package one.mixin.android.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun GetStatusBarHeightValue(): Dp {
    val insets = WindowInsets.statusBars
    val density = LocalDensity.current
    val topInset = insets.getTop(density)
    val safeAreaInset = WindowInsets.safeDrawing.getTop(density)
    return with(density) {
        maxOf(topInset, safeAreaInset).toDp()
    }
}