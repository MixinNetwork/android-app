package one.mixin.android.ui.home.web3.swap

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import one.mixin.android.api.response.Web3Token
import one.mixin.android.compose.theme.MixinAppTheme
import java.math.BigDecimal

@Composable
fun InputTextField(
    token: Web3Token?,
    text: MutableState<String>,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val valueText = remember { mutableStateOf(BigDecimal.ZERO) }

    BasicTextField(
        value = text.value,
        onValueChange = {
            text.value = it
            val v = try {
                if (it.isBlank()) BigDecimal.ZERO else BigDecimal(it)
            } catch (e: Exception) {
                return@BasicTextField
            }
            valueText.value = v.multiply(BigDecimal(token?.price ?: "0"))
        },
        modifier =
        Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (it.isFocused) {
                    keyboardController?.show()
                }
            },
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        textStyle =
        TextStyle(
            fontSize = 20.sp,
            color = MixinAppTheme.colors.textPrimary,
        ),
        cursorBrush = SolidColor(MixinAppTheme.colors.accent),
    ) { innerTextField ->
        Column {
            Box(contentAlignment = Alignment.CenterEnd) {
                innerTextField()
            }
            Box(modifier = Modifier.width(8.dp))
            Box(contentAlignment = Alignment.CenterEnd) {
                Text(text = "$${valueText.value.toPlainString()}")
            }
        }
    }
}