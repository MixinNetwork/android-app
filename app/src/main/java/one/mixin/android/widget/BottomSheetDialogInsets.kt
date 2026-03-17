package one.mixin.android.widget

import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.internal.ViewUtils.doOnApplyWindowInsets

internal fun BottomSheetDialog.applyBottomSheetContainerInsets(transparentStatusBar: Boolean = false) {
    findViewById<View>(com.google.android.material.R.id.container)?.apply {
        fitsSystemWindows = false
        doOnApplyWindowInsets(this) { insetView, windowInsets, initialMargins ->
            insetView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                val topInset = if (transparentStatusBar) {
                    0
                } else {
                    val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                    val displayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
                    maxOf(systemBars.top, displayCutout.top)
                }
                updateMargins(top = initialMargins.top + topInset)
            }
            windowInsets
        }
    }

    findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows = false
    findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.apply {
        fitsSystemWindows = false
        doOnApplyWindowInsets(this) { insetView, windowInsets, initialPadding ->
            insetView.updatePadding(
                bottom = initialPadding.bottom + windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom,
            )
            windowInsets
        }
    }
}
