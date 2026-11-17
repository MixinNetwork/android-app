package one.mixin.android.util

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.getSystemService

private var highPerformanceDevice: Boolean? = null

@Synchronized
fun isHighPerformanceDevice(context: Context): Boolean {
    highPerformanceDevice?.let { return it }
    val activityManager = context.getSystemService<ActivityManager>()
    val result = if (activityManager == null || activityManager.isLowRamDevice) {
        false
    } else {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        memoryInfo.totalMem >= 6L * 1024 * 1024 * 1024
    }
    highPerformanceDevice = result
    return result
}
