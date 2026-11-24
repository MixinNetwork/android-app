package one.mixin.android.ui.common.compose

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow

private class TextFieldSyncState(var lastExternalValue: String) {
    val pendingEmissions = ArrayDeque<String>()
}

@Composable
internal fun rememberSyncedTextFieldState(
    value: String,
    onValueChange: (String) -> Unit,
    saveable: Boolean = false,
): TextFieldState {
    val state = if (saveable) rememberTextFieldState(initialText = value) else remember { TextFieldState(initialText = value) }
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val syncState = remember { TextFieldSyncState(value) }

    LaunchedEffect(value) {
        val echoIndex = syncState.pendingEmissions.indexOf(value)
        if (echoIndex >= 0) {
            repeat(echoIndex + 1) { syncState.pendingEmissions.removeFirst() }
            syncState.lastExternalValue = value
            return@LaunchedEffect
        }
        syncState.pendingEmissions.clear()
        syncState.lastExternalValue = value
        if (state.text.toString() != value) {
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { current ->
            val lastDispatched = syncState.pendingEmissions.lastOrNull() ?: syncState.lastExternalValue
            if (current != lastDispatched) {
                syncState.pendingEmissions.addLast(current)
                latestOnValueChange(current)
            }
        }
    }
    return state
}
