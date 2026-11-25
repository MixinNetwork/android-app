package one.mixin.android.ui.common.compose

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncedTextFieldStateTest {
    @Test
    fun rapidAlternatingEditsBeforeEchoDoNotSwallowSubsequentProgrammaticMax() {
        val state = TextFieldState(initialText = "2")
        val syncState = TextFieldSyncState("2")
        val emitted = mutableListOf<String>()

        state.setTextAndPlaceCursorAtEnd("1")
        syncState.onFieldTextChanged("1") { emitted.add(it) }
        state.setTextAndPlaceCursorAtEnd("2")
        syncState.onFieldTextChanged("2") { emitted.add(it) }
        state.setTextAndPlaceCursorAtEnd("1")
        syncState.onFieldTextChanged("1") { emitted.add(it) }

        syncState.syncExternalValue("1", state)
        assertEquals("1", state.text.toString())

        syncState.syncExternalValue("2", state)
        assertEquals("2", state.text.toString())
        assertEquals(listOf("1", "2", "1"), emitted)
    }

    @Test
    fun programmaticUpdateBeforeEchoAppliesLatestExternalValue() {
        val state = TextFieldState(initialText = "2")
        val syncState = TextFieldSyncState("2")
        val emitted = mutableListOf<String>()

        state.setTextAndPlaceCursorAtEnd("1")
        syncState.onFieldTextChanged("1") { emitted.add(it) }

        syncState.syncExternalValue("2", state)
        assertEquals("2", state.text.toString())
        assertEquals(listOf("1"), emitted)
    }

    @Test
    fun matchingEchoPreservesCursorPosition() {
        val state = TextFieldState(initialText = "01", initialSelection = TextRange(1))
        val syncState = TextFieldSyncState("")
        val emitted = mutableListOf<String>()

        syncState.onFieldTextChanged("01") { emitted.add(it) }
        syncState.syncExternalValue("01", state)

        assertEquals("01", state.text.toString())
        assertEquals(TextRange(1), state.selection)
        assertEquals(listOf("01"), emitted)
    }

    @Test
    fun externalChangeUpdatesFieldWithoutEchoingCallback() {
        val state = TextFieldState(initialText = "12.3")
        val syncState = TextFieldSyncState("12.3")
        val emitted = mutableListOf<String>()

        syncState.syncExternalValue("", state)
        assertEquals("", state.text.toString())

        syncState.onFieldTextChanged(state.text.toString()) { emitted.add(it) }
        assertEquals(emptyList(), emitted)
    }
}
