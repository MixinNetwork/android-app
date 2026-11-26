package one.mixin.android.compose

import android.app.Dialog
import android.os.Looper
import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.activity.ComponentActivity
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.Dispatchers
import one.mixin.android.Constants.Account.PREF_APP_AUTH
import one.mixin.android.MixinApplication
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.util.FlagSecureReason
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(application = MixinAlertDialogTest.TestApplication::class, manifest = Config.NONE, sdk = [29])
class MixinAlertDialogTest {
    private lateinit var controller: ActivityController<ComponentActivity>
    private val activity get() = controller.get()
    private val frameClock = BroadcastFrameClock()
    private val showDialog = mutableStateOf(true)
    private val confirmText = mutableStateOf("Confirm")
    private var renderedConfirmText: String? = null

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    }

    @After
    fun tearDown() {
        if (activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun openDialogTracksLockPreferenceUntilRemovedFromComposition() {
        setAppLockEnabled(false)
        val dialog = openDialog()
        val window = requireNotNull(dialog.window)
        assertNotSame(activity.window, window)
        assertSecure(window, false)

        setAppLockEnabled(true)
        assertSecure(activity.window, false)
        assertSecure(window, true)
        recomposeDialog()
        assertSecure(window, true)

        setAppLockEnabled(false)
        assertSecure(window, false)
        setAppLockEnabled(true)
        assertSecure(window, true)

        showDialog.value = false
        idle()
        assertFalse(dialog.isShowing)
        assertSecure(window, false)
        assertIgnoresLaterAppLockChanges(window)
    }

    @Test
    fun dialogOpenedWithLockKeepsIndependentProtectionAndDetachesOnDestroy() {
        setAppLockEnabled(true)
        val dialog = openDialog()
        val window = requireNotNull(dialog.window)
        assertSecure(window, true)

        val independentReason = FlagSecureReason(window) { true }.apply { attach() }
        setAppLockEnabled(false)
        assertSecure(window, true)
        recomposeDialog()
        assertSecure(window, true)
        independentReason.detach()
        assertSecure(window, false)

        setAppLockEnabled(true)
        controller.pause().stop().destroy()
        idle()
        assertFalse(dialog.isShowing)
        assertSecure(window, false)
        assertIgnoresLaterAppLockChanges(window)
    }

    @Test
    fun disablingAppLockPreservesInheritedHostProtection() {
        val hostReason = FlagSecureReason(activity.window) { true }.apply { attach() }
        try {
            setAppLockEnabled(true)
            val window = requireNotNull(openDialog().window)
            setAppLockEnabled(false)

            assertTrue((window.attributes.flags and FLAG_SECURE) != 0)
            assertFalse(FlagSecureReason.isSecured(window))
        } finally {
            hostReason.detach()
        }
    }

    private fun openDialog(): Dialog {
        activity.setContentView(
            ComposeView(activity).apply {
                setParentCompositionContext(
                    createLifecycleAwareWindowRecomposer(Dispatchers.Main.immediate + frameClock, activity.lifecycle),
                )
                setContent {
                    MixinAppTheme(darkTheme = false) {
                        if (showDialog.value) {
                            val label = confirmText.value
                            MixinAlertDialog(
                                onDismissRequest = { showDialog.value = false },
                                onConfirmClick = { showDialog.value = false },
                                confirmText = label,
                                text = { SideEffect { renderedConfirmText = label } },
                            )
                        }
                    }
                }
            },
        )
        idle()
        return requireNotNull(ShadowDialog.getLatestDialog()).also {
            assertTrue(it.isShowing)
        }
    }

    private fun assertIgnoresLaterAppLockChanges(window: Window) {
        setAppLockEnabled(false)
        setAppLockEnabled(true)
        assertSecure(window, false)
    }

    private fun recomposeDialog() {
        confirmText.value = "Continue"
        idle()
        assertEquals("Continue", renderedConfirmText)
    }

    private fun setAppLockEnabled(enabled: Boolean) {
        MixinApplication.get().defaultSharedPreferences.edit().putInt(PREF_APP_AUTH, if (enabled) 0 else -1).commit()
    }

    private fun assertSecure(window: Window, expected: Boolean) {
        assertEquals(expected, (window.attributes.flags and FLAG_SECURE) != 0)
        assertEquals(expected, FlagSecureReason.isSecured(window))
    }

    private fun idle() {
        Snapshot.sendApplyNotifications()
        val looper = shadowOf(Looper.getMainLooper())
        looper.idle()
        frameClock.sendFrame(System.nanoTime())
        looper.idle()
        frameClock.sendFrame(System.nanoTime())
        looper.idle()
    }

    class TestApplication : MixinApplication() {
        override fun onCreate() {
            MixinApplication.appContext = this
        }
    }
}
