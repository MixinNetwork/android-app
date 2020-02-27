package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.min
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.dpToPx

class FloatingLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle) {
    private var mode: Mode = Mode.MIN
        set(value) {
            field = value
            val rv = (getChildAt(POS_RECYCLER_VIEW) as RecyclerView)
            rv.suppressLayout(value == Mode.PART)
        }

    private var lastY = 0f

    private val minHeight = context.dpToPx(180f)
    private val itemHeight = context.dpToPx(60f)

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var itemClickEnable = true

    override fun onFinishInflate() {
        super.onFinishInflate()
        val child = getChildAt(POS_RECYCLER_VIEW) as RecyclerView
        child.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return !itemClickEnable
            }
        })
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val child = getChildAt(POS_RECYCLER_VIEW) as RecyclerView
        val itemCount = child.adapter?.itemCount ?: 0
        val recyclerViewMaxHeight = itemCount * itemHeight
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val moveY = ev.y - lastY
                if ((moveY > 0 && mode == Mode.MAX && child.canScrollVertically(-1)) ||
                    (moveY < 0 && mode == Mode.MAX)) {
                    return false
                }
                if (abs(moveY) > 0) {
                    mode = Mode.PART
                }
                if (abs(moveY) > touchSlop) {
                    itemClickEnable = false
                }
                lastY = ev.y
                if (mode == Mode.PART) {
                    child.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = (height - moveY).toInt()
                        if (height > recyclerViewMaxHeight) {
                            height = recyclerViewMaxHeight
                            return super.onInterceptTouchEvent(ev)
                        }
                        val otherViewHeight = getOtherViewHeight()
                        val maxHeight = this@FloatingLayout.height - otherViewHeight
                        if (height >= maxHeight) {
                            height = maxHeight
                            mode = Mode.MAX
                        } else if (itemCount > 2 && height < minHeight) {
                            height = minHeight
                        } else if (itemCount <= 2 && height < itemCount * itemHeight) {
                            height = itemCount * itemHeight
                        }
                        // Some device not refresh UI if not call this, like smartisan OS
                        child.parent.requestLayout()
                    }
                } else if (mode == Mode.MAX) {
                    if (moveY > 0 && !child.canScrollVertically(-1)) {
                        mode = Mode.PART
                    }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (!itemClickEnable) {
                    itemClickEnable = true
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (mode == Mode.MAX && getChildAt(POS_RECYCLER_VIEW).height < height) {
            mode = Mode.PART
        }
    }

    fun showMention(itemCount: Int) {
        mode = Mode.MIN
        val child = getChildAt(POS_RECYCLER_VIEW) as RecyclerView
        if (itemCount > 2) {
            child.animateHeight(0, minHeight)
        } else {
            child.animateHeight(0, itemCount * itemHeight)
        }
    }

    fun hideMention() {
        mode = Mode.MIN
        val child = getChildAt(POS_RECYCLER_VIEW)
        child.animateHeight(child.height, 0)
    }

    fun animate2RightHeight(itemCount: Int) {
        val child = getChildAt(POS_RECYCLER_VIEW) as RecyclerView
        child.isVisible = true
        if (mode == Mode.MIN) {
            if (itemCount > 2) {
                child.animateHeight(child.height, minHeight)
            } else {
                child.animateHeight(child.height, itemCount * itemHeight)
            }
        } else {
            child.animateHeight(child.height, min(itemCount * itemHeight, height))
        }
    }

    private fun getOtherViewHeight(): Int {
        var otherViewHeight = 0
        children.forEachIndexed { index, view ->
            if (index != POS_RECYCLER_VIEW && view.isVisible) {
                otherViewHeight += view.height + view.marginTop + view.marginBottom
            }
        }
        return otherViewHeight
    }

    enum class Mode {
        MIN,
        PART,
        MAX
    }

    companion object {
        const val POS_RECYCLER_VIEW = 1
    }
}
