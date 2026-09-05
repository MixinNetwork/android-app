package one.mixin.android.ui.conversation

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ConversationActivityInitialPositionTest {
    @Test
    fun fastShowCarriesUnreadAnchorSeparatelyFromExplicitJump() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        ConversationActivity.fastShow(
            context = activity,
            conversationId = "conversation-id",
            recipient = null,
            initialUnreadMessageId = "unread-message-id",
            initialUnreadCount = 12,
        )

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(
            "unread-message-id",
            intent.getStringExtra(ConversationFragment.INITIAL_UNREAD_MESSAGE_ID),
        )
        assertEquals(12, intent.getIntExtra(ConversationFragment.INITIAL_UNREAD_COUNT, -1))
        assertNull(intent.getStringExtra(ConversationFragment.MESSAGE_ID))
    }
}
