package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R

class DraggableRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    var callback: Callback? = null

    private var direction = DIRECTION_TOP_2_BOTTOM

    private var downY = 0f
    private var dragging = false

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.DraggableRecyclerView)
        if (ta != null) {
            if (ta.hasValue(R.styleable.DraggableRecyclerView_drag_direction)) {
                direction = ta.getInteger(R.styleable.DraggableRecyclerView_drag_direction, DIRECTION_TOP_2_BOTTOM)
            }
            ta.recycle()
        }

        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val moveY = event.rawY

                    // workaround with down event empty value
                    if (downY == 0f) {
                        downY = moveY
                        return@setOnTouchListener false
                    }

                    val disY = moveY - downY
                    if (canDrag(disY) || dragging) {
                        callback?.onScroll(disY)
                        downY = moveY
                        dragging = true
                        isLayoutFrozen = true
                        return@setOnTouchListener true
                    }
                    downY = moveY
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    downY = 0f
                    if (dragging) {
                        dragging = false
                        isLayoutFrozen = false
                        callback?.onRelease()
                        return@setOnTouchListener true
                    }
                }
            }
            return@setOnTouchListener false
        }
    }

    private fun canDrag(disY: Float): Boolean {
        return if (direction == DIRECTION_TOP_2_BOTTOM) {
            !canScrollVertically(DIRECTION_TOP_2_BOTTOM) && disY > 0
        } else {
            !canScrollVertically(DIRECTION_BOTTOM_2_TOP) && disY < 0
        }
    }

    companion object {
        const val DIRECTION_TOP_2_BOTTOM = -1
        const val DIRECTION_BOTTOM_2_TOP = 1
    }

    interface Callback {
        fun onScroll(dis: Float)
        fun onRelease()
    }
}