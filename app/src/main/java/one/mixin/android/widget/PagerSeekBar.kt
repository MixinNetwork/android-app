package one.mixin.android.widget

import android.content.Context
import android.support.v7.widget.AppCompatSeekBar
import android.util.AttributeSet
import android.view.MotionEvent

class PagerSeekBar : AppCompatSeekBar {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> parent.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> parent.requestDisallowInterceptTouchEvent(false)
        }
        return super.onTouchEvent(event)
    }
}
