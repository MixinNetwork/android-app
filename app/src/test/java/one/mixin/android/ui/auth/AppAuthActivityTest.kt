package one.mixin.android.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.CancellationSignal
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories
import dagger.hilt.android.internal.managers.ActivityComponentManager
import one.mixin.android.MixinApplication
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    shadows = [AppAuthActivityTest.ShadowActivityComponentManager::class, AppAuthActivityTest.ShadowDefaultViewModelFactories::class],
)
class AppAuthActivityTest {
    private var previousContext: Context? = null
    private lateinit var controller: ActivityController<AppAuthActivity>

    @Before
    fun setUp() {
        ShadowBiometricPrompt.reset()
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
        val fakeApp = object : MixinApplication() {
            init {
                attachBaseContext(ApplicationProvider.getApplicationContext())
            }
        }
        MixinApplication.appContext = fakeApp
        controller.create().start().resume().visible()
        val activity = controller.get()
        val dialog = VerifyBottomSheetDialogFragment.newInstance(disableBiometric = true)
        ReflectionHelpers.callInstanceMethod<Unit>(
            activity,
            "bindPinVerification",
            ClassParameter.from(VerifyBottomSheetDialogFragment::class.java, dialog),
        )
        controller.pause().stop()

        val onPinSuccess = ReflectionHelpers.getField<((String) -> Unit)?>(dialog, "onPinSuccess")
        onPinSuccess?.invoke("123456")
        assertFalse(activity.isFinishing)

        controller.start()
        assertTrue(activity.isFinishing)
        controller.resume()
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
    fun systemCanceledBiometricWhileResumedDoesNotMoveTaskToBack() {
        controller.create().start().resume().visible()
        val activity = controller.get()
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idleFor(100, TimeUnit.MILLISECONDS)
        val session = currentAuthenticateSession()
        authenticateWithError(session, BiometricPrompt.ERROR_CANCELED, "Canceled")
        mainLooper.idle()
        assertFalse(shadowOf(activity).isTaskMovedToBack)
        assertFalse(activity.isFinishing)
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

    @Implements(ActivityComponentManager::class, isInAndroidSdk = false)
    class ShadowActivityComponentManager {
        @Implementation
        fun initSavedStateHandleHolders() = Unit

        @Implementation
        fun clearSavedStateHandleHolders() = Unit
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
