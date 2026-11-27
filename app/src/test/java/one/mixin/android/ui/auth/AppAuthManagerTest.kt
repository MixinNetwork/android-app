package one.mixin.android.ui.auth

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.Constants.Account.PREF_APP_AUTH
import one.mixin.android.Constants.Account.PREF_APP_ENTER_BACKGROUND
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.FlagSecureReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppAuthManagerTest {
    private lateinit var preferences: SharedPreferences
    private var elapsed = 10_000L
    private var wallTime = 2_000_000L

    @Before
    fun setUp() {
        preferences = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("app-auth-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
    }

    @Test
    fun disabledLockDoesNotBlockEntry() {
        val manager = manager()
        val activity = activity()
        manager.onActivityStarted(activity)
        assertFalse(manager.isLocked)
        assertFalse(manager.checkAndShow(activity))
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    @Test
    fun existingTimeoutValuesAndBoundaryArePreserved() {
        for ((mode, timeout) in listOf(1 to 60_000L, 2 to 1_800_000L)) {
            configure(mode, wallTime - timeout)
            assertFalse(manager().checkAndShow(activity()))
            configure(mode, wallTime - timeout - 1)
            assertTrue(manager().checkAndShow(activity()))
        }
        configure(0, wallTime)
        assertTrue(manager().checkAndShow(activity()))
    }

    @Test
    fun immediateLockIsRequestedOnceUntilAuthenticationCompletes() {
        configure(0)
        val manager = manager()
        val activity = activity()
        manager.onActivityStarted(activity)
        assertTrue(manager.checkAndShow(activity))
        assertEquals(AppAuthActivity::class.java.name, shadowOf(activity).nextStartedActivity.component?.className)
        assertNull(shadowOf(activity).nextStartedActivity)
        manager.onAuthenticated()
        assertFalse(manager.checkAndShow(activity))
        assertFalse(manager.isLocked)
    }

    @Test
    fun internalActivityTransitionsDoNotRestartImmediateLock() {
        configure(0)
        val manager = manager()
        val first = activity()
        val second = activity()
        manager.onActivityStarted(first)
        assertNotNull(shadowOf(first).nextStartedActivity)
        manager.onAuthenticated()
        manager.onActivityStarted(second)
        manager.onActivityStopped(first)
        assertFalse(manager.checkAndShow(second))
        assertNull(shadowOf(second).nextStartedActivity)
        manager.onActivityStopped(second)
        manager.onActivityStarted(first)
        assertTrue(manager.isLocked)
    }

    @Test
    fun configurationChangeDoesNotBecomeABackgroundTransition() {
        configure(0)
        val manager = manager()
        val first = activity()
        manager.onActivityStarted(first)
        assertNotNull(shadowOf(first).nextStartedActivity)
        manager.onAuthenticated()
        first.changingConfigurations = true
        manager.onActivityStopped(first)
        manager.onActivityDestroyed(first)
        val replacement = activity()
        manager.onActivityStarted(replacement)
        assertFalse(manager.isLocked)
        assertNull(shadowOf(replacement).nextStartedActivity)
    }

    @Test
    fun timeoutUsesElapsedTimeDespiteWallClockChanges() {
        configure(1)
        val manager = manager()
        val activity = activity()
        manager.onActivityStarted(activity)
        manager.onAuthenticated()
        manager.onActivityStopped(activity)
        elapsed += 60_001
        wallTime -= 500_000
        manager.onActivityStarted(activity)
        assertTrue(manager.isLocked)
    }

    @Test
    fun movingWallClockForwardDoesNotExpireTheGracePeriod() {
        configure(1)
        val manager = manager()
        val activity = activity()
        manager.onActivityStarted(activity)
        manager.onAuthenticated()
        manager.onActivityStopped(activity)
        elapsed += 1_000
        wallTime += 5_000_000
        manager.onActivityStarted(activity)
        assertFalse(manager.isLocked)
    }

    @Test
    fun foregroundUseDoesNotConsumeBackgroundTimeout() {
        configure(1, wallTime - 1_000)
        val manager = manager()
        val activity = activity()
        manager.onActivityStarted(activity)
        elapsed += 1_800_000
        wallTime += 1_800_000
        assertFalse(manager.checkAndShow(activity))
    }

    @Test
    fun stoppingOrDestroyingLockScreenDoesNotUnlockOrResetDeadline() {
        configure(1, wallTime - 60_001)
        val manager = manager()
        val activity = activity()
        manager.onActivityStarted(activity)
        val lockActivity = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        manager.onActivityStarted(lockActivity)
        manager.onActivityStopped(activity)
        manager.onActivityStopped(lockActivity)
        manager.onActivityDestroyed(lockActivity)
        assertTrue(manager.isLocked)
        assertEquals(0L, preferences.getLong(PREF_APP_ENTER_BACKGROUND, -1))
        assertTrue(manager().checkAndShow(activity()))
    }

    @Test
    fun restoredLockScreenRemainsLockedWhenItIsTheOnlyActivity() {
        configure(1)
        val manager = manager()
        val lockActivity = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        shadowOf(lockActivity).setIsTaskRoot(true)
        manager.onActivityStarted(lockActivity)
        assertTrue(manager.isLocked)
        assertFalse(lockActivity.isFinishing)
        manager.onAuthenticated()
        assertFalse(manager.isLocked)
        assertTrue(lockActivity.isFinishing)
    }

    @Test
    fun resumedContentCannotBypassADestroyedLockScreen() {
        configure(0)
        val manager = manager()
        val activity = activity()
        manager.onActivityStarted(activity)
        shadowOf(activity).nextStartedActivity
        val lockActivity = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        manager.onActivityStarted(lockActivity)
        manager.onActivityResumed(activity)
        assertNotNull(shadowOf(activity).nextStartedActivity)
        manager.onActivityResumed(lockActivity)
        manager.onActivityDestroyed(lockActivity)
        manager.onActivityResumed(activity)
        assertNotNull(shadowOf(activity).nextStartedActivity)
        assertTrue(manager.isLocked)
    }

    @Test
    fun processRestartHonorsSavedGracePeriodButRejectsFutureTimestamp() {
        configure(1, wallTime - 30_000)
        assertFalse(manager().checkAndShow(activity()))
        configure(1, wallTime + 30_000)
        val activity = activity()
        assertTrue(manager().checkAndShow(activity))
        assertNotNull(shadowOf(activity).nextStartedActivity)
    }

    @Test
    fun earlyOnCreateCheckDuringGracePeriodDoesNotSkipSubsequentTimeoutLock() {
        configure(1, wallTime - 10_000)
        val manager = manager()
        val deepLinkActivity = activity()
        assertFalse(manager.checkAndShow(deepLinkActivity))

        wallTime += 60_000
        val mainActivity = activity()
        manager.onActivityStarted(mainActivity)
        assertTrue(manager.isLocked)
        assertNotNull(shadowOf(mainActivity).nextStartedActivity)
    }

    @Test
    @Config(sdk = [33])
    fun recentsScreenshotDisabledWhenAppAuthEnabledAndRestoredWhenDisabled() {
        configure(0)
        val manager = manager()
        val activity = activity()
        manager.onActivityCreated(activity)
        assertEquals(false, activity.recentsScreenshotEnabled)
        assertWindowSecure(activity.window, false)

        preferences.edit().putInt(PREF_APP_AUTH, -1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(true, activity.recentsScreenshotEnabled)
        assertWindowSecure(activity.window, false)

        preferences.edit().putInt(PREF_APP_AUTH, 1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(false, activity.recentsScreenshotEnabled)
        assertWindowSecure(activity.window, false)

        preferences.edit().remove(PREF_APP_AUTH).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(true, activity.recentsScreenshotEnabled)
    }

    @Test
    @Config(sdk = [33])
    fun destroyedActivityStopsReceivingRecentsScreenshotUpdates() {
        configure(0)
        val manager = manager()
        val activity = activity()
        manager.onActivityCreated(activity)
        assertEquals(listOf(false), activity.recentsScreenshotHistory)

        manager.onActivityDestroyed(activity)
        preferences.edit().putInt(PREF_APP_AUTH, -1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        preferences.edit().putInt(PREF_APP_AUTH, 1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf(false), activity.recentsScreenshotHistory)
    }

    @Test
    @Config(sdk = [31])
    fun preApi33DoesNotCallRecentsScreenshotOrSetGlobalFlagSecure() {
        configure(0)
        val manager = manager()
        val activity = activity()
        manager.onActivityCreated(activity)
        assertTrue(activity.recentsScreenshotHistory.isEmpty())
        assertWindowSecure(activity.window, false)

        preferences.edit().putInt(PREF_APP_AUTH, -1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(activity.recentsScreenshotHistory.isEmpty())
        assertWindowSecure(activity.window, false)
    }

    @Test
    @Config(sdk = [33])
    fun sensitiveScreenFlagSecureReasonWorksIndependentlyOfAppAuth() {
        configure(0)
        val manager = manager()
        val activity = activity()
        manager.onActivityCreated(activity)
        assertWindowSecure(activity.window, false)

        val sensitiveReason = FlagSecureReason(activity.window) { true }.apply { attach() }
        assertWindowSecure(activity.window, true)

        preferences.edit().putInt(PREF_APP_AUTH, -1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(activity.window, true)

        sensitiveReason.detach()
        assertWindowSecure(activity.window, false)
    }

    @Test
    fun pendingDeepLinkFromSecondTaskSurvivesUnlockFromFirstTask() {
        configure(0)
        val manager = manager()
        val mainActivity = activity(taskId = 1)
        manager.onActivityStarted(mainActivity)
        val lock1 = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        manager.onActivityStarted(lock1)

        val uri = Uri.parse("mixin://pay/cross-task")
        val urlActivity = Robolectric.buildActivity(
            UrlInterpreterActivity::class.java,
            Intent(Intent.ACTION_VIEW, uri),
        ).get()
        assertTrue(manager.checkAndShow(urlActivity))
        val lock2 = Robolectric.buildActivity(AppAuthActivity::class.java, Intent().setData(uri)).get()
        manager.onActivityStarted(lock2)

        // Switching back to the first task's lock screen must not lose the deep link from the second task.
        manager.onActivityStarted(lock1)

        assertEquals(uri, manager.onAuthenticated())
        assertTrue(lock1.isFinishing)
        assertTrue(lock2.isFinishing)
        assertNull(manager.onAuthenticated())
    }

    @Test
    fun pendingDeepLinkIsRecordedEvenWhenTaskAlreadyPending() {
        configure(0)
        val manager = manager()
        val hostActivity = activity(taskId = 0)
        manager.onActivityStarted(hostActivity)
        assertNotNull(shadowOf(hostActivity).nextStartedActivity)

        val uri = Uri.parse("mixin://users/same-task")
        val urlActivity = Robolectric.buildActivity(
            UrlInterpreterActivity::class.java,
            Intent(Intent.ACTION_VIEW, uri),
        ).get()
        assertTrue(manager.checkAndShow(urlActivity))
        assertNull(shadowOf(urlActivity).nextStartedActivity)

        assertEquals(uri, manager.onAuthenticated())
    }

    @Test
    fun unlockedUrlInterpreterDoesNotPolluteSubsequentLockDeepLink() {
        configure(1, wallTime - 1_000)
        val manager = manager()
        val urlActivity = Robolectric.buildActivity(
            UrlInterpreterActivity::class.java,
            Intent(Intent.ACTION_VIEW, Uri.parse("mixin://users/unlocked")),
        ).get()
        assertFalse(manager.checkAndShow(urlActivity))

        wallTime += 60_001
        val mainActivity = activity(taskId = 1)
        manager.onActivityStarted(mainActivity)
        assertTrue(manager.isLocked)
        assertNull(manager.onAuthenticated())
    }

    @Test
    fun olderAuthActivityStartDoesNotOverwriteNewerPendingDeepLink() {
        configure(0)
        val manager = manager()
        val first = Uri.parse("mixin://users/first")
        val second = Uri.parse("mixin://users/second")
        val lock1 = Robolectric.buildActivity(AppAuthActivity::class.java, Intent().setData(first)).get()
        manager.onActivityStarted(lock1)

        val urlActivity = Robolectric.buildActivity(
            UrlInterpreterActivity::class.java,
            Intent(Intent.ACTION_VIEW, second),
        ).get()
        assertTrue(manager.checkAndShow(urlActivity))

        manager.onActivityStarted(lock1)
        assertEquals(second, manager.onAuthenticated())
    }

    @Test
    @Config(sdk = [33])
    fun resetClearsLockStatePendingDeepLinkTasksAndRestoresRecentsScreenshot() {
        configure(1, wallTime - 60_001)
        val manager = manager()
        val activity = activity(taskId = 1)
        manager.onActivityCreated(activity)
        assertEquals(false, activity.recentsScreenshotEnabled)
        manager.onActivityStarted(activity)
        assertTrue(manager.isLocked)

        val lockActivity = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        manager.onActivityStarted(lockActivity)
        val urlActivity = Robolectric.buildActivity(
            UrlInterpreterActivity::class.java,
            Intent(Intent.ACTION_VIEW, Uri.parse("mixin://pay/logout-clear")),
        ).get()
        assertTrue(manager.checkAndShow(urlActivity))
        manager.onActivityStopped(activity)
        manager.onActivityStopped(lockActivity)

        // Simulate logout removing PREF_APP_AUTH and resetting in-memory auth state.
        preferences.edit().remove(PREF_APP_AUTH).commit()
        manager.reset()
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(manager.isLocked)
        assertTrue(lockActivity.isFinishing)
        assertEquals(true, activity.recentsScreenshotEnabled)
        assertEquals(0L, preferences.getLong(PREF_APP_ENTER_BACKGROUND, -1))

        // Re-enabling lock after re-login should start from a clean slate without stale pendingTasks or deep links.
        configure(0)
        val nextActivity = activity(taskId = 1)
        manager.onActivityStarted(nextActivity)
        assertTrue(manager.isLocked)
        assertEquals(AppAuthActivity::class.java.name, shadowOf(nextActivity).nextStartedActivity.component?.className)
        assertNull(manager.onAuthenticated())
    }

    @Test
    fun pendingDeepLinkIsClearedWhenLockedAppGoesToBackground() {
        configure(0)
        val manager = manager()
        val urlActivity = Robolectric.buildActivity(
            UrlInterpreterActivity::class.java,
            Intent(Intent.ACTION_VIEW, Uri.parse("mixin://pay/stale-transfer")),
        ).get()
        assertTrue(manager.checkAndShow(urlActivity))

        val lockActivity = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        manager.onActivityStarted(lockActivity)
        manager.onActivityResumed(lockActivity)

        // Leaving the lock screen without unlocking clears the stale pending deep link.
        manager.onActivityPaused(lockActivity)
        manager.onActivityStopped(lockActivity)
        assertTrue(manager.isLocked)

        manager.onActivityStarted(lockActivity)
        manager.onActivityResumed(lockActivity)
        assertNull(manager.onAuthenticated())
    }

    @Test
    fun singleInstanceUrlInterpreterDoesNotBlockSubsequentAuthLaunches() {
        configure(0)
        val manager = AppAuthManager(
            preferences,
            { elapsed },
            { wallTime },
            { if (it is UrlInterpreterActivity) 20 else 1 },
        )
        val firstUrl = Robolectric.buildActivity(
            UrlInterpreterActivity::class.java,
            Intent(Intent.ACTION_VIEW, Uri.parse("mixin://users/first")),
        ).get()
        assertTrue(manager.checkAndShow(firstUrl))
        assertNotNull(shadowOf(firstUrl).nextStartedActivity)

        // Simulate AppAuthActivity launching in the main task while UrlInterpreterActivity stays in its own task.
        val lockInMainTask = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        manager.onActivityStarted(lockInMainTask)
        manager.onActivityStopped(lockInMainTask)

        val secondUrl = Robolectric.buildActivity(
            UrlInterpreterActivity::class.java,
            Intent(Intent.ACTION_VIEW, Uri.parse("mixin://users/second")),
        ).get()
        assertTrue(manager.checkAndShow(secondUrl))
        assertEquals(AppAuthActivity::class.java.name, shadowOf(secondUrl).nextStartedActivity.component?.className)
        assertEquals(Uri.parse("mixin://users/second"), manager.onAuthenticated())
    }

    @Test
    fun clearTaskNewActivityCanLaunchAuthBeforePreviousPendingLaunchCompletes() {
        configure(0)
        val manager = manager()
        val oldActivity = activity(taskId = 1)
        manager.onActivityCreated(oldActivity)
        manager.onActivityStarted(oldActivity)
        assertNotNull(shadowOf(oldActivity).nextStartedActivity)

        val clearTaskNewActivity = activity(taskId = 1)
        manager.onActivityCreated(clearTaskNewActivity)
        manager.onActivityStarted(clearTaskNewActivity)
        assertEquals(
            AppAuthActivity::class.java.name,
            shadowOf(clearTaskNewActivity).nextStartedActivity.component?.className,
        )
    }

    @Test
    fun startingAuthActivityWhenLockDisabledFinishesWithoutLocking() {
        val manager = manager()
        val restoredLock = Robolectric.buildActivity(
            AppAuthActivity::class.java,
            Intent().setData(Uri.parse("mixin://pay/stale")),
        ).get()
        manager.onActivityStarted(restoredLock)

        assertFalse(manager.isLocked)
        assertTrue(restoredLock.isFinishing)
        val nextHost = activity(taskId = 1)
        manager.onActivityStarted(nextHost)
        assertNull(shadowOf(nextHost).nextStartedActivity)
        assertNull(manager.onAuthenticated())
    }

    @Test
    @Config(sdk = [33])
    fun resetUpdatesAllStateAtomicallyOnMainThread() {
        configure(1, wallTime - 60_001)
        val manager = manager()
        val activity = activity(taskId = 1)
        manager.onActivityCreated(activity)
        manager.onActivityStarted(activity)
        val lockActivity = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        manager.onActivityStarted(lockActivity)
        assertTrue(manager.isLocked)

        preferences.edit().remove(PREF_APP_AUTH).commit()
        val worker = Thread { manager.reset() }
        worker.start()
        worker.join()

        // Before the main looper runs, state has not been partially mutated on the background thread.
        assertTrue(manager.isLocked)
        assertEquals(0L, preferences.getLong(PREF_APP_ENTER_BACKGROUND, -1))
        assertFalse(lockActivity.isFinishing)

        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(manager.isLocked)
        assertTrue(lockActivity.isFinishing)
        assertEquals(0L, preferences.getLong(PREF_APP_ENTER_BACKGROUND, -1))
        assertEquals(true, activity.recentsScreenshotEnabled)
    }

    @Test
    fun cancellingAuthSuppressesResumeRelockAndFlagsMultiTaskEscape() {
        configure(0)
        val mainActivity = activity(taskId = 1)
        val callActivity = activity(taskId = 2)

        val lock1 = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        val lock2 = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        val multiTaskManager = AppAuthManager(
            preferences,
            { elapsed },
            { wallTime },
            { activity ->
                when (activity) {
                    mainActivity, lock1 -> 1
                    else -> 2
                }
            },
        )
        multiTaskManager.onActivityStarted(mainActivity)
        assertNotNull(shadowOf(mainActivity).nextStartedActivity)
        multiTaskManager.onActivityStarted(lock1)
        multiTaskManager.onActivityResumed(lock1)

        // First cancellation moves task 1 away without triggering a second prompt on resume.
        assertFalse(multiTaskManager.onAuthCancelled(lock1))
        multiTaskManager.onActivityPaused(lock1)
        multiTaskManager.onActivityStopped(lock1)
        multiTaskManager.onActivityResumed(mainActivity)
        assertNull(shadowOf(mainActivity).nextStartedActivity)

        // Background task 2 resuming passively does not re-launch a lock screen via onActivityResumed.
        multiTaskManager.onActivityResumed(callActivity)
        assertNull(shadowOf(callActivity).nextStartedActivity)

        // If task 2 starts via onActivityStarted while locked and user cancels again, signal home escape.
        multiTaskManager.onActivityStarted(callActivity)
        assertNotNull(shadowOf(callActivity).nextStartedActivity)
        multiTaskManager.onActivityStarted(lock2)
        assertTrue(multiTaskManager.onAuthCancelled(lock2))
        multiTaskManager.onActivityStopped(mainActivity)
        multiTaskManager.onActivityStopped(callActivity)
        multiTaskManager.onActivityStopped(lock2)

        // Returning from background resets cancelled state and locks normally again.
        multiTaskManager.onActivityStarted(mainActivity)
        assertTrue(multiTaskManager.isLocked)
        assertNotNull(shadowOf(mainActivity).nextStartedActivity)
    }

    @Test
    fun lateAuthActivityStartAfterForegroundUnlockFinishesWithoutRelocking() {
        configure(0)
        val mainActivity = activity(taskId = 1)
        val otherActivity = activity(taskId = 2)
        val lock1 = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        val lock2 = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        val manager = AppAuthManager(
            preferences,
            { elapsed },
            { wallTime },
            { activity ->
                when (activity) {
                    mainActivity, lock1 -> 1
                    else -> 2
                }
            },
        )

        manager.onActivityStarted(mainActivity)
        manager.onActivityStarted(otherActivity)
        manager.onActivityStarted(lock1)
        assertNull(manager.onAuthenticated())
        assertFalse(manager.isLocked)

        // A second AppAuthActivity that finishes launching after unlock must close instead of locking again.
        manager.onActivityStarted(lock2)
        assertTrue(lock2.isFinishing)
        assertFalse(manager.isLocked)

        // Once the app goes to background and returns, locking resumes as expected.
        manager.onActivityStopped(mainActivity)
        manager.onActivityStopped(otherActivity)
        manager.onActivityStarted(mainActivity)
        assertTrue(manager.isLocked)
        assertNotNull(shadowOf(mainActivity).nextStartedActivity)
    }

    @Test
    fun pendingLaunchDeduplicationDoesNotExpireByElapsedTimeAlone() {
        configure(0)
        val manager = manager()
        val mainActivity = activity(taskId = 0)
        manager.onActivityCreated(mainActivity)
        manager.onActivityStarted(mainActivity)
        assertNotNull(shadowOf(mainActivity).nextStartedActivity)

        // Simulate slow cold-start taking longer than 500ms before AppAuthActivity starts.
        elapsed += 5_000L
        manager.onActivityResumed(mainActivity)
        assertNull(shadowOf(mainActivity).nextStartedActivity)

        val lockActivity = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        manager.onActivityCreated(lockActivity)
        manager.onActivityStarted(lockActivity)
        // Once AppAuthActivity has started without resuming, content resuming can bring it back.
        manager.onActivityResumed(mainActivity)
        assertNotNull(shadowOf(mainActivity).nextStartedActivity)
    }

    @Test
    fun finishingActivityStillEvaluatesLockStateAndBlocksWithoutLaunchingUi() {
        configure(0)
        val immediateManager = manager()
        val finishingActivity = activity(taskId = 1).apply { finish() }
        assertTrue(finishingActivity.isFinishing)
        assertTrue(immediateManager.checkAndShow(finishingActivity))
        assertTrue(immediateManager.isLocked)
        assertNull(shadowOf(finishingActivity).nextStartedActivity)

        configure(1, wallTime - 1_000)
        val graceManager = manager()
        val graceFinishing = activity(taskId = 2).apply { finish() }
        assertFalse(graceManager.checkAndShow(graceFinishing))
        assertFalse(graceManager.isLocked)

        wallTime += 60_001
        assertTrue(graceManager.checkAndShow(graceFinishing))
        assertTrue(graceManager.isLocked)
        assertNull(shadowOf(graceFinishing).nextStartedActivity)
    }

    private fun assertWindowSecure(window: Window, expected: Boolean) {
        assertEquals(expected, (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0)
        assertEquals(expected, FlagSecureReason.isSecured(window))
    }

    private fun configure(mode: Int, backgroundTime: Long = 0) {
        preferences.edit().putInt(PREF_APP_AUTH, mode)
            .putLong(PREF_APP_ENTER_BACKGROUND, backgroundTime).commit()
    }

    private fun manager() = AppAuthManager(preferences, { elapsed }, { wallTime })

    private fun activity(taskId: Int = 0) =
        Robolectric.buildActivity(TestActivity::class.java).create().get().apply {
            customTaskId = taskId
        }

    class TestActivity : Activity() {
        var changingConfigurations = false
        var customTaskId = 0
        var recentsScreenshotEnabled: Boolean? = null
        val recentsScreenshotHistory = mutableListOf<Boolean>()

        override fun isChangingConfigurations() = changingConfigurations

        override fun getTaskId() = customTaskId

        override fun setRecentsScreenshotEnabled(enabled: Boolean) {
            recentsScreenshotEnabled = enabled
            recentsScreenshotHistory.add(enabled)
        }
    }
}
