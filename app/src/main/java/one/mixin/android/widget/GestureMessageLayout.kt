package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat

class GestureMessageLayout(context: Context, attrs: AttributeSet?) : MessageLayout(context, attrs) {
    var listener: GestureDetector.SimpleOnGestureListener? = null

    private val gestureDetector by lazy {
        GestureDetectorCompat(context, listener)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }
}
