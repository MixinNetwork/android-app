package one.mixin.android.ui.home.web3.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun OutlinedTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (selected) MixinAppTheme.colors.bgClip else Color.Transparent
    val borderColor = if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight
    val textColor = if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.textPrimary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = TextStyle(
                fontSize = 14.sp
            )
        )
    }
}