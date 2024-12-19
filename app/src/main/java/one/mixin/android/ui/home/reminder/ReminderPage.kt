package one.mixin.android.ui.home.reminder

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun ReminderPage(@DrawableRes contentImage: Int, @StringRes title: Int, @StringRes content: Int, @StringRes actionStr: Int, action: () -> Unit, dismiss: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
            .clip(RoundedCornerShape(topEnd = 12.dp, topStart = 12.dp))
            .background(MixinAppTheme.colors.primary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MixinAppTheme.colors.bgGradientStart,
                            MixinAppTheme.colors.bgGradientEnd
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(0f, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(horizontal = 22.dp)
                .padding(top = 30.dp)
        ) {
            Image(
                painter = painterResource(id = contentImage),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(42.dp))
            Text(
                stringResource(title), color = MixinAppTheme.colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.W600
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                stringResource(content), color = MixinAppTheme.colors.textAssist
            )
            Spacer(modifier = Modifier.height(50.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = action,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        backgroundColor = MixinAppTheme.colors.accent
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
                    text = stringResource(actionStr),
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        dismiss.invoke()
                    },
                text = stringResource(R.string.Not_Now),
                color = MixinAppTheme.colors.textBlue
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}