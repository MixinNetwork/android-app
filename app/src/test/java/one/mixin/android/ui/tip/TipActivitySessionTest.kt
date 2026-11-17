package one.mixin.android.ui.tip

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.internal.managers.ActivityComponentManager
import one.mixin.android.MixinApplication
import one.mixin.android.session.Session
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.vo.Account
import org.junit.After
import org.junit.Assert.assertEquals
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
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [TipActivitySessionTest.ShadowActivityComponentManager::class])
class TipActivitySessionTest {
    private var previousAccount: Account? = null
    private var previousContext: Context? = null

    @Before
    fun setUp() {
        previousAccount = ReflectionHelpers.getField(Session, "self")
        previousContext = ReflectionHelpers.getStaticField(MixinApplication::class.java, "appContext")
        MixinApplication.appContext = ApplicationProvider.getApplicationContext()
        MixinApplication.appContext.getSharedPreferences(Session.PREF_SESSION, Context.MODE_PRIVATE).edit().clear().commit()
        ReflectionHelpers.setField(Session, "self", null)
        RestoredFragment.attachments = 0
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setField(Session, "self", previousAccount)
        ReflectionHelpers.setStaticField(MixinApplication::class.java, "appContext", previousContext)
    }

    @Test
    fun missingAccountReturnsToLandingBeforeStartingTip() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TipActivity::class.java)
            .putExtra(TipFragment.ARGS_TIP_BUNDLE, TipBundle(TipType.Upgrade, "device-id", TryConnecting))
        val controller = Robolectric.buildActivity(TipActivity::class.java, intent)
        ReflectionHelpers.setField(controller.get(), "injected", true)
        try {
            controller.create()

            val activity = controller.get()
            assertTrue(activity.isFinishing)
            controller.start()
            assertTrue(activity.supportFragmentManager.fragments.isEmpty())
            val redirect = shadowOf(activity).nextStartedActivity
            assertEquals(LandingActivity::class.java.name, redirect.component?.className)
            assertTrue(redirect.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
            assertNull(shadowOf(activity).nextStartedActivity)
        } finally {
            if (controller.get().isFinishing) controller.stop().destroy()
        }
    }

    @Test
    fun missingAccountDoesNotRestoreAccountDependentFragments() {
        val savedState = Bundle()
        val previous = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        previous.get().supportFragmentManager.beginTransaction().add(RestoredFragment(), "restored").commitNow()
        previous.saveInstanceState(savedState).pause().stop().destroy()
        assertEquals(1, RestoredFragment.attachments)

        val intent = Intent(ApplicationProvider.getApplicationContext(), TipActivity::class.java)
            .putExtra(TipFragment.ARGS_TIP_BUNDLE, TipBundle(TipType.Upgrade, "device-id", TryConnecting))
        val controller = Robolectric.buildActivity(TipActivity::class.java, intent)
        ReflectionHelpers.setField(controller.get(), "injected", true)
        try {
            controller.create(savedState)

            assertTrue(controller.get().isFinishing)
            controller.start()
            assertEquals(1, RestoredFragment.attachments)
            assertTrue(controller.get().supportFragmentManager.fragments.isEmpty())
            assertEquals(LandingActivity::class.java.name, shadowOf(controller.get()).nextStartedActivity.component?.className)
        } finally {
            if (controller.get().isFinishing) controller.stop().destroy()
        }
    }

    @Implements(ActivityComponentManager::class, isInAndroidSdk = false)
    class ShadowActivityComponentManager {
        @Implementation
        fun initSavedStateHandleHolders() = Unit

        @Implementation
        fun clearSavedStateHandleHolders() = Unit
    }

    class RestoredFragment : Fragment() {
        override fun onAttach(context: Context) {
            super.onAttach(context)
            attachments++
        }

        companion object {
            var attachments = 0
        }
    }
}
