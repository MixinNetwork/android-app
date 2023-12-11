package one.mixin.android.util

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.content.Context
import android.database.CursorWindow
import androidx.core.content.getSystemService
import timber.log.Timber
import java.lang.reflect.Field
import kotlin.math.max
import kotlin.math.min

class CursorWindowFixer {
    companion object {
        @SuppressLint("DiscouragedPrivateApi")
        fun fix(context: Context) {
            try {
                val field: Field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
                field.isAccessible = true
                field.set(null, getCursorWindowSize(context) * 1024 * 1024)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        private fun getCursorWindowSize(context: Context): Int {
            val activityManager = context.getSystemService<ActivityManager>() ?: return 10
            val memoryInfo = MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            val total = memoryInfo.totalMem / 1024 / 1024 / 1024
            if (total < 6) { // for device memory less than 6GB, use 10MB for cursor window size
                return 10
            }
            val memorySize = max(total, 2)
            return min(100, (memorySize * 12.5).toInt()) // 8G Memory set window size to 100MB, 1G Memory set to 12.5MB
        }
    }
}
