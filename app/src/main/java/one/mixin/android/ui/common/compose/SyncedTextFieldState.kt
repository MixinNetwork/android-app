package one.mixin.android.ui.common.compose

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

@Composable
internal fun rememberSyncedTextFieldState(
    value: String,
    onValueChange: (String) -> Unit,
    saveable: Boolean = false,
): TextFieldState {
    val state = if (saveable) rememberTextFieldState(initialText = value) else remember { TextFieldState(initialText = value) }
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val pendingEmissions = remember { ArrayDeque<String>() }
    var lastExternalValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        val echoIndex = pendingEmissions.indexOf(value)
        if (echoIndex >= 0) {
            repeat(echoIndex + 1) { pendingEmissions.removeFirst() }
            lastExternalValue = value
            return@LaunchedEffect
        }
        pendingEmissions.clear()
        lastExternalValue = value
        if (state.text.toString() != value) {
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { current ->
            if (current != lastExternalValue) {
                pendingEmissions.addLast(current)
                latestOnValueChange(current)
            }
        }
    }
    return state
}
