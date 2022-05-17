package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat

class GestureMessageLayout(context: Context, attrs: AttributeSet?) : MessageLayout(context, attrs) {
    var listener: GestureDetector.SimpleOnGestureListener? = null
        set(value) {
            if (field != null) {
                field = value

                if (value != null) {
                    GestureDetectorCompat(context, value)
                }
            }
        }

    private var gestureDetector: GestureDetectorCompat? = null

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector?.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }
}
