package one.mixin.android.widget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun DotText(
    text: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 5.dp,
    color: Color = MixinAppTheme.colors.textAssist,
    style: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Spacer(modifier= Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .padding(end = 6.dp, top = 5.dp)
                .size(dotSize)
                .background(color = color, shape = CircleShape),
        )
        Spacer(modifier= Modifier.width(5.dp))
        Text(
            text = text,
            style = style,
            color = color,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
