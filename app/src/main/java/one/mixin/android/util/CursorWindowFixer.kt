package one.mixin.android.util

import android.database.CursorWindow
import timber.log.Timber
import java.lang.reflect.Field

class CursorWindowFixer {
    companion object {
        fun fix() {
            try {
                val field: Field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
                field.isAccessible = true
                field.set(null, 100 * 1024 * 1024) // the 100MB is the new size
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}
