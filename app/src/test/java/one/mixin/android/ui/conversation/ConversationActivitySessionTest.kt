package one.mixin.android.ui.conversation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.internal.managers.ActivityComponentManager
import one.mixin.android.MixinApplication
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.ChatHistoryActivity
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.media.pager.transcript.TranscriptMediaPagerActivity
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
import javax.inject.Inject
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ConversationActivitySessionTest.ShadowActivityComponentManager::class])
class ConversationActivitySessionTest {
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
    fun conversationActivityMissingAccountReturnsToLandingWithoutRestoringFragments() {
        val savedState = createSavedStateWithFragment()
        val intent = Intent(ApplicationProvider.getApplicationContext(), ConversationActivity::class.java)
            .putExtra(ConversationFragment.CONVERSATION_ID, "conversation-id")
        val controller = Robolectric.buildActivity(ConversationActivity::class.java, intent)
        ReflectionHelpers.setField(controller.get(), "injected", true)
        try {
            controller.create(savedState)

            val activity = controller.get()
            assertTrue(activity.isFinishing)
            controller.start()
            assertEquals(1, RestoredFragment.attachments)
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
    fun bubbleActivityMissingAccountReturnsToLandingWithoutRestoringFragments() {
        val savedState = createSavedStateWithFragment()
        val intent = Intent(ApplicationProvider.getApplicationContext(), BubbleActivity::class.java)
            .putExtra(ConversationFragment.CONVERSATION_ID, "conversation-id")
        val controller = Robolectric.buildActivity(BubbleActivity::class.java, intent)
        ReflectionHelpers.setField(controller.get(), "injected", true)
        try {
            controller.create(savedState)

            val activity = controller.get()
            assertTrue(activity.isFinishing)
            controller.start()
            assertEquals(1, RestoredFragment.attachments)
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
    fun chatHistoryActivityMissingAccountReturnsToLandingWithoutRestoringFragments() {
        val savedState = createSavedStateWithFragment()
        val intent = Intent(ApplicationProvider.getApplicationContext(), ChatHistoryActivity::class.java)
            .putExtra("conversation_id", "conversation-id")
            .putExtra("transcript_id", "message-id")
        val controller = Robolectric.buildActivity(ChatHistoryActivity::class.java, intent)
        ReflectionHelpers.setField(controller.get(), "injected", true)
        try {
            controller.create(savedState)

            val activity = controller.get()
            assertTrue(activity.isFinishing)
            controller.start()
            assertEquals(1, RestoredFragment.attachments)
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
    fun transcriptMediaPagerActivityMissingAccountReturnsToLandingWithoutRestoringFragments() {
        val savedState = createSavedStateWithFragment()
        val intent = Intent(ApplicationProvider.getApplicationContext(), TranscriptMediaPagerActivity::class.java)
        val controller = Robolectric.buildActivity(TranscriptMediaPagerActivity::class.java, intent)
        ReflectionHelpers.setField(controller.get(), "injected", true)
        try {
            controller.create(savedState)

            val activity = controller.get()
            assertTrue(activity.isFinishing)
            controller.start()
            assertEquals(1, RestoredFragment.attachments)
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
    fun injectedRepositoriesAreDeferredViaProvider() {
        for (
            activityClass in listOf(
                ConversationActivity::class.java,
                BubbleActivity::class.java,
                ChatHistoryActivity::class.java,
                TranscriptMediaPagerActivity::class.java,
            )
        ) {
            val injectedFields = activityClass.declaredFields.filter { it.isAnnotationPresent(Inject::class.java) }
            assertTrue(injectedFields.isNotEmpty())
            for (field in injectedFields) {
                assertEquals(
                    "${activityClass.simpleName}.${field.name} must use Provider to avoid eager DB creation",
                    Provider::class.java,
                    field.type,
                )
            }
        }
    }

    private fun createSavedStateWithFragment(): Bundle {
        val savedState = Bundle()
        val previous = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        previous.get().supportFragmentManager.beginTransaction().add(RestoredFragment(), "restored").commitNow()
        previous.saveInstanceState(savedState).pause().stop().destroy()
        assertEquals(1, RestoredFragment.attachments)
        return savedState
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
