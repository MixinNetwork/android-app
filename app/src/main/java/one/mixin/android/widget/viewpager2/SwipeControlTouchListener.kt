package one.mixin.android.widget.viewpager2

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class SwipeControlTouchListener : RecyclerView.OnItemTouchListener {
    private var initialXValue = 0f
    private var initialYValue = 0f
    var direction: SwipeDirection = SwipeDirection.ALL

    fun setSwipeDirection(direction: SwipeDirection) {
        this.direction = direction
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
        return !isSwipeAllowed(event)
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

    private fun isSwipeAllowed(event: MotionEvent): Boolean {
        if (direction === SwipeDirection.ALL) return true
        if (direction == SwipeDirection.NONE) // disable any swipe
            return false
        if (event.action == MotionEvent.ACTION_DOWN) {
            initialXValue = event.x
            initialYValue = event.y
            return true
        }
        if (event.action == MotionEvent.ACTION_MOVE) {
            try {
                val diffX: Float = event.x - initialXValue
                val diffY: Float = event.y - initialYValue
                if (diffX > 0 && abs(diffX) > abs(diffY) && direction == SwipeDirection.RIGHT) {
                    // swipe from left to right detected
                    return false
                } else if (diffX < 0 && abs(diffX) > abs(diffY) && direction == SwipeDirection.LEFT) {
                    // swipe from right to left detected
                    return false
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
        return true
    }
}

enum class SwipeDirection {
    ALL, LEFT, RIGHT, NONE
}
