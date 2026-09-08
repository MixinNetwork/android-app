package one.mixin.android.vo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit

class MessageRecallTest {
    @Test
    fun `sender can recall own group message within thirty days`() {
        assertTrue(message("me", 29).canRecallBy("me", isGroup = true, canManageGroup = false))
    }

    @Test
    fun `regular member cannot recall another group member message`() {
        assertFalse(message("other", 29).canRecallBy("me", isGroup = true, canManageGroup = false))
    }

    @Test
    fun `group manager can recall any member message`() {
        assertTrue(message("other", 29).canRecallBy("me", isGroup = true, canManageGroup = true))
    }

    @Test
    fun `contact can recall the other participant message`() {
        assertTrue(message("other", 29).canRecallBy("me", isGroup = false, canManageGroup = false))
    }

    @Test
    fun `message older than thirty days cannot be recalled`() {
        assertFalse(message("me", 31).canRecallBy("me", isGroup = false, canManageGroup = false))
    }

    private fun message(
        userId: String,
        daysAgo: Long,
    ) =
        create(
            MessageCategory.PLAIN_TEXT.name,
            Instant.now().minus(daysAgo, ChronoUnit.DAYS).toString(),
        ).copy(
            userId = userId,
            status = MessageStatus.SENT.name,
        )
}
