package one.mixin.android.widget.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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