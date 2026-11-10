package one.mixin.android.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import one.mixin.android.extension.findFragmentActivityOrNull
import kotlin.math.roundToInt

@Composable
internal fun homeNavigationInset(): Dp {
    val activity = LocalContext.current.findFragmentActivityOrNull() as? MainActivity
    return with(LocalDensity.current) { (activity?.homeContentInset ?: 0).toDp() }
}

@Composable
internal fun Modifier.homeNavigationScroll(): Modifier {
    val activity = LocalContext.current.findFragmentActivityOrNull() as? MainActivity ?: return this
    val connection = remember(activity) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) {
                    activity.setBottomNavigationCollapsed(false)
                } else {
                    activity.onHomeContentScrolled(-consumed.y.roundToInt())
                }
                return Offset.Zero
            }
        }
    }
    return nestedScroll(connection)
}
