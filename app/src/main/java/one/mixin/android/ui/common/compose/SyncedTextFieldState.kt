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

@Composable
internal fun rememberSyncedTextFieldState(
    value: String,
    onValueChange: (String) -> Unit,
    saveable: Boolean = true,
): TextFieldState {
    val state = if (saveable) rememberTextFieldState(initialText = value) else remember { TextFieldState(initialText = value) }
    val latestValue by rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(value) {
        if (state.text.toString() != value) {
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { current ->
            if (current != latestValue) {
                latestOnValueChange(current)
            }
        }
    }
    return state
}
