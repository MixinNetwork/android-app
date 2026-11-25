package one.mixin.android.ui.auth

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_APP_AUTH
import one.mixin.android.Constants.Account.PREF_APP_ENTER_BACKGROUND
import one.mixin.android.extension.putLong
import one.mixin.android.util.FlagSecureReason

internal class AppAuthManager(
    private val preferences: SharedPreferences,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private class TrackedDialog(
        val host: FragmentActivity,
        val window: Window,
        val reason: FlagSecureReason,
    )

    private val secureReasons = mutableSetOf<FlagSecureReason>()
    private val activitySecureReasons = mutableMapOf<Activity, FlagSecureReason>()
    private val fragmentCallbacks = mutableMapOf<FragmentActivity, FragmentManager.FragmentLifecycleCallbacks>()
    private val dialogFragmentReasons = mutableMapOf<DialogFragment, TrackedDialog>()
    private val startedActivities = mutableSetOf<Activity>()
    private val authActivities = mutableSetOf<Activity>()
    private val pendingTasks = mutableSetOf<Int>()
    private var backgroundElapsedRealtime: Long? = null
    private var foregroundChecked = false

    @Volatile
    var isLocked = false
        private set

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == PREF_APP_AUTH) {
                invalidateSecureWindows()
            }
        }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    fun isEnabled(): Boolean = preferences.getInt(PREF_APP_AUTH, -1) != -1

    fun attachSecureWindow(window: Window?): FlagSecureReason? {
        if (window == null) return null
        val reason = FlagSecureReason(window) { isEnabled() }
        secureReasons.add(reason)
        reason.attach()
        return reason
    }

    fun detachSecureWindow(reason: FlagSecureReason?) {
        if (reason == null) return
        secureReasons.remove(reason)
        reason.detach()
    }

    fun onActivityCreated(activity: Activity) {
        val reason = activitySecureReasons[activity]
        if (reason == null) {
            attachSecureWindow(activity.window)?.let { activitySecureReasons[activity] = it }
        } else {
            reason.invalidate()
        }
        if (activity is FragmentActivity && activity !in fragmentCallbacks) {
            val callback =
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentViewCreated(
                        fm: FragmentManager,
                        f: Fragment,
                        v: View,
                        savedInstanceState: Bundle?,
                    ) {
                        syncDialogFragmentWindow(activity, f)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onFragmentActivityCreated(
                        fm: FragmentManager,
                        f: Fragment,
                        savedInstanceState: Bundle?,
                    ) {
                        syncDialogFragmentWindow(activity, f)
                    }

                    override fun onFragmentStarted(
                        fm: FragmentManager,
                        f: Fragment,
                    ) {
                        syncDialogFragmentWindow(activity, f)
                    }

                    override fun onFragmentViewDestroyed(
                        fm: FragmentManager,
                        f: Fragment,
                    ) {
                        detachDialogFragmentWindow(f)
                    }

                    override fun onFragmentDestroyed(
                        fm: FragmentManager,
                        f: Fragment,
                    ) {
                        detachDialogFragmentWindow(f)
                    }

                    override fun onFragmentDetached(
                        fm: FragmentManager,
                        f: Fragment,
                    ) {
                        detachDialogFragmentWindow(f)
                    }
                }
            fragmentCallbacks[activity] = callback
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(callback, true)
        }
    }

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
        activitySecureReasons.remove(activity)?.let(::detachSecureWindow)
        if (activity is FragmentActivity) {
            fragmentCallbacks.remove(activity)?.let(activity.supportFragmentManager::unregisterFragmentLifecycleCallbacks)
            val iterator = dialogFragmentReasons.entries.iterator()
            while (iterator.hasNext()) {
                val tracked = iterator.next().value
                if (tracked.host === activity) {
                    iterator.remove()
                    detachSecureWindow(tracked.reason)
                }
            }
        }
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

    private fun syncDialogFragmentWindow(
        host: FragmentActivity,
        fragment: Fragment,
    ) {
        if (fragment !is DialogFragment) return
        val window = fragment.dialog?.window ?: return
        val existing = dialogFragmentReasons[fragment]
        if (existing != null) {
            if (existing.window === window) {
                existing.reason.invalidate()
                return
            }
            dialogFragmentReasons.remove(fragment)
            detachSecureWindow(existing.reason)
        }
        attachSecureWindow(window)?.let { reason ->
            dialogFragmentReasons[fragment] = TrackedDialog(host, window, reason)
        }
    }

    private fun detachDialogFragmentWindow(fragment: Fragment) {
        if (fragment !is DialogFragment) return
        dialogFragmentReasons.remove(fragment)?.reason?.let(::detachSecureWindow)
    }

    private fun invalidateSecureWindows() {
        val mainLooper = Looper.getMainLooper()
        if (Looper.myLooper() == mainLooper) {
            secureReasons.toList().forEach(FlagSecureReason::invalidate)
        } else {
            Handler(mainLooper).post {
                secureReasons.toList().forEach(FlagSecureReason::invalidate)
            }
        }
    }

    private fun lock() {
        isLocked = true
        // Keep the legacy timestamp format; zero requires authentication after process death.
        preferences.putLong(PREF_APP_ENTER_BACKGROUND, 0)
    }
}
