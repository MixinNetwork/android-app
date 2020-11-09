package one.mixin.android.widget

import android.os.Handler
import android.os.SystemClock
import android.view.View

abstract class DebugClickListener : View.OnClickListener {
    private var isSingleEvent = false
    private var timestampLastClick: Long
    private var clickCount: Int = 0
    private val handler: Handler
    private val runnable: Runnable
    override fun onClick(v: View) {
        if (SystemClock.elapsedRealtime() - timestampLastClick < DEBUG_INTERVAL) {
            isSingleEvent = false
            clickCount++
            handler.removeCallbacks(runnable)
            if (clickCount >= 5) {
                clickCount = 0
                onDebugClick()
            }
        } else {
            clickCount = 1
            isSingleEvent = true
            handler.postDelayed(runnable, CLICK_INTERVAL)
            timestampLastClick = SystemClock.elapsedRealtime()
        }
    }

    abstract fun onDebugClick()
    abstract fun onSingleClick()

    companion object {
        private const val CLICK_INTERVAL = 300L
        private const val DEBUG_INTERVAL = 1500L
    }

    init {
        timestampLastClick = 0
        handler = Handler()
        runnable = Runnable {
            if (isSingleEvent) {
                onSingleClick()
            }
        }
    }
}