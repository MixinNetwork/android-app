package one.mixin.android.ui.common

import android.content.Context
import android.view.View
import one.mixin.android.extension.getSafeAreaInsetsBottom
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.screenHeight

internal fun Context.resolveBottomSheetPeekHeight(
    view: View,
    contentHeight: Int,
): Int {
    if (contentHeight <= 0) return 0

    val screenHeight = screenHeight()
    val fullHeight = screenHeight - view.getSafeAreaInsetsTop()
    val bottomInset = if (contentHeight < fullHeight) view.getSafeAreaInsetsBottom() else 0

    return (contentHeight + bottomInset).coerceAtMost(screenHeight)
}
