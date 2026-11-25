package one.mixin.android.ui.common.compose

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow

internal class TextFieldSyncState(
    private var lastSyncedValue: String,
) {
    fun syncExternalValue(
        value: String,
        state: TextFieldState,
    ) {
        if (value == lastSyncedValue) return
        lastSyncedValue = value
        if (state.text.toString() != value) {
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }

    fun onFieldTextChanged(
        current: String,
        onValueChange: (String) -> Unit,
    ) {
        if (current == lastSyncedValue) return
        lastSyncedValue = current
        onValueChange(current)
    }
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

    SideEffect {
        syncState.syncExternalValue(value, state)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { current ->
            syncState.onFieldTextChanged(current, latestOnValueChange)
        }
    }
    return state
}
