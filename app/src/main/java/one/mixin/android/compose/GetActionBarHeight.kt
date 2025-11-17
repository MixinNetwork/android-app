package one.mixin.android.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun GetActionBarHeight(): Dp {
    val context = LocalContext.current
    val density = LocalDensity.current
    val actionBarHeightPx = with(context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))) {
        val height = getDimension(0, 0f)
        recycle()
        height
    }
    return with(density) { actionBarHeightPx.toDp() }
}