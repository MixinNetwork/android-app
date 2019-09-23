package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.min
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

    private val minHeight = context.dpToPx(180f)
    private val itemHeight = context.dpToPx(60f)

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var itemClickEnable = true

    override fun onFinishInflate() {
        super.onFinishInflate()
        val child = getChildAt(0) as RecyclerView
        child.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return !itemClickEnable
            }
        })
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val child = getChildAt(0) as RecyclerView
        val itemCount = child.adapter?.itemCount ?: 0
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val moveY = ev.y - lastY
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
                        if (height >= itemCount * itemHeight) {
                            height = itemCount * itemHeight
                            mode = Mode.MAX
                        } else if (itemCount > 2 && height < minHeight) {
                            height = minHeight
                        } else if (itemCount <= 2 && height < itemCount * itemHeight) {
                            height = itemCount * itemHeight
                        }
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
        if (mode == Mode.MAX && getChildAt(0).height < height) {
            mode = Mode.PART
        }
    }

    fun show() {
        mode = Mode.MIN
        val child = getChildAt(0) as RecyclerView
        val itemCount = child.adapter?.itemCount ?: 0
        if (itemCount > 2) {
            child.animateHeight(0, minHeight)
        } else {
            child.animateHeight(0, itemCount * itemHeight)
        }
        isVisible = true
    }

    fun hide() {
        mode = Mode.MIN
        val child = getChildAt(0)
        child.animateHeight(child.height, 0, onEndAction = {
            isGone = true
        })
    }

    fun animate2RightHeight(itemCount: Int) {
        val child = getChildAt(0) as RecyclerView
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

    enum class Mode {
        MIN,
        PART,
        MAX
    }
}
