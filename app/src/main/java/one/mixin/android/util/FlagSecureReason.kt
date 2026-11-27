package one.mixin.android.util

import android.view.Window
import android.view.WindowManager
import java.util.WeakHashMap

class FlagSecureReason(
    private val window: Window?,
    private val condition: () -> Boolean,
) {
    private var attached = false
    private var active = false

    fun attach() {
        if (attached) return
        attached = true
        invalidate()
    }

    fun detach() {
        if (!attached) return
        attached = false
        invalidate()
    }

    fun invalidate() {
        val next = attached && condition()
        if (next != active) {
            active = next
            update(window, if (next) 1 else -1)
        }
    }

    companion object {
        private val reasons = WeakHashMap<Window, Int>()

        private fun update(window: Window?, delta: Int) {
            if (window == null) return
            val current = reasons[window] ?: 0
            val next = (current + delta).coerceAtLeast(0)
            if (next == 0) {
                reasons.remove(window)
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                reasons[window] = next
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
