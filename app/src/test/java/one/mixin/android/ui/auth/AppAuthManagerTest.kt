package one.mixin.android.ui.auth

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.Constants.Account.PREF_APP_AUTH
import one.mixin.android.Constants.Account.PREF_APP_ENTER_BACKGROUND
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
    fun flagSecureAppliedWhenAppAuthEnabledAndClearedWhenDisabled() {
        configure(0)
        val manager = manager()
        val activity = activity()
        manager.onActivityCreated(activity)
        assertWindowSecure(activity, true)

        preferences.edit().putInt(PREF_APP_AUTH, -1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(activity, false)

        preferences.edit().putInt(PREF_APP_AUTH, 1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(activity, true)

        preferences.edit().remove(PREF_APP_AUTH).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(activity, false)
    }

    @Test
    fun sharedWindowKeepsFlagSecureUntilAllReasonsDetach() {
        configure(0)
        val manager = manager()
        val activity = activity()
        manager.onActivityCreated(activity)
        assertWindowSecure(activity, true)
        val secondReason = FlagSecureReason(activity.window) { true }.apply { attach() }

        preferences.edit().putInt(PREF_APP_AUTH, -1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(activity, true)

        preferences.edit().putInt(PREF_APP_AUTH, 0).commit()
        shadowOf(Looper.getMainLooper()).idle()
        secondReason.detach()
        assertWindowSecure(activity, true)

        preferences.edit().putInt(PREF_APP_AUTH, -1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(activity, false)
    }

    @Test
    fun destroyedActivityDetachesFlagSecureReasonAndIgnoresLaterChanges() {
        configure(0)
        val manager = manager()
        val activity = activity()
        manager.onActivityCreated(activity)
        assertWindowSecure(activity, true)

        manager.onActivityDestroyed(activity)
        assertWindowSecure(activity, false)

        preferences.edit().putInt(PREF_APP_AUTH, -1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        preferences.edit().putInt(PREF_APP_AUTH, 1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(activity, false)
    }

    @Test
    fun dialogFragmentWindowTracksAppAuthFlagSecureAndLifecycle() {
        configure(0)
        val manager = manager()
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java).create()
        val activity = activityController.get()
        manager.onActivityCreated(activity)
        activityController.start().resume()

        val dialogFragment = TestDialogFragment()
        dialogFragment.showNow(activity.supportFragmentManager, "test-dialog")
        shadowOf(Looper.getMainLooper()).idle()

        val dialogWindow = requireNotNull(dialogFragment.dialog?.window)
        assertWindowSecure(activity.window, true)
        assertWindowSecure(dialogWindow, true)

        // Backgrounding into Recents stops activity and dialog without clearing FLAG_SECURE.
        activityController.pause().stop()
        assertWindowSecure(activity.window, true)
        assertWindowSecure(dialogWindow, true)
        activityController.start().resume()

        preferences.edit().putInt(PREF_APP_AUTH, -1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(activity.window, false)
        assertWindowSecure(dialogWindow, false)

        preferences.edit().putInt(PREF_APP_AUTH, 1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(activity.window, true)
        assertWindowSecure(dialogWindow, true)

        dialogFragment.dismissNow()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(dialogWindow, false)

        preferences.edit().putInt(PREF_APP_AUTH, -1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        preferences.edit().putInt(PREF_APP_AUTH, 0).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(dialogWindow, false)
        assertWindowSecure(activity.window, true)
    }

    @Test
    fun sharedDialogWindowPreservesFlagSecureAcrossAppAuthAndSecondReason() {
        configure(0)
        val manager = manager()
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java).create()
        val activity = activityController.get()
        manager.onActivityCreated(activity)
        activityController.start().resume()

        val dialogFragment = TestDialogFragment()
        dialogFragment.showNow(activity.supportFragmentManager, "shared-dialog")
        shadowOf(Looper.getMainLooper()).idle()

        val dialogWindow = requireNotNull(dialogFragment.dialog?.window)
        var pinSecure = true
        val pinReason = FlagSecureReason(dialogWindow) { pinSecure }.apply { attach() }
        assertWindowSecure(dialogWindow, true)

        // Clearing PIN's FlagSecureReason while App Lock is enabled must keep FLAG_SECURE.
        pinSecure = false
        pinReason.invalidate()
        assertWindowSecure(dialogWindow, true)

        // Re-enabling PIN's FlagSecureReason while disabling App Lock must keep FLAG_SECURE.
        pinSecure = true
        pinReason.invalidate()
        preferences.edit().putInt(PREF_APP_AUTH, -1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(dialogWindow, true)

        pinReason.detach()
        assertWindowSecure(dialogWindow, false)
    }

    @Test
    fun attachAndDetachSecureWindowTracksAppAuthChanges() {
        configure(0)
        val manager = manager()
        val activity = activity()
        val dialog = Dialog(activity)
        val dialogWindow = requireNotNull(dialog.window)

        val reason = manager.attachSecureWindow(dialogWindow)
        assertNotNull(reason)
        assertWindowSecure(dialogWindow, true)

        preferences.edit().putInt(PREF_APP_AUTH, -1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(dialogWindow, false)

        preferences.edit().putInt(PREF_APP_AUTH, 2).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(dialogWindow, true)

        manager.detachSecureWindow(reason)
        assertWindowSecure(dialogWindow, false)

        preferences.edit().putInt(PREF_APP_AUTH, -1).commit()
        shadowOf(Looper.getMainLooper()).idle()
        preferences.edit().putInt(PREF_APP_AUTH, 0).commit()
        shadowOf(Looper.getMainLooper()).idle()
        assertWindowSecure(dialogWindow, false)
    }

    private fun assertWindowSecure(activity: Activity, expected: Boolean) {
        assertWindowSecure(activity.window, expected)
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

    private fun activity() = Robolectric.buildActivity(TestActivity::class.java).create().get()

    class TestActivity : Activity() {
        var changingConfigurations = false

        override fun isChangingConfigurations() = changingConfigurations
    }

    class TestDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = Dialog(requireContext())

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View = View(requireContext())
    }
}
