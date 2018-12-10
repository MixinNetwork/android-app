package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView
import org.jetbrains.anko.dip

class DragWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private val dp1 = context.dip(1)

    private var tempY = 0f
    private var handled = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (event.action == MotionEvent.ACTION_DOWN) {
            tempY = event.rawY
            handled = false
            super.onTouchEvent(event)
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            val dis = event.rawY - tempY
            tempY = event.rawY
            if (dis > 0 && canScrollVertically(-dis.toInt())) {
                super.onTouchEvent(event)
            } else {
                when {
                    dis == 0f -> {
                        true
                    }
                    onDragListener.onScroll(dis) -> {
                        if (!handled) {
                            handled = (dis > dp1 || dis < -dp1)
                        }
                        true
                    }
                    else -> {
                        super.onTouchEvent(event)
                    }
                }
            }
        } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            onDragListener.onUp()
            if (handled) {
                super.onTouchEvent(MotionEvent.obtain(event).apply {
                    action = MotionEvent.ACTION_CANCEL
                })
                return true
            } else {
                super.onTouchEvent(event)
            }
        } else {
            super.onTouchEvent(event)
        }
    }

    private lateinit var onDragListener: OnDragListener

    fun setOnScrollListener(onDragListener: OnDragListener) {
        this.onDragListener = onDragListener
    }

    interface OnDragListener {
        fun onScroll(disY: Float): Boolean
        fun onUp()
    }
}
