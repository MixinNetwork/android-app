package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R

@SuppressLint("ClickableViewAccessibility")
class DraggableRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    var callback: Callback? = null

    private var velocityTracker: VelocityTracker? = null
    private val minVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity

    var direction = DIRECTION_NONE

    var lastVelocityY = 0f
        private set

    private var over = OVER_NONE

    private var downY = 0f
    private var startY = 0f
    private var dragging = false

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.DraggableRecyclerView)
        if (ta.hasValue(R.styleable.DraggableRecyclerView_drag_direction)) {
            direction = ta.getInteger(R.styleable.DraggableRecyclerView_drag_direction, DIRECTION_NONE)
        }
        if (ta.hasValue(R.styleable.DraggableRecyclerView_over_direction)) {
            over = ta.getInteger(R.styleable.DraggableRecyclerView_over_direction, OVER_NONE)
        }
        ta.recycle()

        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.rawY
                    startY = event.rawY

                    lastVelocityY = 0f
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(event)
                }
                MotionEvent.ACTION_MOVE -> {
                    val moveY = event.rawY

                    // workaround with down event empty value
                    if (downY == 0f) {
                        downY = moveY
                        startY = moveY
                        if (velocityTracker == null) {
                            velocityTracker = VelocityTracker.obtain()
                        }
                        velocityTracker?.addMovement(event)
                        return@setOnTouchListener false
                    }

                    val disY = moveY - downY
                    if (canDrag(disY) ||
                        dragging ||
                        // scroll bottom to top over view area
                        ((over == OVER_TOP || over == OVER_BOTH) && event.y < 0 && disY < 0 && direction < 1) ||
                        // scroll top to bottom over view area
                        ((over == OVER_BOTTOM || over == OVER_BOTH) && event.y > height && disY > 0 && direction < 1)
                    ) {
                        velocityTracker?.addMovement(event)
                        callback?.onScroll(disY)
                        downY = moveY
                        dragging = true
                        suppressLayout(true)
                        return@setOnTouchListener true
                    }
                    downY = moveY
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        velocityTracker?.addMovement(event)
                        velocityTracker?.computeCurrentVelocity(1000)
                        val vY = velocityTracker?.yVelocity
                        val vX = velocityTracker?.xVelocity
                        velocityTracker?.recycle()
                        velocityTracker = null
                        vY?.let { lastVelocityY = it }
                        val fling = if (vY != null && Math.abs(vY) >= minVelocity) {
                            if (vX != null && Math.abs(vX) > Math.abs(vY)) {
                                FLING_NONE
                            } else {
                                if (startY > event.rawY) {
                                    FLING_UP
                                } else {
                                    FLING_DOWN
                                }
                            }
                        } else {
                            FLING_NONE
                        }

                        downY = 0f
                        startY = 0f
                        dragging = false
                        suppressLayout(false)
                        callback?.onRelease(fling)
                        return@setOnTouchListener true
                    }

                    downY = 0f
                    startY = 0f
                    velocityTracker?.recycle()
                    velocityTracker = null
                }
            }
            return@setOnTouchListener false
        }
    }

    private fun canDrag(disY: Float): Boolean {
        return when (direction) {
            DIRECTION_TOP_2_BOTTOM -> !canScrollVertically(DIRECTION_TOP_2_BOTTOM) && disY > 0
            DIRECTION_BOTTOM_2_TOP -> !canScrollVertically(DIRECTION_BOTTOM_2_TOP) && disY < 0
            DIRECTION_BOTH -> (!canScrollVertically(DIRECTION_TOP_2_BOTTOM) && disY > 0) || (!canScrollVertically(DIRECTION_BOTTOM_2_TOP) && disY < 0)
            else -> false
        }
    }

    companion object {
        const val DIRECTION_NONE = -2
        const val DIRECTION_TOP_2_BOTTOM = -1
        const val DIRECTION_BOTH = 0
        const val DIRECTION_BOTTOM_2_TOP = 1

        const val OVER_NONE = -2
        const val OVER_BOTTOM = -1
        const val OVER_BOTH = 0
        const val OVER_TOP = 1

        const val FLING_UP = -1
        const val FLING_NONE = 0
        const val FLING_DOWN = 1
    }

    interface Callback {
        fun onScroll(dis: Float)
        fun onRelease(fling: Int)
    }
}
