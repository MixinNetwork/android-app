package one.mixin.android.util

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NotificationIdsTest {
    @Test
    fun conversationNotificationIdDoesNotDependOnMessageId() {
        val conversationId = "conversation-id"

        val firstMessageNotificationId = conversationNotificationId(conversationId)
        val secondMessageNotificationId = conversationNotificationId(conversationId)

        assertEquals(firstMessageNotificationId, secondMessageNotificationId)
    }

    @Test
    fun conversationSummaryNotificationIdIsStableAndDifferentFromConversationNotificationId() {
        val conversationId = "conversation-id"

        val summaryNotificationId = conversationSummaryNotificationId(conversationId)

        assertEquals(summaryNotificationId, conversationSummaryNotificationId(conversationId))
        assertNotEquals(conversationNotificationId(conversationId), summaryNotificationId)
    }

    @Test
    fun bubbleShortcutIdIsStableForConversation() {
        val conversationId = "conversation-id"

        assertEquals("Bubble-conversation-id", bubbleShortcutId(conversationId))
        assertEquals(bubbleShortcutId(conversationId), bubbleShortcutId(conversationId))
    }
}
