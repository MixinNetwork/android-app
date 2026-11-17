package one.mixin.android.ui.home

import android.view.View
import android.view.ViewGroup
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
import androidx.core.view.forEach
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomnavigation.BottomNavigationView
import one.mixin.android.R
import one.mixin.android.extension.findFragmentActivityOrNull
import kotlin.math.roundToInt
import com.google.android.material.R as MaterialR

private const val COLLAPSED_WIDTH_RATIO = 0.86f

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

// Labels grow with the font scale, so the expanded height follows the item content instead of a fixed value.
internal fun BottomNavigationView.homeNavigationExpandedHeight(): Int {
    val minHeight = resources.getDimensionPixelSize(R.dimen.home_navigation_height)
    val itemView = findViewById<View>(menu.getItem(0).itemId) ?: return minHeight
    val content = itemView.findViewById<View>(MaterialR.id.navigation_bar_item_content_container) ?: return minHeight
    content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    return maxOf(minHeight, content.marginTop + content.measuredHeight)
}

internal fun BottomNavigationView.applyHomeNavigationCollapse(progress: Float, expandedHeight: Int) {
    val collapsedHeight = resources.getDimensionPixelSize(R.dimen.home_navigation_collapsed_height)
    val margin = resources.getDimensionPixelSize(R.dimen.margin20)
    val expandedWidth = (parent as View).width - margin * 2
    val horizontalMargin = margin + (expandedWidth * (1 - COLLAPSED_WIDTH_RATIO) / 2 * progress).roundToInt()
    val barHeight = (expandedHeight + (collapsedHeight - expandedHeight) * progress).roundToInt()
    updateLayoutParams<ViewGroup.MarginLayoutParams> {
        height = barHeight
        marginStart = horizontalMargin
        marginEnd = horizontalMargin
    }
    // Item views ignore their own minimum height, and the menu view wraps them and sits centered.
    // Filling the bar with the menu view stretches every item to the bar, so the selected pill keeps an equal inset on all sides.
    val firstItem = findViewById<View>(menu.getItem(0).itemId) ?: return
    (firstItem.parent as? View)?.minimumHeight = barHeight
    // Every item has the same content height; homeNavigationExpandedHeight has already measured the first one.
    val firstContent = firstItem.findViewById<View>(MaterialR.id.navigation_bar_item_content_container) ?: return
    // Keep the expanded content centered in the bar, as it was before the items were stretched.
    val expandedOffset = (expandedHeight - firstContent.marginTop - firstContent.measuredHeight) / 2f
    menu.forEach { item ->
        val itemView = findViewById<View>(item.itemId) ?: return@forEach
        val content = itemView.findViewById<View>(MaterialR.id.navigation_bar_item_content_container) ?: return@forEach
        val icon = itemView.findViewById<View>(MaterialR.id.navigation_bar_item_icon_container) ?: return@forEach
        val iconCenter = content.top + icon.top + icon.height / 2f
        content.translationY = expandedOffset * (1 - progress) + (collapsedHeight / 2f - iconCenter) * progress
        itemView.findViewById<View>(MaterialR.id.navigation_bar_item_labels_group)?.alpha =
            (1f - progress * 2f).coerceAtLeast(0f)
    }
}
