package one.mixin.android.util

import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import kotlin.math.abs

class DraggableViewHelper(private val target: View) {
    var callback: Callback? = null

    private var velocityTracker: VelocityTracker? = null
    private val minVelocity = FLING_MIN_VELOCITY

    var direction = DIRECTION_NONE

    var over = OVER_NONE

    var isParentBottom2TopEnable = true

    private var lastVelocityY = 0f

    private var downY = 0f
    private var startY = 0f
    private var dragging = false

    init {
        target.setOnTouchListener { _, event ->
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
                        ((over == OVER_BOTTOM || over == OVER_BOTH) && event.y > target.height && disY > 0 && direction < 1)) {
                        velocityTracker?.addMovement(event)
                        callback?.onScroll(disY)
                        downY = moveY
                        dragging = true
                        // target.suppressLayout(true)
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
                        val fling = if (vY != null && abs(vY) >= minVelocity) {
                            if (vX != null && abs(vX) > abs(vY)) {
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
                        // suppressLayout(false)
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
            DIRECTION_TOP_2_BOTTOM -> !target.canScrollVertically(DIRECTION_TOP_2_BOTTOM) && disY > 0
            DIRECTION_BOTTOM_2_TOP -> bottom2TopDirection(disY)
            DIRECTION_BOTH -> (!target.canScrollVertically(DIRECTION_TOP_2_BOTTOM) && disY > 0) || bottom2TopDirection(disY)
            else -> false
        }
    }

    private fun bottom2TopDirection(disY: Float) =
        isParentBottom2TopEnable || (!target.canScrollVertically(DIRECTION_BOTTOM_2_TOP) && disY < 0)

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

        const val FLING_MIN_VELOCITY = 3500
    }

    interface Callback {
        fun onScroll(dis: Float)
        fun onRelease(fling: Int)
    }
}
