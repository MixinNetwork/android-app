package one.mixin.android.ui.conversation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.vo.Account
import com.google.gson.Gson
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
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class ConversationActivityNavigationTest {
    private var previousAccount: Account? = null

    @Before
    fun setUp() {
        previousAccount = ReflectionHelpers.getField(Session, "self")
        ReflectionHelpers.setField(Session, "self", Gson().fromJson("{\"user_id\":\"self-id\"}", Account::class.java))
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setField(Session, "self", previousAccount)
    }

    @Test
    fun showAndClearOpensContactWithoutAnEmptyIntermediateActivity() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        ConversationActivity.showAndClear(
            context = activity,
            conversationId = null,
            recipientId = "recipient-id",
        )

        val shadowActivity = shadowOf(activity)
        val intent = shadowActivity.nextStartedActivity
        assertEquals(ConversationActivity::class.java.name, intent.component?.className)
        assertEquals("recipient-id", intent.getStringExtra(ConversationFragment.RECIPIENT_ID))
        assertNull(shadowActivity.nextStartedActivity)
        assertEquals(Intent.FLAG_ACTIVITY_CLEAR_TOP, intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    @Test
    fun showAndClearPreservesMessageJumpAndClearTopFlag() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        ConversationActivity.showAndClear(
            context = activity,
            conversationId = "conversation-id",
            messageId = "message-id",
            keyword = "keyword",
        )

        val shadowActivity = shadowOf(activity)
        val intent = shadowActivity.nextStartedActivity
        assertEquals("conversation-id", intent.getStringExtra(ConversationFragment.CONVERSATION_ID))
        assertEquals("message-id", intent.getStringExtra(ConversationFragment.MESSAGE_ID))
        assertEquals("keyword", intent.getStringExtra(ConversationFragment.KEY_WORD))
        assertNull(shadowActivity.nextStartedActivity)
        assertEquals(Intent.FLAG_ACTIVITY_CLEAR_TOP, intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    @Test
    fun shouldReturnToMainOnExitRequiresTaskRootActiveSessionAndNoSuppression() {
        assertTrue(
            ConversationActivity.shouldReturnToMainOnExit(
                isTaskRoot = true,
                hasSession = true,
                suppressHomeFallback = false,
            ),
        )
        assertFalse(
            ConversationActivity.shouldReturnToMainOnExit(
                isTaskRoot = false,
                hasSession = true,
                suppressHomeFallback = false,
            ),
        )
        assertFalse(
            ConversationActivity.shouldReturnToMainOnExit(
                isTaskRoot = true,
                hasSession = false,
                suppressHomeFallback = false,
            ),
        )
        assertFalse(
            ConversationActivity.shouldReturnToMainOnExit(
                isTaskRoot = true,
                hasSession = true,
                suppressHomeFallback = true,
            ),
        )
    }

    @Test
    fun mainActivityShowAndSingleIntentPairClearTaskWithNewTask() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        MainActivity.show(activity)
        val showIntent = shadowOf(activity).nextStartedActivity
        assertEquals(MainActivity::class.java.name, showIntent.component?.className)
        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
            showIntent.flags and (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )

        val singleIntent = MainActivity.getSingleIntent(activity)
        assertEquals(MainActivity::class.java.name, singleIntent.component?.className)
        assertTrue(singleIntent.hasCategory(Intent.CATEGORY_LAUNCHER))
        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
            singleIntent.flags and (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        )
    }

    @Test
    fun baseActivityDispatchesFragmentBackAndBackStackBeforeRootBackHook() {
        val controller = Robolectric.buildActivity(TestRootBackActivity::class.java).setup()
        try {
            val activity = controller.get()
            val fragment = TestBackFragment().apply { consumeBack = true }
            activity.supportFragmentManager.beginTransaction()
                .add(fragment, "root")
                .commitNow()
            activity.supportFragmentManager.beginTransaction()
                .add(Fragment(), "child")
                .addToBackStack("child")
                .commit()
            activity.supportFragmentManager.executePendingTransactions()

            // BaseFragment.onBackPressed() handles back first without popping or calling root hook.
            activity.onBackPressedDispatcher.onBackPressed()
            assertEquals(1, fragment.backPressedCount)
            assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
            assertEquals(0, activity.rootBackCount)

            // With fragment not consuming, back stack entry pops before calling root hook.
            fragment.consumeBack = false
            activity.onBackPressedDispatcher.onBackPressed()
            activity.supportFragmentManager.executePendingTransactions()
            assertEquals(2, fragment.backPressedCount)
            assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
            assertEquals(0, activity.rootBackCount)

            // With empty back stack and unconsumed fragment back, root hook handles back.
            activity.handleRootBack = true
            activity.onBackPressedDispatcher.onBackPressed()
            assertEquals(3, fragment.backPressedCount)
            assertEquals(1, activity.rootBackCount)
            assertFalse(activity.isFinishing)

            // When root hook returns false, default fallback finishes the activity.
            activity.handleRootBack = false
            activity.onBackPressedDispatcher.onBackPressed()
            assertEquals(2, activity.rootBackCount)
            assertTrue(activity.isFinishing)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    class TestRootBackActivity : BaseActivity() {
        var handleRootBack = true
        var rootBackCount = 0

        override fun onCreate(savedInstanceState: Bundle?) {
            skipSystemUi = true
            super.onCreate(savedInstanceState)
        }

        override fun onBackPressedAtRoot(): Boolean {
            rootBackCount++
            return handleRootBack
        }
    }

    class TestBackFragment : BaseFragment() {
        var consumeBack = false
        var backPressedCount = 0

        override fun onBackPressed(): Boolean {
            backPressedCount++
            return consumeBack
        }
    }
}
