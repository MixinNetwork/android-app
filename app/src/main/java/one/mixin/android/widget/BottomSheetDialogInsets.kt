package one.mixin.android.widget

import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.internal.ViewUtils.doOnApplyWindowInsets

internal fun BottomSheetDialog.applyBottomSheetContainerInsets(
    transparentStatusBar: Boolean = false,
    applyBottomInsetToSheet: Boolean = true,
) {
    findViewById<View>(com.google.android.material.R.id.container)?.apply {
        fitsSystemWindows = false
        doOnApplyWindowInsets(this) { insetView, windowInsets, initialMargins ->
            val topInset = if (transparentStatusBar) {
                0
            } else {
                val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val displayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
                maxOf(systemBars.top, displayCutout.top)
            }
            insetView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(top = initialMargins.top + topInset)
            }
            windowInsets
        }
    }

    findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows = false
    findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.apply {
        fitsSystemWindows = false
        doOnApplyWindowInsets(this) { insetView, windowInsets, initialPadding ->
            val tappableBottom = windowInsets.getInsets(WindowInsetsCompat.Type.tappableElement()).bottom
            insetView.updatePadding(
                bottom = if (applyBottomInsetToSheet) initialPadding.bottom + tappableBottom else initialPadding.bottom,
            )
            windowInsets
        }
    }
}
