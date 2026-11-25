package one.mixin.android.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories
import dagger.hilt.android.internal.managers.ActivityComponentManager
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        controller.destroy()
        ReflectionHelpers.setStaticField(MixinApplication::class.java, "appContext", previousContext)
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
    fun pinSuccessWhileStoppedUnlocksWhenActivityStarts() {
        val fakeApp = FakeMixinApplication()
        MixinApplication.appContext = fakeApp
        controller.create().start().resume().visible()
        val activity = controller.get()
        val dialog = VerifyBottomSheetDialogFragment.newInstance(disableBiometric = true)
        ReflectionHelpers.callInstanceMethod<Unit>(
            activity,
            "bindPinVerification",
            ClassParameter.from(VerifyBottomSheetDialogFragment::class.java, dialog),
        )
        assertTrue(dialog.disableToast)
        controller.pause().stop()

        val onPinSuccess = ReflectionHelpers.getField<((String) -> Unit)?>(dialog, "onPinSuccess")
        onPinSuccess?.invoke("123456")
        assertFalse(activity.isFinishing)
        assertNull(ShadowToast.getLatestToast())

        controller.start()
        assertTrue(activity.isFinishing)
        assertEquals(activity.getString(R.string.Successful), ShadowToast.getTextOfLatestToast())
        controller.resume()
    }

    @Test
    fun pinSuccessAfterSaveInstanceStateSurvivesRecreation() {
        val fakeApp = FakeMixinApplication()
        MixinApplication.appContext = fakeApp
        controller.create().start().resume().visible()
        val oldActivity = controller.get()
        val dialog = VerifyBottomSheetDialogFragment.newInstance(disableBiometric = true)
        ReflectionHelpers.callInstanceMethod<Unit>(
            oldActivity,
            "bindPinVerification",
            ClassParameter.from(VerifyBottomSheetDialogFragment::class.java, dialog),
        )
        val savedState = Bundle()
        controller.pause().stop().saveInstanceState(savedState)

        val onPinSuccess = ReflectionHelpers.getField<((String) -> Unit)?>(dialog, "onPinSuccess")
        onPinSuccess?.invoke("123456")
        assertFalse(oldActivity.isFinishing)
        assertNull(ShadowToast.getLatestToast())

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
