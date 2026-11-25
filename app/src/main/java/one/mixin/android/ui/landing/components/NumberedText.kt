package one.mixin.android.ui.landing.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun NumberedText(modifier: Modifier, numberStr: String, instructionStr: String, color: Color = MixinAppTheme.colors.textMinor) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = numberStr,
            modifier = Modifier
                .padding(top = 3.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(MixinAppTheme.colors.backgroundWindow),
            color = MixinAppTheme.colors.textMinor,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            style = TextStyle(textAlign = TextAlign.Center),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = instructionStr,
            modifier = Modifier.weight(1f),
            color = color,
            fontSize = 14.sp,
            lineHeight = 19.6.sp,
        )
    }
}
