package one.mixin.android.ui.group

import one.mixin.android.util.ConversationIdUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupTest {

    @Test
    fun testGroupConversationID() {
        val ownerID = "c8cb0ac7-d456-4341-be66-0b143aa09922"
        val groupName = "Mixin Rocks"
        val participantUserIDs = listOf(
            "f937ca18-d1ff-46f5-99e8-e23fbd6fd5f2",
            "0e0a20c8-31b8-4093-81b8-9cebd9bc8afc",
            "8391e472-cdbe-4704-be1f-7d184635b885",
            "831fdb67-13ed-4dc5-ac64-dda89aeda2bb",
            "f7ff9dde-18c2-4375-8097-b364068b120e",
            "088c1e3e-1f07-4065-85b5-6b49b4370d32"
        )
        val randomID = "01d21e2c-76f5-4940-8ea0-9b7f21728674"

        val conversationID = ConversationIdUtil.generateGroupConversationId(
            ownerId = ownerID,
            groupName = groupName,
            participants = participantUserIDs,
            randomId = randomID
        )

        assertEquals("5dac944e-2037-31b4-bbd9-e5fd3ffe571e", conversationID)
    }
}
