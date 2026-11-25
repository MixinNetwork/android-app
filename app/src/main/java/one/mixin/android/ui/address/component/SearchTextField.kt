package one.mixin.android.ui.address.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.common.compose.rememberSyncedTextFieldState

@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MixinAppTheme.colors.backgroundWindow,
    hint: String = stringResource(R.string.Search),
) {
    val textFieldState = rememberSyncedTextFieldState(value, onValueChange)
    BasicTextField(
        state = textFieldState,
        modifier = modifier
            .background(
                backgroundColor,
                RoundedCornerShape(32.dp)
            )
            .padding(vertical = 2.dp),
        textStyle = TextStyle(
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textPrimary
        ),
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(MixinAppTheme.colors.textBlue),
        decorator = { innerTextField ->
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .height(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                    modifier = Modifier.size(16.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textFieldState.text.isEmpty()) {
                        Text(
                            hint,
                            color = MixinAppTheme.colors.textAssist,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
                if (textFieldState.text.isNotEmpty()) {
                    IconButton(
                        onClick = { textFieldState.clearText() },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_float_close),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    )
}