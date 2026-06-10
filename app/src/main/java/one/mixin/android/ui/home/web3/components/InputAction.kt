package one.mixin.android.ui.home.web3.components


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun InputAction(
    text: String,
    modifier: Modifier = Modifier,
    showBorder: Boolean = true,
    horizontalPadding: Dp = 20.dp,
    verticalPadding: Dp = 6.dp,
    fontSize: TextUnit = 14.sp,
    onAction: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = if (showBorder) {
            modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(MixinAppTheme.colors.background)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    onAction.invoke()
                }
                .defaultMinSize(minHeight = 32.dp)
                .padding(horizontalPadding, verticalPadding)
        } else {
            modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    onAction.invoke()
                }
                .defaultMinSize(minHeight = 32.dp)
                .padding(verticalPadding, verticalPadding)
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = fontSize,
                lineHeight = 16.sp,
                color = if (isPressed) MixinAppTheme.colors.textAssist else MixinAppTheme.colors.textPrimary,
            ),
        )
    }
}

@Preview
@Composable
fun PreviewInputActionMax() {
    MixinAppTheme {
        InputAction("MAX") {}
    }
}
