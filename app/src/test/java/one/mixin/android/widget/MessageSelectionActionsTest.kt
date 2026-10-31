package one.mixin.android.widget

import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.create
import one.mixin.android.websocket.PinAction
import one.mixin.android.widget.ToolView.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageSelectionActionsTest {
    @Test
    fun selectionKeepsOnlyActionsSupportedByAllSelectedMessages() {
        val text = create(MessageCategory.PLAIN_TEXT.name, "2026-09-23T00:00:00Z")
        assertEquals(
            listOf(Action.REPLY, Action.COPY, Action.FORWARD, Action.PIN, Action.DELETE, Action.SELECT),
            messageSelectionActions(listOf(text), PinAction.PIN),
        )
        assertFalse(Action.PIN in messageSelectionActions(listOf(text)))
        assertTrue(Action.UNPIN in messageSelectionActions(listOf(text), PinAction.UNPIN))
        assertEquals(
            listOf(Action.FORWARD, Action.DELETE),
            messageSelectionActions(listOf(text, text.copy(messageId = "second")), PinAction.PIN),
        )
        assertTrue(Action.COPY in messageSelectionActions(listOf(text)))
        assertTrue(messageSelectionActions(emptyList()).isEmpty())

        val restricted = text.copy(type = MessageCategory.SYSTEM_SAFE_SNAPSHOT.name)
        assertEquals(listOf(Action.DELETE, Action.SELECT), messageSelectionActions(listOf(restricted), PinAction.PIN))
        assertEquals(listOf(Action.DELETE), messageSelectionActions(listOf(text, restricted), PinAction.PIN))
        assertTrue(Action.FORWARD in messageSelectionActions(List(99) { text.copy(messageId = it.toString()) }))
        assertEquals(listOf(Action.DELETE), messageSelectionActions(List(100) { text.copy(messageId = it.toString()) }))
        assertFalse(Action.PIN in messageSelectionActions(listOf(text.copy(status = MessageStatus.SENDING.name)), PinAction.PIN))

        val image = text.copy(type = MessageCategory.PLAIN_IMAGE.name, mediaStatus = MediaStatus.DONE.name)
        val file = text.copy(type = MessageCategory.PLAIN_DATA.name, mediaStatus = MediaStatus.DONE.name)
        assertTrue(Action.ADD_STICKER in messageSelectionActions(listOf(image)))
        assertFalse(Action.COPY in messageSelectionActions(listOf(image)))
        assertTrue(Action.SHARE in messageSelectionActions(listOf(file)))
        assertFalse(Action.ADD_STICKER in messageSelectionActions(listOf(file)))
    }
}
