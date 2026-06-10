package one.mixin.android.widget.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun MaterialWindowButton(onClick: () -> Unit, title: String) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        onClick = onClick,
        colors =
        ButtonDefaults.outlinedButtonColors(
            backgroundColor = MixinAppTheme.colors.backgroundWindow
        ),
        shape = RoundedCornerShape(32.dp),
        elevation =
        ButtonDefaults.elevation(
            pressedElevation = 0.dp,
            defaultElevation = 0.dp,
            hoveredElevation = 0.dp,
            focusedElevation = 0.dp,
        ),
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textBlue
        )
    }
}

@Composable
fun MixinButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(32.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 35.dp, vertical = 10.dp),
    elevation: Dp = 0.dp,
    backgroundColor: Color = MixinAppTheme.colors.accent,
    contentColor: Color = Color.White,
    disabledBackgroundColor: Color = backgroundColor.copy(alpha = 0.4f),
    disabledContentColor: Color = contentColor.copy(alpha = 0.6f),
    content: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            disabledBackgroundColor = disabledBackgroundColor,
            disabledContentColor = disabledContentColor,
        ),
        shape = shape,
        contentPadding = contentPadding,
        elevation = ButtonDefaults.elevation(
            defaultElevation = elevation,
            pressedElevation = elevation,
            hoveredElevation = elevation,
            focusedElevation = elevation,
        ),
    ) {
        content()
    }
}
