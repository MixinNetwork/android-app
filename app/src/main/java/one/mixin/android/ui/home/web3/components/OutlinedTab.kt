package one.mixin.android.ui.home.web3.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun OutlinedTab(
    text: String,
    selected: Boolean,
    showBadge: Boolean = false,
    badgeOffsetX: Dp = (-4).dp,
    badgeOffsetY: Dp = -4.dp,
    onClick: () -> Unit,
) {
    val backgroundColor = if (selected) MixinAppTheme.colors.bgClip else Color.Transparent
    val borderColor = if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.borderGray
    val textColor = if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.textPrimary
    val tabShape: Shape = RoundedCornerShape(20.dp)
    Box(
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(tabShape)
                .background(backgroundColor)
                .border(width = 1.dp, color = borderColor, shape = tabShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = textColor,
                style = TextStyle(
                    fontSize = 14.sp,
                ),
            )
        }
        if (showBadge) {
            BadgeDot(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = badgeOffsetX, y = badgeOffsetY),
            )
        }
    }
}

@Composable
private fun BadgeDot(modifier: Modifier = Modifier) {
    val badgeSize: Dp = 12.dp
    val badgeStrokeWidth: Dp = 1.5.dp
    val badgeShape: Shape = RoundedCornerShape(badgeSize)
    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(badgeShape)
            .background(MixinAppTheme.colors.badgeRed)
            .border(
                width = badgeStrokeWidth,
                color = MixinAppTheme.colors.backgroundWindow,
                shape = badgeShape,
            ),
    )
}