package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
internal fun PerpsInfoLabel(
    text: String,
    onTipClick: (() -> Unit)?,
    fontSize: TextUnit = 14.sp,
    color: Color = MixinAppTheme.colors.textAssist,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    textClickable: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            fontSize = fontSize,
            color = color,
            style = style,
            maxLines = maxLines,
            modifier = if (onTipClick != null && textClickable) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTipClick,
                )
            } else {
                Modifier
            },
        )
        if (onTipClick != null) {
            Icon(
                painter = painterResource(R.drawable.ic_tip),
                contentDescription = text,
                tint = MixinAppTheme.colors.textAssist,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onTipClick)
                    .padding(6.dp),
            )
        }
    }
}
