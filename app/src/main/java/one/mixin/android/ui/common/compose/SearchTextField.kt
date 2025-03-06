package one.mixin.android.ui.common.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun SearchTextField(
    text: MutableState<String>,
    hint: String,
    color: Color = MixinAppTheme.colors.background,
    h: Dp? = 60.dp
) {
    val focusRequester =
        remember {
            FocusRequester()
        }

    val keyboardController = LocalSoftwareKeyboardController.current

    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        value = text.value,
        onValueChange = { text.value = it },
        modifier =
            Modifier
                .fillMaxWidth()
                .apply {
                    if (h != null) {
                        this.height(h)
                    }
                }
                .focusRequester(focusRequester)
                .background(color)
                .onFocusChanged {
                    if (it.isFocused) {
                        keyboardController?.show()
                    }
                },
        interactionSource = interactionSource,
        textStyle =
            TextStyle(
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textPrimary,
            ),
        cursorBrush = SolidColor(MixinAppTheme.colors.accent),
    ) { innerTextField ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                tint = MixinAppTheme.colors.textAssist,
                contentDescription = null,
            )
            Box(modifier = Modifier.width(8.dp))
            Box(contentAlignment = Alignment.CenterStart) {
                innerTextField()
                if (text.value.isEmpty()) {
                    Text(
                        text = hint,
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist,
                    )
                }
            }
            Box(modifier = Modifier.width(16.dp))
        }
    }
}
