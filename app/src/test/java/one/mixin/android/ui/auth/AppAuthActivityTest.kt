package one.mixin.android.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories
import dagger.hilt.android.internal.managers.ActivityComponentManager
import one.mixin.android.Constants.Account.PREF_APP_AUTH
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.url.UrlInterpreterActivity
import org.junit.After
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
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowBiometricPrompt
import org.robolectric.shadows.ShadowToast
import org.robolectric.util.ReflectionHelpers
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBiometricManager
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    shadows = [
        AppAuthActivityTest.ShadowActivityComponentManager::class,
        AppAuthActivityTest.ShadowDefaultViewModelFactories::class,
    ],
)
class AppAuthActivityTest {
    private var previousContext: Context? = null
    private lateinit var controller: ActivityController<AppAuthActivity>

    @Before
    fun setUp() {
        ShadowBiometricPrompt.reset()
        ShadowToast.reset()
        previousContext = ReflectionHelpers.getStaticField(MixinApplication::class.java, "appContext")
        MixinApplication.appContext = ApplicationProvider.getApplicationContext()
        controller = Robolectric.buildActivity(AppAuthActivity::class.java)
        ReflectionHelpers.setField(controller.get(), "injected", true)
    }

    @After
    fun tearDown() {
        val lifecycle = controller.get().lifecycle
        if (lifecycle.currentState == Lifecycle.State.RESUMED) controller.pause()
        if (lifecycle.currentState == Lifecycle.State.STARTED) controller.stop()
        if (lifecycle.currentState != Lifecycle.State.INITIALIZED) controller.destroy()
        ReflectionHelpers.setStaticField(MixinApplication::class.java, "appContext", previousContext)
        ShadowBiometricPrompt.reset()
        ShadowToast.reset()
    }

    @Test
    fun backMovesTaskAwayWithoutRemovingTheLockScreen() {
        controller.create().start().resume()
        val activity = controller.get()
        activity.onBackPressedDispatcher.onBackPressed()
        assertTrue(shadowOf(activity).isTaskMovedToBack)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun reusedLockScreenKeepsTheLatestDeepLink() {
        controller.create().start().resume()
        val activity = controller.get()
        val first = Uri.parse("mixin://users/first")
        val latest = Uri.parse("mixin://users/second")
        controller.newIntent(Intent(activity, AppAuthActivity::class.java).setData(first))
        controller.newIntent(Intent(activity, AppAuthActivity::class.java).setData(latest))
        controller.newIntent(Intent(activity, AppAuthActivity::class.java))
        assertEquals(latest, activity.intent.data)
    }

    @Test
    fun cancelledBiometricCallbackCannotDismissOrUnlockStoppedActivity() {
        controller.create().start().resume().visible()
        shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS)
        val session = ReflectionHelpers.getStaticField<Any>(ShadowBiometricPrompt::class.java, "currentAuthenticateSession")
        val cancellation = ReflectionHelpers.getField<CancellationSignal>(session, "cancel")
        controller.pause().stop()
        assertTrue(cancellation.isCanceled)
        ReflectionHelpers.callInstanceMethod<Unit>(
            session,
            "authenticateWithError",
            ClassParameter.from(Int::class.javaPrimitiveType, BiometricPrompt.ERROR_CANCELED),
            ClassParameter.from(String::class.java, "Cancelled"),
        )
        ReflectionHelpers.callInstanceMethod<Unit>(session, "authenticateSuccessfully")
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(controller.get().isFinishing)
        assertFalse(shadowOf(controller.get()).isTaskMovedToBack)
    }

    @Test
    fun backgroundedPinSuccessDoesNotUnlockWhenActivityRestarts() {
        val fakeApp = FakeMixinApplication()
        MixinApplication.appContext = fakeApp
        fakeApp.defaultSharedPreferences.edit().putInt(PREF_APP_AUTH, 0).commit()
        controller.create().start().resume().visible()
        val activity = controller.get()
        fakeApp.appAuth.onActivityStarted(activity)
        assertTrue(fakeApp.appAuth.isLocked)

        val dialog = VerifyBottomSheetDialogFragment.newInstance(disableBiometric = true)
        ReflectionHelpers.callInstanceMethod<Unit>(
            activity,
            "bindPinVerification",
            ClassParameter.from(VerifyBottomSheetDialogFragment::class.java, dialog),
        )
        assertTrue(dialog.disableToast)
        controller.pause().stop()
        fakeApp.appAuth.onActivityStopped(activity)

        val onPinSuccess = ReflectionHelpers.getField<((String) -> Unit)?>(dialog, "onPinSuccess")
        onPinSuccess?.invoke("123456")
        assertFalse(activity.isFinishing)
        assertNull(ShadowToast.getLatestToast())

        controller.start()
        assertFalse(activity.isFinishing)
        assertTrue(fakeApp.appAuth.isLocked)
        assertNull(ShadowToast.getLatestToast())
        controller.resume()
    }

    @Test
    fun pendingPinUnlockIsInvalidatedWhenAppGoesToBackground() {
        val fakeApp = FakeMixinApplication()
        MixinApplication.appContext = fakeApp
        fakeApp.defaultSharedPreferences.edit().putInt(PREF_APP_AUTH, 0).commit()
        controller.create().start().resume().visible()
        val activity = controller.get()
        fakeApp.appAuth.onActivityStarted(activity)

        val dialog = VerifyBottomSheetDialogFragment.newInstance(disableBiometric = true)
        ReflectionHelpers.callInstanceMethod<Unit>(
            activity,
            "bindPinVerification",
            ClassParameter.from(VerifyBottomSheetDialogFragment::class.java, dialog),
        )
        // Simulate stopping during a config-change window so pendingPinUnlock becomes populated.
        ReflectionHelpers.setField(activity, "mChangingConfigurations", true)
        controller.pause().stop()
        ReflectionHelpers.getField<((String) -> Unit)?>(dialog, "onPinSuccess")?.invoke("123456")
        assertTrue(ViewModelProvider(activity)[AppAuthViewModel::class.java].pendingPinUnlock)

        // Then app enters background (e.g. non-config stop / process backgrounded), advancing lockEpoch.
        ReflectionHelpers.setField(activity, "mChangingConfigurations", false)
        fakeApp.appAuth.onActivityStopped(activity)
        controller.start()
        assertFalse(activity.isFinishing)
        assertTrue(fakeApp.appAuth.isLocked)
        assertFalse(ViewModelProvider(activity)[AppAuthViewModel::class.java].pendingPinUnlock)
        controller.resume()
    }

    @Test
    fun pinSuccessIsNotPersistedAcrossProcessRecreation() {
        val fakeApp = FakeMixinApplication()
        MixinApplication.appContext = fakeApp
        fakeApp.defaultSharedPreferences.edit().putInt(PREF_APP_AUTH, 0).commit()
        controller.create().start().resume().visible()
        val oldActivity = controller.get()
        fakeApp.appAuth.onActivityStarted(oldActivity)

        val dialog = VerifyBottomSheetDialogFragment.newInstance(disableBiometric = true)
        ReflectionHelpers.callInstanceMethod<Unit>(
            oldActivity,
            "bindPinVerification",
            ClassParameter.from(VerifyBottomSheetDialogFragment::class.java, dialog),
        )
        ReflectionHelpers.setField(oldActivity, "mChangingConfigurations", true)
        controller.pause().stop()
        ReflectionHelpers.getField<((String) -> Unit)?>(dialog, "onPinSuccess")?.invoke("123456")

        val savedState = Bundle()
        controller.saveInstanceState(savedState)
        assertFalse(savedState.containsKey("pending_pin_unlock"))

        // Even if a restored bundle contained a legacy pending_pin_unlock key, a fresh process must not auto-unlock.
        savedState.putBoolean("pending_pin_unlock", true)
        val restoredController = Robolectric.buildActivity(AppAuthActivity::class.java)
        ReflectionHelpers.setField(restoredController.get(), "injected", true)
        restoredController.create(savedState).start()
        val restoredActivity = restoredController.get()
        assertFalse(restoredActivity.isFinishing)
        assertTrue(fakeApp.appAuth.isLocked)
        assertNull(ShadowToast.getLatestToast())
        restoredController.resume().pause().stop().destroy()
    }

    @Test
    fun pinSuccessDuringConfigurationChangeSurvivesRecreation() {
        val fakeApp = FakeMixinApplication()
        MixinApplication.appContext = fakeApp
        fakeApp.defaultSharedPreferences.edit().putInt(PREF_APP_AUTH, 0).commit()
        controller.create().start().resume().visible()
        val oldActivity = controller.get()
        fakeApp.appAuth.onActivityStarted(oldActivity)

        val dialog = VerifyBottomSheetDialogFragment.newInstance(disableBiometric = true)
        ReflectionHelpers.callInstanceMethod<Unit>(
            oldActivity,
            "bindPinVerification",
            ClassParameter.from(VerifyBottomSheetDialogFragment::class.java, dialog),
        )
        ReflectionHelpers.setField(oldActivity, "mChangingConfigurations", true)
        controller.pause().stop()
        ReflectionHelpers.getField<((String) -> Unit)?>(dialog, "onPinSuccess")?.invoke("123456")

        controller.recreate()
        val recreatedActivity = controller.get()
        assertTrue(recreatedActivity !== oldActivity)
        assertTrue(recreatedActivity.isFinishing)
        assertFalse(fakeApp.appAuth.isLocked)
        assertEquals(recreatedActivity.getString(R.string.Successful), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun dismissingPinSheetWhileFragmentStillPresentRetriesBiometricPrompt() {
        controller.create().start().resume().visible()
        val activity = controller.get()
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        val session1 = currentAuthenticateSession()
        ReflectionHelpers.callInstanceMethod<Unit>(activity, "cancelPrompt")

        val dialog = VerifyBottomSheetDialogFragment.newInstance(disableBiometric = true)
        ReflectionHelpers.callInstanceMethod<Unit>(
            activity,
            "bindPinVerification",
            ClassParameter.from(VerifyBottomSheetDialogFragment::class.java, dialog),
        )
        val stubSheet = Fragment()
        activity.supportFragmentManager.beginTransaction()
            .add(stubSheet, VerifyBottomSheetDialogFragment.TAG)
            .commitNow()

        // Invoke dismissal callback while fragment is still findable by tag.
        val onResult = ReflectionHelpers.getField<((Boolean) -> Unit)?>(dialog, "onResult")
        onResult?.invoke(false)
        assertTrue(ReflectionHelpers.getField<Boolean>(activity, "promptScheduled"))

        // If the sheet has not completed removal when the runnable fires, prompt stays suppressed.
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        assertFalse(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))

        // Once dismissal completes, a retry scheduled during onDismiss triggers the biometric prompt.
        onResult?.invoke(false)
        activity.supportFragmentManager.beginTransaction()
            .remove(stubSheet)
            .commitNow()
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        val session2 = currentAuthenticateSession()
        assertTrue(session2 !== session1)
        assertTrue(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))
    }

    @Test
    fun systemCanceledBiometricWhilePausedDoesNotMoveTaskToBackAndRetriesOnResume() {
        controller.create().start().resume().visible()
        val activity = controller.get()
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        val session1 = currentAuthenticateSession()
        assertTrue(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))

        controller.pause()
        authenticateWithError(session1, BiometricPrompt.ERROR_CANCELED, "Canceled")
        mainLooper.idle()
        assertFalse(shadowOf(activity).isTaskMovedToBack)
        assertFalse(activity.isFinishing)
        assertFalse(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))

        controller.resume()
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        val session2 = currentAuthenticateSession()
        assertTrue(session2 !== session1)
        assertTrue(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))
    }

    @Test
    fun systemCanceledBiometricWhileResumedRetriesAfterDelay() {
        controller.create().start().resume().visible()
        val activity = controller.get()
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        val session1 = currentAuthenticateSession()
        authenticateWithError(session1, BiometricPrompt.ERROR_CANCELED, "Canceled")
        mainLooper.idle()
        assertFalse(shadowOf(activity).isTaskMovedToBack)
        assertFalse(activity.isFinishing)
        assertFalse(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))

        mainLooper.idleFor(1000, TimeUnit.MILLISECONDS)
        val session2 = currentAuthenticateSession()
        assertTrue(session2 !== session1)
        assertTrue(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))
    }

    @Test
    fun userCanceledBiometricStillMovesTaskToBack() {
        controller.create().start().resume().visible()
        val activity = controller.get()
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        val session = currentAuthenticateSession()
        authenticateWithError(session, BiometricPrompt.ERROR_USER_CANCELED, "User canceled")
        mainLooper.idle()
        assertTrue(shadowOf(activity).isTaskMovedToBack)
        assertFalse(activity.isFinishing)
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    @Test
    fun cancellingSecondTaskLockScreenRoutesToHomeToAvoidLoop() {
        val fakeApp = FakeMixinApplication()
        MixinApplication.appContext = fakeApp
        fakeApp.defaultSharedPreferences.edit().putInt(PREF_APP_AUTH, 0).commit()

        val firstLock = Robolectric.buildActivity(AppAuthActivity::class.java).get()
        assertFalse(fakeApp.appAuth.onAuthCancelled(firstLock))

        // Second task lock screen (distinct taskId) triggers HOME intent when cancelled to prevent looping.
        val secondManager = AppAuthManager(
            fakeApp.defaultSharedPreferences,
            taskIdOf = { activity -> if (activity === firstLock) 1 else 2 },
        )
        assertFalse(secondManager.onAuthCancelled(firstLock))
        ReflectionHelpers.setField(fakeApp, "appAuth\$delegate", lazyOf(secondManager))

        controller.create().start().resume().visible()
        val activity = controller.get()
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        authenticateWithError(currentAuthenticateSession(), BiometricPrompt.ERROR_USER_CANCELED, "User canceled")
        mainLooper.idle()

        assertTrue(shadowOf(activity).isTaskMovedToBack)
        val homeIntent = shadowOf(activity).nextStartedActivity
        assertNotNull(homeIntent)
        assertEquals(Intent.ACTION_MAIN, homeIntent.action)
        assertTrue(homeIntent.hasCategory(Intent.CATEGORY_HOME))
    }

    @Test
    fun negativeButtonUsesVerifyPinAndOpensPinVerification() {
        val biometricManager = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(android.hardware.biometrics.BiometricManager::class.java)
        Shadow.extract<ShadowBiometricManager>(biometricManager).setCanAuthenticate(false)
        controller.create().start().resume().visible()
        val activity = controller.get()
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        val session = currentAuthenticateSession()

        authenticateWithError(session, BiometricPrompt.ERROR_NEGATIVE_BUTTON, activity.getString(R.string.Verify_PIN))
        mainLooper.idle()

        assertFalse(shadowOf(activity).isTaskMovedToBack)
        assertFalse(activity.isFinishing)
        assertFalse(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))
        assertFalse(ReflectionHelpers.getField<Boolean>(activity, "promptScheduled"))
        assertTrue(activity.findViewById<TextView>(R.id.pin_tv).isVisible)
        val fragment = activity.supportFragmentManager.findFragmentByTag(VerifyBottomSheetDialogFragment.TAG)
        assertNotNull(fragment)
    }

    @Test
    fun resolveAppAuthAuthenticatorsPrefersStrongAndFallsBackToWeak() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowBiometric = Shadow.extract<ShadowBiometricManager>(
            context.getSystemService(android.hardware.biometrics.BiometricManager::class.java),
        )
        shadowBiometric.setCanAuthenticate(true)
        shadowBiometric.setAuthenticatorType(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG)
        assertEquals(BiometricManager.Authenticators.BIOMETRIC_STRONG, resolveAppAuthAuthenticators(context))

        shadowBiometric.setCanAuthenticate(false)
        shadowBiometric.setAuthenticatorType(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK)
        assertEquals(BiometricManager.Authenticators.BIOMETRIC_WEAK, resolveAppAuthAuthenticators(context))
    }

    @Test
    fun promptScheduledBeforePauseDefersUntilResume() {
        controller.create().start().resume().visible()
        val activity = controller.get()
        val mainLooper = shadowOf(Looper.getMainLooper())
        controller.pause()
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        assertFalse(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))

        controller.resume()
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        assertTrue(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))
    }

    @Test
    fun systemCanceledBiometricStopsAutoRetryingAfterLimitAndRecoversOnManualClickOrRestart() {
        controller.create().start().resume().visible()
        val activity = controller.get()
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)

        var lastSession = currentAuthenticateSession()
        repeat(3) {
            authenticateWithError(lastSession, BiometricPrompt.ERROR_CANCELED, "Canceled")
            mainLooper.idleFor(1000, TimeUnit.MILLISECONDS)
            val nextSession = currentAuthenticateSession()
            assertTrue(nextSession !== lastSession)
            assertTrue(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))
            lastSession = nextSession
        }

        // Fourth consecutive ERROR_CANCELED reaches the retry cap and stops auto-retrying.
        authenticateWithError(lastSession, BiometricPrompt.ERROR_CANCELED, "Sensor canceled")
        mainLooper.idleFor(5000, TimeUnit.MILLISECONDS)
        assertTrue(currentAuthenticateSession() === lastSession)
        assertFalse(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))
        assertFalse(ReflectionHelpers.getField<Boolean>(activity, "promptScheduled"))
        assertTrue(activity.findViewById<TextView>(R.id.pin_tv).isVisible)
        assertEquals("Sensor canceled", activity.findViewById<TextView>(R.id.info).text.toString())

        // Tapping the fingerprint icon manually resets the counter and triggers a new prompt.
        activity.findViewById<View>(R.id.swirl).performClick()
        mainLooper.idle()
        val manualSession = currentAuthenticateSession()
        assertTrue(manualSession !== lastSession)
        assertTrue(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))

        // Returning from background (onStop -> onStart) also resets auto-retry allowance.
        controller.pause().stop()
        controller.start().resume()
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        val restartedSession = currentAuthenticateSession()
        assertTrue(restartedSession !== manualSession)
        authenticateWithError(restartedSession, BiometricPrompt.ERROR_CANCELED, "Canceled after restart")
        mainLooper.idleFor(1000, TimeUnit.MILLISECONDS)
        assertTrue(currentAuthenticateSession() !== restartedSession)
        assertTrue(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))
    }

    @Test
    fun otherBiometricErrorsStopAutoRetryingAfterLimit() {
        controller.create().start().resume().visible()
        val activity = controller.get()
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)

        var lastSession = currentAuthenticateSession()
        repeat(3) { attempt ->
            authenticateWithError(lastSession, BiometricPrompt.ERROR_UNABLE_TO_PROCESS, "Sensor busy")
            mainLooper.idle()
            assertTrue("expected retry scheduled on attempt $attempt", ReflectionHelpers.getField<Boolean>(activity, "promptScheduled"))
            mainLooper.idleFor(1000, TimeUnit.MILLISECONDS)
            val nextSession = currentAuthenticateSession()
            assertTrue("expected new session on attempt $attempt", nextSession !== lastSession)
            assertTrue(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))
            lastSession = nextSession
        }

        authenticateWithError(lastSession, BiometricPrompt.ERROR_UNABLE_TO_PROCESS, "Sensor failed")
        mainLooper.idleFor(5000, TimeUnit.MILLISECONDS)
        assertTrue(currentAuthenticateSession() === lastSession)
        assertFalse(ReflectionHelpers.getField<Boolean>(activity, "promptActive"))
        assertFalse(ReflectionHelpers.getField<Boolean>(activity, "promptScheduled"))
        assertTrue(activity.findViewById<TextView>(R.id.pin_tv).isVisible)
        assertEquals("Sensor failed", activity.findViewById<TextView>(R.id.info).text.toString())
    }

    @Test
    fun hardwareUnavailableDoesNotDisableAppAuthAfterPinUnlock() {
        val fakeApp = FakeMixinApplication()
        MixinApplication.appContext = fakeApp
        fakeApp.defaultSharedPreferences.edit().putInt(PREF_APP_AUTH, 0).commit()
        val biometricManager = fakeApp.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
        Shadow.extract<ShadowBiometricManager>(biometricManager).setCanAuthenticate(true)
        controller.create().start().resume().visible()
        val activity = controller.get()
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)

        val session = currentAuthenticateSession()
        authenticateWithError(session, BiometricPrompt.ERROR_HW_UNAVAILABLE, "Hardware unavailable")
        mainLooper.idle()

        val viewModel = ViewModelProvider(activity)[AppAuthViewModel::class.java]
        assertFalse(viewModel.clearWhenPinSuccess)
        assertTrue(activity.findViewById<TextView>(R.id.pin_tv).isVisible)

        val dialog = VerifyBottomSheetDialogFragment.newInstance(disableBiometric = true)
        ReflectionHelpers.callInstanceMethod<Unit>(
            activity,
            "bindPinVerification",
            ClassParameter.from(VerifyBottomSheetDialogFragment::class.java, dialog),
        )
        val onPinSuccess = ReflectionHelpers.getField<((String) -> Unit)?>(dialog, "onPinSuccess")
        onPinSuccess?.invoke("123456")

        assertTrue(activity.isFinishing)
        assertEquals(0, fakeApp.defaultSharedPreferences.getInt(PREF_APP_AUTH, -1))
    }

    @Test
    fun hardwareUnavailableWithMissingHardwareClearsAppAuthAfterPinUnlock() {
        val fakeApp = FakeMixinApplication()
        MixinApplication.appContext = fakeApp
        fakeApp.defaultSharedPreferences.edit().putInt(PREF_APP_AUTH, 0).commit()
        val biometricManager = fakeApp.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
        Shadow.extract<ShadowBiometricManager>(biometricManager).setCanAuthenticate(false)
        controller.create().start().resume().visible()
        val activity = controller.get()
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)

        authenticateWithError(currentAuthenticateSession(), BiometricPrompt.ERROR_HW_UNAVAILABLE, "Hardware unavailable")
        mainLooper.idle()

        val viewModel = ViewModelProvider(activity)[AppAuthViewModel::class.java]
        assertTrue(viewModel.clearWhenPinSuccess)

        val dialog = VerifyBottomSheetDialogFragment.newInstance(disableBiometric = true)
        ReflectionHelpers.callInstanceMethod<Unit>(
            activity,
            "bindPinVerification",
            ClassParameter.from(VerifyBottomSheetDialogFragment::class.java, dialog),
        )
        ReflectionHelpers.getField<((String) -> Unit)?>(dialog, "onPinSuccess")?.invoke("123456")

        assertTrue(activity.isFinishing)
        assertEquals(-1, fakeApp.defaultSharedPreferences.getInt(PREF_APP_AUTH, 0))
    }

    @Test
    fun newIntentClearsPreviousBiometricErrorState() {
        controller.create().start().resume().visible()
        val activity = controller.get()
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)

        authenticateWithError(currentAuthenticateSession(), BiometricPrompt.ERROR_LOCKOUT, "Too many attempts")
        mainLooper.idle()
        val infoView = activity.findViewById<TextView>(R.id.info)
        assertEquals("Too many attempts", infoView.text.toString())
        assertEquals(activity.getColor(R.color.colorRed), infoView.currentTextColor)

        controller.newIntent(Intent(activity, AppAuthActivity::class.java))
        assertEquals(activity.getString(R.string.Confirm_fingerprint), infoView.text.toString())
        assertFalse(infoView.currentTextColor == activity.getColor(R.color.colorRed))
    }

    @Test
    fun leavingLockScreenWithoutUnlockingClearsIntentDataAndPendingDeepLink() {
        val fakeApp = FakeMixinApplication()
        MixinApplication.appContext = fakeApp
        fakeApp.defaultSharedPreferences.edit().putInt(PREF_APP_AUTH, 0).commit()

        val staleUri = Uri.parse("mixin://pay/stale-link")
        val localController = Robolectric.buildActivity(
            AppAuthActivity::class.java,
            Intent(Intent.ACTION_VIEW, staleUri),
        )
        val lockActivity = localController.get()
        ReflectionHelpers.setField(lockActivity, "injected", true)
        localController.create().start().resume().visible()
        fakeApp.appAuth.onActivityStarted(lockActivity)
        fakeApp.appAuth.onActivityResumed(lockActivity)

        localController.pause().stop()
        fakeApp.appAuth.onActivityPaused(lockActivity)
        fakeApp.appAuth.onActivityStopped(lockActivity)
        assertNull(lockActivity.intent.data)

        localController.start().resume()
        fakeApp.appAuth.onActivityStarted(lockActivity)
        fakeApp.appAuth.onActivityResumed(lockActivity)
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        ReflectionHelpers.callInstanceMethod<Unit>(currentAuthenticateSession(), "authenticateSuccessfully")
        mainLooper.idle()

        assertTrue(lockActivity.isFinishing)
        val startedIntent = shadowOf(lockActivity).nextStartedActivity
        assertEquals(MainActivity::class.java.name, startedIntent?.component?.className)
        assertNull(startedIntent?.data)
        localController.pause().stop().destroy()
    }

    @Test
    fun noBiometricsAndHardwareNotPresentClearAppAuthAfterPinUnlock() {
        for (errorCode in listOf(BiometricPrompt.ERROR_NO_BIOMETRICS, BiometricPrompt.ERROR_HW_NOT_PRESENT)) {
            val fakeApp = FakeMixinApplication()
            MixinApplication.appContext = fakeApp
            fakeApp.defaultSharedPreferences.edit().putInt(PREF_APP_AUTH, 1).commit()
            val localController = Robolectric.buildActivity(AppAuthActivity::class.java)
            ReflectionHelpers.setField(localController.get(), "injected", true)
            localController.create().start().resume().visible()
            val activity = localController.get()
            val mainLooper = shadowOf(Looper.getMainLooper())
            mainLooper.idleFor(100, TimeUnit.MILLISECONDS)

            authenticateWithError(currentAuthenticateSession(), errorCode, "Unavailable")
            mainLooper.idle()

            val viewModel = ViewModelProvider(activity)[AppAuthViewModel::class.java]
            assertTrue(viewModel.clearWhenPinSuccess)

            val dialog = VerifyBottomSheetDialogFragment.newInstance(disableBiometric = true)
            ReflectionHelpers.callInstanceMethod<Unit>(
                activity,
                "bindPinVerification",
                ClassParameter.from(VerifyBottomSheetDialogFragment::class.java, dialog),
            )
            ReflectionHelpers.getField<((String) -> Unit)?>(dialog, "onPinSuccess")?.invoke("123456")

            assertTrue(activity.isFinishing)
            assertEquals(-1, fakeApp.defaultSharedPreferences.getInt(PREF_APP_AUTH, 0))
            localController.pause().stop().destroy()
        }
    }

    @Test
    fun unlockingInFirstTaskOpensPendingDeepLinkFromSecondTaskOnce() {
        val fakeApp = FakeMixinApplication()
        MixinApplication.appContext = fakeApp
        fakeApp.defaultSharedPreferences.edit().putInt(PREF_APP_AUTH, 0).commit()

        controller.create().start().resume().visible()
        val lock1 = controller.get()
        fakeApp.appAuth.onActivityStarted(lock1)

        val uri = Uri.parse("mixin://pay/from-second-task")
        val urlActivity = Robolectric.buildActivity(
            UrlInterpreterActivity::class.java,
            Intent(Intent.ACTION_VIEW, uri),
        ).get()
        assertTrue(fakeApp.appAuth.checkAndShow(urlActivity))
        // Consume the AppAuthActivity launch triggered by UrlInterpreterActivity before verifying unlock.
        assertEquals(AppAuthActivity::class.java.name, shadowOf(urlActivity).nextStartedActivity.component?.className)

        val lock2Controller = Robolectric.buildActivity(AppAuthActivity::class.java, Intent().setData(uri))
        ReflectionHelpers.setField(lock2Controller.get(), "injected", true)
        val lock2 = lock2Controller.create().start().get()
        fakeApp.appAuth.onActivityStarted(lock2)

        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        ReflectionHelpers.callInstanceMethod<Unit>(currentAuthenticateSession(), "authenticateSuccessfully")
        mainLooper.idle()

        val startedIntent = shadowOf(lock1).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(UrlInterpreterActivity::class.java.name, startedIntent.component?.className)
        assertEquals(uri, startedIntent.data)
        assertNull(shadowOf(lock1).nextStartedActivity)
        assertTrue(lock1.isFinishing)
        assertTrue(lock2.isFinishing)
        assertNull(shadowOf(lock2).nextStartedActivity)
        lock2Controller.stop().destroy()
    }

    private fun currentAuthenticateSession(): Any =
        ReflectionHelpers.getStaticField(ShadowBiometricPrompt::class.java, "currentAuthenticateSession")

    private fun authenticateWithError(session: Any, errorCode: Int, message: String) {
        ReflectionHelpers.callInstanceMethod<Unit>(
            session,
            "authenticateWithError",
            ClassParameter.from(Int::class.javaPrimitiveType, errorCode),
            ClassParameter.from(String::class.java, message),
        )
    }

    private class FakeMixinApplication : MixinApplication() {
        init {
            attachBaseContext(ApplicationProvider.getApplicationContext())
        }
    }

    @Implements(ActivityComponentManager::class, isInAndroidSdk = false)
    class ShadowActivityComponentManager {
        @Implementation
        fun initSavedStateHandleHolders() = Unit

        @Implementation
        fun clearSavedStateHandleHolders() = Unit

        @Implementation
        fun generatedComponent(): Any =
            Proxy.newProxyInstance(
                ActivityComponentManager::class.java.classLoader,
                arrayOf(Class.forName("one.mixin.android.ui.auth.AppAuthActivity_GeneratedInjector")),
            ) { _, _, _ -> null }
    }

    @Implements(DefaultViewModelFactories::class, isInAndroidSdk = false)
    class ShadowDefaultViewModelFactories {
        companion object {
            @JvmStatic
            @Implementation
            fun getActivityFactory(activity: ComponentActivity, delegateFactory: ViewModelProvider.Factory): ViewModelProvider.Factory = delegateFactory
        }
    }
}
