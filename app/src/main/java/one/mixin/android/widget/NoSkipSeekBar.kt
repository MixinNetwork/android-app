package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar

class NoSkipSeekBar constructor(context: Context, attrs: AttributeSet) : AppCompatSeekBar(context, attrs) {

    var isDragging = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> isDragging = true
            MotionEvent.ACTION_UP -> isDragging = false
            MotionEvent.ACTION_MOVE -> if (!isDragging) return true
            MotionEvent.ACTION_CANCEL -> isDragging = false
        }
        return super.onTouchEvent(event)
    }
}
