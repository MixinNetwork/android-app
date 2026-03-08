package one.mixin.android.widget

import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.internal.ViewUtils.doOnApplyWindowInsets

internal fun BottomSheetDialog.applyBottomSheetContainerInsets(transparentStatusBar: Boolean = false) {
    findViewById<View>(com.google.android.material.R.id.container)?.apply {
        fitsSystemWindows = false
        doOnApplyWindowInsets(this) { insetView, windowInsets, initialMargins ->
            insetView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(top = initialMargins.top + if (transparentStatusBar) 0 else windowInsets.getInsets(systemBars()).top)
            }
            windowInsets
        }
    }

    findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows = false
}
