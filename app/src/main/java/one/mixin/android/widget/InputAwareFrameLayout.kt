package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import one.mixin.android.extension.animateHeight
import one.mixin.android.widget.keyboard.InputAwareLayout

class InputAwareFrameLayout : FrameLayout, InputAwareLayout.InputView {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun show(height: Int, immediate: Boolean) {
        if (immediate) {
            updateLayoutParams<ViewGroup.LayoutParams> {
                this.height = height
            }
        } else {
            animateHeight(0, height)
        }
        visibility = View.VISIBLE
    }

    override fun hide(immediate: Boolean) {
        if (immediate) {
            visibility = View.GONE
        } else {
            animateHeight(height, 0)
        }
    }

    override fun isShowing() = visibility == View.VISIBLE
}
