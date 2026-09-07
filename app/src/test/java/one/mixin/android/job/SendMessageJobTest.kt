package one.mixin.android.job

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import one.mixin.android.vo.ICategory
import one.mixin.android.vo.MessageCategory

class SendMessageJobTest {
    @Test
    fun appCardDescriptionIsUsedForOutgoingMentions() {
        val category =
            object : ICategory {
                override val type = MessageCategory.APP_CARD.name
            }

        assertEquals(
            "➡️ @7000105334 ⬅️",
            outgoingMentionContent(
                category,
                """{"description":"➡️ @7000105334 ⬅️"}""",
            ),
        )
    }

    @Test
    fun invalidAppCardHasNoOutgoingMentionContent() {
        val category =
            object : ICategory {
                override val type = MessageCategory.APP_CARD.name
            }

        assertNull(outgoingMentionContent(category, "invalid"))
    }
}
