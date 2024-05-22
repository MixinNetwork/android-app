package one.mixin.android.ui.home.web3.swap

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun InputTextField(
    token: SwapToken?,
    text: String,
    onInputChanged: ((String) -> Unit) ?= null,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val valueText = remember { mutableStateOf(BigDecimal.ZERO) }

    BasicTextField(
        value = text,
        onValueChange = {
            val v = try {
                if (it.isBlank()) BigDecimal.ZERO else BigDecimal(it)
            } catch (e: Exception) {
                return@BasicTextField
            }
            // todo
            valueText.value = v.multiply(BigDecimal( token?.price ?: "0")).setScale(2, RoundingMode.CEILING)
            // TODO debounce
            onInputChanged?.invoke(it)
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
            fontSize = 18.sp,
            color = MixinAppTheme.colors.textPrimary,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.End
        ),
        cursorBrush = SolidColor(MixinAppTheme.colors.accent),
    ) { innerTextField ->
        Column(modifier = Modifier.fillMaxWidth()){
            Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                innerTextField()
            }
            Box(modifier = Modifier.width(8.dp))
            if (valueText.value != BigDecimal.ZERO) {
                Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "$${valueText.value.toPlainString()}", style = TextStyle(
                            fontSize = 16.sp,
                            color = MixinAppTheme.colors.textSubtitle,
                        )
                    )
                }
            } else {
                Box(modifier = Modifier.height(12.dp))  // placeholder
            }
        }
    }
}