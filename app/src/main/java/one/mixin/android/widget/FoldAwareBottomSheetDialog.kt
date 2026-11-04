package one.mixin.android.widget

import android.content.Context
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Job
import one.mixin.android.R
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.util.avoidBottomSheetFolds

class FoldAwareBottomSheetDialog(
    context: Context,
    theme: Int,
) : BottomSheetDialog(context, theme) {
    private var foldJob: Job? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!context.isExpandedSheetScreen()) return
        behavior.maxWidth = context.resources.getDimensionPixelSize(R.dimen.adaptive_sheet_max_width)
        val activity = context.findFragmentActivityOrNull() ?: return
        val container = findViewById<View>(com.google.android.material.R.id.container) ?: return
        foldJob?.cancel()
        foldJob = activity.avoidBottomSheetFolds(activity, container)
    }

    override fun onDetachedFromWindow() {
        foldJob?.cancel()
        foldJob = null
        super.onDetachedFromWindow()
    }
}
