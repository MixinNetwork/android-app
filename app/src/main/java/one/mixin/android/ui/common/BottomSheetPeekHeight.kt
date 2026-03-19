package one.mixin.android.ui.common

import android.content.Context
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.screenHeight

internal fun Context.resolveBottomSheetPeekHeight(
    view: View,
    contentHeight: Int,
    includeBottomInset: Boolean = true,
): Int {
    if (contentHeight <= 0) return 0
    if (!includeBottomInset) return contentHeight

    val screenHeight = screenHeight()
    val topInset = view.getSafeAreaInsetsTop()
    val fullHeight = screenHeight - topInset
    val sheetView = view.parent as? View
    val appliedBottomInset = sheetView?.paddingBottom ?: 0
    val availableHeight = (sheetView?.parent as? View)?.height ?: fullHeight
    val consumedBottomInset = (fullHeight - availableHeight).coerceAtLeast(0)
    val tappableBottomInset =
        ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.tappableElement())
            ?.bottom
            ?: 0
    val remainingTappableBottomInset = (tappableBottomInset - consumedBottomInset).coerceAtLeast(0)
    val bottomInset =
        if (contentHeight < fullHeight) {
            maxOf(appliedBottomInset, remainingTappableBottomInset)
        } else {
            0
        }

    return (contentHeight + bottomInset).coerceAtMost(screenHeight)
}
