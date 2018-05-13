package one.mixin.android.widget

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView
import android.util.DisplayMetrics
import one.mixin.android.R
import one.mixin.android.extension.notNullElse

class SmoothScrollLinearLayoutManager(
    context: Context,
    @RecyclerView.Orientation orientation: Int,
    reverseLayout: Boolean
) : LinearLayoutManager(context, orientation, reverseLayout) {

    companion object {
        const val FAST_SPEED = 50f
        const val MEDIUM_SPEED = 85f
        const val SLOW_SPEED = 120f
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State?, position: Int) {

        val smoothScroller = object : LinearSmoothScroller(recyclerView.context) {
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                val speed = notNullElse(recyclerView.getTag(R.id.speed_tag), { it as Float }, FAST_SPEED)
                return speed / displayMetrics.densityDpi / Math.abs(findFirstVisibleItemPosition() - position).run {
                    if (this <= 0) {
                        1
                    } else {
                        this
                    }
                }
            }
        }

        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }
}