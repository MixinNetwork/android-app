package one.mixin.android.ui.conversation

import android.app.Activity
import android.content.Intent
import com.google.gson.Gson
import one.mixin.android.session.Session
import one.mixin.android.vo.Account
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
