package one.mixin.android.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import one.mixin.android.extension.findFragmentActivityOrNull

@Composable
internal fun homeNavigationInset(): Dp {
    val activity = LocalContext.current.findFragmentActivityOrNull() as? MainActivity
    return with(LocalDensity.current) { (activity?.homeContentInset ?: 0).toDp() }
}
