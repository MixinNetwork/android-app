package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import one.mixin.android.widget.keyboard.InputAwareFrameLayoutLayout
import one.mixin.android.widget.keyboard.InputAwareLayout

class PanelLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), InputAwareFrameLayoutLayout.InputView {

    override fun show(height: Int, immediate: Boolean) {
        val params = layoutParams
        params.height = height
        layoutParams = params
        visibility = View.VISIBLE
    }

    override fun hide(immediate: Boolean) {
        visibility = GONE
    }

    override fun isShowing() = visibility == View.VISIBLE
}