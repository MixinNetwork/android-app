package one.mixin.android.ui.auth

import android.app.Activity
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_APP_AUTH
import one.mixin.android.Constants.Account.PREF_APP_ENTER_BACKGROUND
import one.mixin.android.extension.putLong
import one.mixin.android.ui.url.UrlInterpreterActivity
import java.lang.ref.WeakReference

internal class AppAuthManager(
    private val preferences: SharedPreferences,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val taskIdOf: (Activity) -> Int = Activity::getTaskId,
) {
    companion object {
        private const val DEFERRED_BACKGROUND_TIMEOUT_MS = 700L
    }

    private val createdActivities = mutableSetOf<Activity>()
    private val startedActivities = mutableSetOf<Activity>()
    private val authActivities = mutableSetOf<Activity>()
    private val startedAuthActivities = mutableSetOf<Activity>()
    private val resumedAuthActivities = mutableSetOf<Activity>()
    private val launchingAuthTasks = mutableMapOf<Int, WeakReference<Activity>>()
    private val activeAuthTasks = mutableSetOf<Int>()
    private val cancelledAuthTasks = mutableSetOf<Int>()
    private var backgroundElapsedRealtime: Long? = null
    private var inForeground = false
    private var foregroundChecked = false
    private var authenticatedInForeground = false
    private val pendingDeepLinks = linkedMapOf<Int, Uri>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var awaitingPostAuthActivity = false
    private var pendingBackgroundElapsed: Long? = null
    private var pendingBackgroundWallTime: Long? = null
    private val deferredBackgroundRunnable = Runnable { completeDeferredBackground() }

    var lockEpoch: Long = 0L
        private set

    @Volatile
    var isLocked = false
        private set

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == PREF_APP_AUTH) {
                syncAllRecentsScreenshots()
            }
        }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    fun isEnabled(): Boolean = preferences.getInt(PREF_APP_AUTH, -1) != -1

    fun recordPendingDeepLink(taskId: Int, uri: Uri?) {
        if (uri != null) {
            pendingDeepLinks.remove(taskId)
            pendingDeepLinks[taskId] = uri
        }
    }

    fun onActivityCreated(activity: Activity) {
        createdActivities.add(activity)
        syncRecentsScreenshot(activity)
        if (activity is AppAuthActivity) {
            authActivities.add(activity)
        }
        if (activity !is UrlInterpreterActivity) {
            launchingAuthTasks.remove(taskIdOf(activity))
        }
    }

    fun onActivityStarted(activity: Activity) {
        cancelDeferredBackground()
        awaitingPostAuthActivity = false
        if (startedActivities.isEmpty()) {
            inForeground = true
        }
        startedActivities.add(activity)
        if (activity is AppAuthActivity) {
            val taskId = taskIdOf(activity)
            if (!isEnabled() || authenticatedInForeground) {
                startedActivities.remove(activity)
                authActivities.remove(activity)
                startedAuthActivities.remove(activity)
                resumedAuthActivities.remove(activity)
                launchingAuthTasks.remove(taskId)
                if (!isEnabled()) {
                    isLocked = false
                    foregroundChecked = true
                    authenticatedInForeground = false
                    activeAuthTasks.clear()
                    cancelledAuthTasks.clear()
                    pendingDeepLinks.clear()
                }
                activity.finish()
                return
            }
            authActivities.add(activity)
            startedAuthActivities.add(activity)
            launchingAuthTasks.remove(taskId)
            activeAuthTasks.add(taskId)
            cancelledAuthTasks.remove(taskId)
            if (activity.intent?.data != null && taskId !in pendingDeepLinks) {
                recordPendingDeepLink(taskId, activity.intent?.data)
            }
            lock()
        } else {
            checkAndShow(activity)
        }
    }

    fun onActivityResumed(activity: Activity) {
        if (activity is AppAuthActivity) {
            resumedAuthActivities.add(activity)
            launchingAuthTasks.remove(taskIdOf(activity))
        } else if (isLocked && shouldRelockOnResume(activity)) {
            checkAndShowInternal(activity, fromResume = true)
        }
    }

    fun onActivityPaused(activity: Activity) {
        resumedAuthActivities.remove(activity)
    }

    fun onActivityStopped(activity: Activity) {
        startedAuthActivities.remove(activity)
        resumedAuthActivities.remove(activity)
        startedActivities.remove(activity)
        startedActivities.removeAll { it.isDestroyed }
        if (!inForeground || startedActivities.isNotEmpty()) return
        if (activity.isChangingConfigurations || awaitingPostAuthActivity) {
            scheduleDeferredBackground()
        } else {
            enterBackground()
        }
    }

    private fun scheduleDeferredBackground() {
        if (pendingBackgroundElapsed != null) return
        pendingBackgroundElapsed = elapsedRealtime()
        pendingBackgroundWallTime = currentTimeMillis()
        mainHandler.postDelayed(deferredBackgroundRunnable, DEFERRED_BACKGROUND_TIMEOUT_MS)
    }

    private fun cancelDeferredBackground() {
        mainHandler.removeCallbacks(deferredBackgroundRunnable)
        pendingBackgroundElapsed = null
        pendingBackgroundWallTime = null
    }

    private fun completeDeferredBackground() {
        val elapsed = pendingBackgroundElapsed ?: elapsedRealtime()
        val wallTime = pendingBackgroundWallTime ?: currentTimeMillis()
        cancelDeferredBackground()
        if (inForeground && startedActivities.isEmpty()) enterBackground(elapsed, wallTime)
    }

    fun onUiHidden() {
        runOnMain {
            if (pendingBackgroundElapsed != null && inForeground && startedActivities.isEmpty()) completeDeferredBackground()
        }
    }

    private fun enterBackground(backgroundElapsed: Long = elapsedRealtime(), backgroundWallTime: Long = currentTimeMillis()) {
        inForeground = false
        foregroundChecked = false
        authenticatedInForeground = false
        awaitingPostAuthActivity = false
        lockEpoch++
        backgroundElapsedRealtime = backgroundElapsed
        launchingAuthTasks.clear()
        activeAuthTasks.clear()
        cancelledAuthTasks.clear()
        if (isLocked) {
            pendingDeepLinks.clear()
        } else {
            preferences.putLong(PREF_APP_ENTER_BACKGROUND, backgroundWallTime)
        }
    }

    fun onActivityDestroyed(activity: Activity) {
        createdActivities.remove(activity)
        startedActivities.remove(activity)
        startedAuthActivities.remove(activity)
        resumedAuthActivities.remove(activity)
        authActivities.remove(activity)
        val taskId = taskIdOf(activity)
        if (activity is AppAuthActivity || launchingAuthTasks[taskId]?.get() === activity) {
            launchingAuthTasks.remove(taskId)
        }
    }

    fun onAuthCancelled(activity: Activity, ownTask: Boolean = true): Boolean {
        val taskId = taskIdOf(activity)
        activeAuthTasks.remove(taskId)
        launchingAuthTasks.remove(taskId)
        if (!ownTask) return false
        cancelledAuthTasks.add(taskId)
        return cancelledAuthTasks.size > 1
    }

    fun checkAndShow(activity: Activity): Boolean = checkAndShowInternal(activity, fromResume = false)

    private fun checkAndShowInternal(
        activity: Activity,
        fromResume: Boolean,
    ): Boolean {
        if (activity is AppAuthActivity) return isLocked
        val mode = preferences.getInt(PREF_APP_AUTH, -1)
        if (mode == -1) {
            isLocked = false
            foregroundChecked = true
            authenticatedInForeground = false
            return false
        }
        if (authenticatedInForeground) return false
        val isActivityClosing = activity.isFinishing || activity.isDestroyed
        if (!isLocked && !foregroundChecked) {
            if (!isActivityClosing && activity in startedActivities) {
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
        if (isActivityClosing) return true
        if (activity is UrlInterpreterActivity) {
            recordPendingDeepLink(taskIdOf(activity), activity.intent?.data)
        }
        if (shouldLaunchAuthForTask(activity, fromResume)) {
            val taskId = taskIdOf(activity)
            activeAuthTasks.add(taskId)
            cancelledAuthTasks.remove(taskId)
            if (activity !is UrlInterpreterActivity) {
                launchingAuthTasks[taskId] = WeakReference(activity)
            }
            AppAuthActivity.show(activity)
        }
        return true
    }

    private fun shouldRelockOnResume(activity: Activity): Boolean {
        val taskId = taskIdOf(activity)
        return taskId !in cancelledAuthTasks && taskId in activeAuthTasks
    }

    private fun shouldLaunchAuthForTask(
        activity: Activity,
        fromResume: Boolean,
    ): Boolean {
        val taskId = taskIdOf(activity)
        if (activity !is UrlInterpreterActivity) {
            if (resumedAuthActivities.any { taskIdOf(it) == taskId && !it.isFinishing && !it.isDestroyed }) {
                return false
            }
            if (!fromResume && startedAuthActivities.any { taskIdOf(it) == taskId && !it.isFinishing && !it.isDestroyed }) {
                return false
            }
        }
        val initiator = launchingAuthTasks[taskId]?.get()
        if (initiator == null || initiator.isFinishing || initiator.isDestroyed) {
            launchingAuthTasks.remove(taskId)
            return true
        }
        // A pending launch only covers the paired onStart -> onResume; the first resume consumes it so a
        // swallowed launch (or one that hit a finishing lock screen) is retried on the next resume or start.
        if (fromResume) {
            launchingAuthTasks.remove(taskId)
        }
        return false
    }

    fun onAuthenticated(taskId: Int? = null, expectNextActivity: Boolean = false): Uri? {
        isLocked = false
        foregroundChecked = true
        authenticatedInForeground = true
        backgroundElapsedRealtime = null
        launchingAuthTasks.clear()
        activeAuthTasks.clear()
        cancelledAuthTasks.clear()
        startedAuthActivities.clear()
        resumedAuthActivities.clear()
        preferences.putLong(PREF_APP_ENTER_BACKGROUND, 0)
        val deepLink = taskId?.let { pendingDeepLinks[it] } ?: pendingDeepLinks.values.lastOrNull()
        pendingDeepLinks.clear()
        awaitingPostAuthActivity = deepLink != null || expectNextActivity
        authActivities.forEach { it.finish() }
        startedActivities.removeAll { it in authActivities }
        authActivities.clear()
        return deepLink
    }

    fun reset() {
        runOnMain {
            isLocked = false
            foregroundChecked = false
            authenticatedInForeground = false
            backgroundElapsedRealtime = null
            launchingAuthTasks.clear()
            activeAuthTasks.clear()
            cancelledAuthTasks.clear()
            cancelDeferredBackground()
            awaitingPostAuthActivity = false
            pendingDeepLinks.clear()
            lockEpoch++
            preferences.putLong(PREF_APP_ENTER_BACKGROUND, 0)
            startedAuthActivities.clear()
            authActivities.forEach { it.finish() }
            startedActivities.removeAll { it in authActivities }
            authActivities.clear()
            resumedAuthActivities.clear()
            createdActivities.toList().forEach(::syncRecentsScreenshot)
        }
    }

    private fun syncRecentsScreenshot(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.setRecentsScreenshotEnabled(!isEnabled())
        }
    }

    private fun syncAllRecentsScreenshots() {
        runOnMain {
            createdActivities.toList().forEach(::syncRecentsScreenshot)
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == mainHandler.looper) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun lock() {
        isLocked = true
        // Keep the legacy timestamp format; zero requires authentication after process death.
        preferences.putLong(PREF_APP_ENTER_BACKGROUND, 0)
    }
}
