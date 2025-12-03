package one.mixin.android.ui.wallet

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R

import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.landing.components.HighlightedTextWithClick

@Composable
fun ImportKeyPage(
    @DrawableRes image: Int,
    @StringRes title: Int,
    @StringRes description: Int,
    action: () -> Unit,
    dismiss: () -> Unit,
    learnMoreAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(topEnd = 12.dp, topStart = 12.dp))
            .background(MixinAppTheme.colors.primary)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = painterResource(id = image),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,) {
            Text(
                text = stringResource(id = title),
                color = MixinAppTheme.colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.W600
            )
            Spacer(modifier = Modifier.height(8.dp))
            HighlightedTextWithClick(
                stringResource(description, stringResource(R.string.Learn_More)),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                stringResource(R.string.Learn_More),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp,
                lineHeight = 21.sp
            ) {
                learnMoreAction.invoke()
            }
            Spacer(modifier = Modifier.height(50.dp))
            Spacer(modifier = Modifier.height(24.dp))
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
                Text(text = stringResource(id = R.string.Import), color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = dismiss,
            ) {
                Text(
                    text = stringResource(id = R.string.Not_Now),
                    color = MixinAppTheme.colors.accent
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
