package one.mixin.android.widget

import android.os.Handler
import android.os.SystemClock
import android.view.View

abstract class DebugClickHandler {

    private var isSingleEvent = false
    private var timestampLastClick: Long
    private var clickCount: Int = 0
    private val handler: Handler
    private val runnable: Runnable

    protected abstract fun onDebugClick()
    protected abstract fun onSingleClick()

    fun onClick() {
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

abstract class DebugClickListener : DebugClickHandler(), View.OnClickListener {

    override fun onClick(v: View) {
        onClick()
    }

}
