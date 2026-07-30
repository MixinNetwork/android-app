package one.mixin.android.ui.conversation.holder

import kotlin.test.Test
import kotlin.test.assertEquals

class AppCardTest {
    @Test
    fun mentionIdentityNumberUsesFullName() {
        assertEquals(
            "Pump",
            appCardMentionDisplayText(
                "7000105334",
                mapOf("7000105334" to "Pump"),
            ),
        )
    }

    @Test
    fun unknownMentionKeepsIdentityNumber() {
        assertEquals(
            "7000105334",
            appCardMentionDisplayText("7000105334", emptyMap()),
        )
    }
}
