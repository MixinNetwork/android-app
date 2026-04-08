package one.mixin.android.widget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    text: String? = null,
    content: (@Composable () -> Unit)? = null,
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
        when {
            text != null -> {
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W500
                )
            }
            content != null -> content()
        }
    }
}


@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledBackgroundColor: Color = backgroundColor.copy(alpha = 0.4f),
    disabledContentColor: Color = contentColor.copy(alpha = 0.6f),
) {
    MixinButton(
        onClick = onClick,
        enabled = enabled,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor,
        shape = RoundedCornerShape(30.dp),
        contentPadding = PaddingValues(horizontal = 35.dp, vertical = 10.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = if (enabled) contentColor else disabledContentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.W400
        )
    }
}

@Composable
fun ActionBottom(
    modifier: Modifier,
    cancelTitle: String,
    confirmTitle: String,
    cancelAction: () -> Unit,
    confirmAction: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .background(MixinAppTheme.colors.background)
                .padding(8.dp)
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        ActionButton(
            text = cancelTitle,
            onClick = cancelAction,
            backgroundColor = MixinAppTheme.colors.backgroundWindow,
            contentColor = MixinAppTheme.colors.textPrimary
        )
        Box(modifier = Modifier.width(36.dp))
        ActionButton(
            text = confirmTitle,
            onClick = confirmAction,
            backgroundColor = MixinAppTheme.colors.accent,
            contentColor = Color.White
        )
    }
}
