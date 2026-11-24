package one.mixin.android.ui.auth

import android.app.Activity
import android.content.SharedPreferences
import android.os.SystemClock
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_APP_AUTH
import one.mixin.android.Constants.Account.PREF_APP_ENTER_BACKGROUND
import one.mixin.android.extension.putLong

internal class AppAuthManager(
    private val preferences: SharedPreferences,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val startedActivities = mutableSetOf<Activity>()
    private val authActivities = mutableSetOf<Activity>()
    private val pendingTasks = mutableSetOf<Int>()
    private var backgroundElapsedRealtime: Long? = null
    private var foregroundChecked = false

    @Volatile
    var isLocked = false
        private set

    fun onActivityStarted(activity: Activity) {
        startedActivities.add(activity)
        if (activity is AppAuthActivity) {
            authActivities.add(activity)
            pendingTasks.remove(activity.taskId)
            lock()
        } else {
            checkAndShow(activity)
        }
    }

    fun onActivityResumed(activity: Activity) {
        if (activity is AppAuthActivity) {
            pendingTasks.remove(activity.taskId)
        } else if (isLocked) {
            checkAndShow(activity)
        }
    }

    fun onActivityStopped(activity: Activity) {
        if (!startedActivities.remove(activity) || startedActivities.isNotEmpty() || activity.isChangingConfigurations) return
        foregroundChecked = false
        backgroundElapsedRealtime = elapsedRealtime()
        if (!isLocked) {
            preferences.putLong(PREF_APP_ENTER_BACKGROUND, currentTimeMillis())
        }
    }

    fun onActivityDestroyed(activity: Activity) {
        startedActivities.remove(activity)
        authActivities.remove(activity)
        if (activity is AppAuthActivity) pendingTasks.remove(activity.taskId)
    }

    fun checkAndShow(activity: Activity): Boolean {
        if (activity is AppAuthActivity || activity.isFinishing || activity.isDestroyed) return isLocked
        val mode = preferences.getInt(PREF_APP_AUTH, -1)
        if (mode == -1) {
            isLocked = false
            foregroundChecked = true
            return false
        }
        if (!isLocked && !foregroundChecked) {
            if (activity in startedActivities) {
                foregroundChecked = true
            }
            val backgroundTime = preferences.getLong(PREF_APP_ENTER_BACKGROUND, 0)
            val elapsed = backgroundElapsedRealtime?.let { elapsedRealtime() - it }
                ?: (currentTimeMillis() - backgroundTime)
            val timeout = if (mode == 1) Constants.INTERVAL_1_MIN else Constants.INTERVAL_30_MINS
            if (mode == 0 || backgroundTime == 0L || elapsed < 0 || elapsed > timeout) {
                lock()
            }
        }
        if (!isLocked) return false
        if (pendingTasks.add(activity.taskId)) {
            AppAuthActivity.show(activity)
        }
        return true
    }

    fun onAuthenticated() {
        isLocked = false
        foregroundChecked = true
        backgroundElapsedRealtime = null
        pendingTasks.clear()
        preferences.putLong(PREF_APP_ENTER_BACKGROUND, 0)
        authActivities.forEach { it.finish() }
        authActivities.clear()
    }

    private fun lock() {
        isLocked = true
        // Keep the legacy timestamp format; zero requires authentication after process death.
        preferences.putLong(PREF_APP_ENTER_BACKGROUND, 0)
    }
}
