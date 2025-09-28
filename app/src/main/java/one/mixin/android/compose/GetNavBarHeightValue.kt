package one.mixin.android.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun GetNavBarHeightValue(): Dp {
    val insets = WindowInsets.navigationBars
    val density = LocalDensity.current
    
    return with(density) {
        insets.getBottom(density).toDp()
    }
}