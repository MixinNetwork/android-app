package one.mixin.android.widget.linktext

import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.view.MotionEvent
import android.widget.TextView

internal class LinkTouchMovementMethod : LinkMovementMethod() {
    private var pressedSpan: TouchableSpan? = null

    override fun onTouchEvent(
        textView: TextView,
        spannable: Spannable,
        event: MotionEvent
    ): Boolean {
        when (val action = event.action) {
            MotionEvent.ACTION_DOWN -> {
                pressedSpan = getPressedSpan(textView, spannable, event)
                pressedSpan?.let { pressedSpan ->
                    pressedSpan.setPressed(true)
                    Selection.setSelection(
                        spannable, spannable.getSpanStart(pressedSpan),
                        spannable.getSpanEnd(pressedSpan)
                    )
                    if (pressedSpan is LongTouchableSpan) {
                        pressedSpan.startLongClick()
                    }
                    record(textView)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val touchedSpan = getPressedSpan(textView, spannable, event)
                pressedSpan?.let { pressedSpan ->
                    if (touchedSpan !== pressedSpan) {
                        pressedSpan.setPressed(false)
                        this.pressedSpan = null
                        Selection.removeSelection(spannable)
                        record(textView)
                    }
                }
            }
            else -> {
                pressedSpan?.let { pressedSpan ->
                    pressedSpan.setPressed(false)
                    record(textView)
                    if (pressedSpan is LongTouchableSpan && (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP)) {
                        pressedSpan.cancelLongClick()
                    }
                }
                pressedSpan = null
                Selection.removeSelection(spannable)
                super.onTouchEvent(textView, spannable, event)
            }
        }
        return true
    }

    private fun record(textView: TextView) {
        if (textView is AutoLinkTextView) {
            textView.clickTime = System.currentTimeMillis()
        }
    }

    private fun getPressedSpan(
        textView: TextView,
        spannable: Spannable,
        event: MotionEvent
    ): TouchableSpan? {
        var x = event.x.toInt()
        var y = event.y.toInt()
        x -= textView.totalPaddingLeft
        y -= textView.totalPaddingTop
        x += textView.scrollX
        y += textView.scrollY
        val layout = textView.layout
        val verticalLine = layout.getLineForVertical(y)
        val horizontalOffset = layout.getOffsetForHorizontal(verticalLine, x.toFloat())
        val link = spannable.getSpans(horizontalOffset, horizontalOffset, TouchableSpan::class.java)
        var touchedSpan: TouchableSpan? = null
        if (link.isNotEmpty()) {
            touchedSpan = link[0]
        }
        return touchedSpan
    }
}
