package one.mixin.android

import android.app.Activity
import android.content.Intent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.extension.remove
import one.mixin.android.ui.landing.InitializeActivity
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(application = OldVersionForegroundGuardTest.TestApplication::class, manifest = Config.NONE, sdk = [29])
class OldVersionForegroundGuardTest {
    class TestApplication : MixinApplication() {
        override fun onCreate() {
            MixinApplication.appContext = this
        }
    }

    private lateinit var app: TestApplication

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication() as TestApplication
        MixinApplication.appContext = app
        app.defaultSharedPreferences.edit().clear().commit()
        val blockedFlag: AtomicBoolean = ReflectionHelpers.getField(app, "oldVersionBlocked")
        blockedFlag.set(false)
        shadowOf(app).clearNextStartedActivities()
    }

    @After
    fun tearDown() {
        app.defaultSharedPreferences.edit().clear().commit()
        val blockedFlag: AtomicBoolean = ReflectionHelpers.getField(app, "oldVersionBlocked")
        blockedFlag.set(false)
    }

    @Test
    fun onActivityStartedRedirectsNonAlertActivityWhenOldVersionIsBlocked() {
        app.defaultSharedPreferences.putInt(Constants.Account.PREF_OLD_VERSION_CODE, BuildConfig.VERSION_CODE)
        app.defaultSharedPreferences.putInt(Constants.Account.PREF_APP_AUTH, 0)
        val activity = Robolectric.buildActivity(Activity::class.java).get()

        app.onActivityStarted(activity)

        assertTrue(activity.isFinishing)
        val launched = shadowOf(app).nextStartedActivity
        assertNotNull(launched)
        assertEquals(InitializeActivity::class.java.name, launched.component?.className)
        assertTrue(launched.getBooleanExtra(InitializeActivity.OLD_VERSION, false))
        assertTrue(launched.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(launched.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
    }

    @Test
    fun onActivityStartedRedirectsInitializeActivityWithoutOldVersionExtra() {
        app.defaultSharedPreferences.putInt(Constants.Account.PREF_OLD_VERSION_CODE, BuildConfig.VERSION_CODE)
        val nonAlertIntent = Intent(app, InitializeActivity::class.java).putExtra(InitializeActivity.OLD_VERSION, false)
        val activity = Robolectric.buildActivity(InitializeActivity::class.java, nonAlertIntent).get()

        app.onActivityStarted(activity)

        assertTrue(activity.isFinishing)
        val launched = shadowOf(app).nextStartedActivity
        assertNotNull(launched)
        assertTrue(launched.getBooleanExtra(InitializeActivity.OLD_VERSION, false))
    }

    @Test
    fun onActivityStartedDoesNotLoopOrShowAppAuthOnOldVersionAlertActivity() {
        app.defaultSharedPreferences.putInt(Constants.Account.PREF_OLD_VERSION_CODE, BuildConfig.VERSION_CODE)
        app.defaultSharedPreferences.putInt(Constants.Account.PREF_APP_AUTH, 0)
        val alertIntent = Intent(app, InitializeActivity::class.java).putExtra(InitializeActivity.OLD_VERSION, true)
        val alertActivity = Robolectric.buildActivity(InitializeActivity::class.java, alertIntent).get()

        app.onActivityStarted(alertActivity)

        assertFalse(alertActivity.isFinishing)
        assertNull(shadowOf(app).nextStartedActivity)
        assertFalse(app.checkAndShowAppAuth(alertActivity))
    }

    @Test
    fun onActivityStartedDoesNotRedirectWhenVersionIsNotBlocked() {
        app.defaultSharedPreferences.remove(Constants.Account.PREF_OLD_VERSION_CODE)
        val activity = Robolectric.buildActivity(Activity::class.java).get()

        app.onActivityStarted(activity)

        assertFalse(activity.isFinishing)
        assertNull(shadowOf(app).nextStartedActivity)
    }
}
