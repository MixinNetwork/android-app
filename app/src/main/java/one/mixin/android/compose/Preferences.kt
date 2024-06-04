package one.mixin.android.compose

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putLong
import one.mixin.android.extension.putString

@Composable
fun SharedPreferences.stringValueAsState(
    key: String,
    defaultValue: String,
) =
    value(
        getValue = { getString(key, defaultValue) },
        setValue = { putString(key, it) },
    )

@Composable
fun SharedPreferences.booleanValueAsState(
    key: String,
    defaultValue: Boolean,
) =
    value(
        getValue = { getBoolean(key, defaultValue) },
        setValue = { putBoolean(key, it) },
    )

@Composable
fun SharedPreferences.intValueAsState(
    key: String,
    defaultValue: Int,
) =
    value(
        getValue = { getInt(key, defaultValue) },
        setValue = { putInt(key, it) },
    )

@Composable
fun SharedPreferences.longValueAsState(
    key: String,
    defaultValue: Long,
) =
    value(
        getValue = { getLong(key, defaultValue) },
        setValue = { putLong(key, it) },
    )

@Composable
private fun <T> value(
    getValue: () -> T,
    setValue: (T) -> Unit,
): MutableState<T> {
    val state =
        remember {
            mutableStateOf(getValue())
        }
    LaunchedEffect(state.value) {
        setValue(state.value)
    }
    return state
}
