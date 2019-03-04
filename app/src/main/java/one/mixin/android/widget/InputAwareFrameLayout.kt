package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import one.mixin.android.widget.keyboard.InputAwareLayout

class InputAwareFrameLayout : FrameLayout, InputAwareLayout.InputView {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

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

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        visibilityChangedListener?.onVisibilityChanged(changedView, visibility)
    }

    var visibilityChangedListener: OnVisibilityChangedListener? = null

    interface OnVisibilityChangedListener {
        fun onVisibilityChanged(changedView: View, visibility: Int)
    }
}