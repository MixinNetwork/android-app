package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView

class DragWebView : WebView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    private var tempY = 0f
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (event.action == MotionEvent.ACTION_DOWN) {
            tempY = event.rawY
            super.onTouchEvent(event)
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            val dis = event.rawY - tempY
            tempY = event.rawY
            if (dis > 0 && canScrollVertically(-dis.toInt())) {
                super.onTouchEvent(event)
            } else if (!onDragListener.onScroll(dis)) {
                super.onTouchEvent(event)
            } else {
                true
            }
        } else {
            onDragListener.onUp()
            super.onTouchEvent(event)
        }
    }

    private lateinit var onDragListener: OnDragListener

    fun setOnScrollListener(onDargListener: OnDragListener) {
        this.onDragListener = onDargListener
    }

    interface OnDragListener {
        fun onScroll(disY: Float): Boolean
        fun onUp()
    }
}
