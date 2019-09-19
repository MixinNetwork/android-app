package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.dpToPx

class MentionLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    private var mode: Mode = Mode.MIN
        set(value) {
            field = value
            val rv = (getChildAt(0) as RecyclerView)
            rv.isLayoutFrozen = value == Mode.PART
        }

    private var lastY = 0f

    private var minHeight = context.dpToPx(150f)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val child = getChildAt(0) as RecyclerView
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val moveY = ev.y - lastY
                lastY = ev.y
                if (mode == Mode.PART) {
                    child.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = (height - moveY).toInt()
                        if (height >= this@MentionLayout.height) {
                            height = this@MentionLayout.height
                            mode = Mode.MAX
                        } else if (height < minHeight) {
                            hide()
                            return true
                        }
                    }
                } else if (mode == Mode.MAX) {
                    if (moveY > 0 && !child.canScrollVertically(-1)) {
                        mode = Mode.PART
                    }
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (mode == Mode.MAX && getChildAt(0).height < height) {
            mode = Mode.PART
        }
    }

    fun show() {
        mode = Mode.PART
        val child = getChildAt(0)
        child.animateHeight(0, minHeight)
        isVisible = true
    }

    fun hide() {
        mode = Mode.MIN
        val child = getChildAt(0)
        child.animateHeight(child.height, 0, onEndAction = {
            isGone = true
        })
    }

    enum class Mode {
        MIN,
        PART,
        MAX
    }
}
