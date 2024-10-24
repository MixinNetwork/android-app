package one.mixin.android.ui.landing.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun NumberedText(modifier: Modifier, numberStr: String, instructionStr: String, color: Color = MixinAppTheme.colors.textMinor) {
    ConstraintLayout(modifier) {
        val (number, instruction) = createRefs()
        Text(
            text = numberStr,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(MixinAppTheme.colors.backgroundWindow)
                .constrainAs(number) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top, 2.dp)
                },
            color = MixinAppTheme.colors.textMinor,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            style = TextStyle(textAlign = TextAlign.Center)
        )
        Text(
            text = instructionStr,
            modifier = Modifier.constrainAs(instruction) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                linkTo(start = number.end, end = parent.end, startMargin = 10.dp, bias = 0f)
                width = Dimension.fillToConstraints
            },
            color = color,
            fontSize = 14.sp,
            lineHeight = 14.sp
        )
    }
}
