package one.mixin.android.ui.landing.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun MnemonicPhrasePage(
) {
    MixinAppTheme {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(120.dp))
            Icon(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                painter = painterResource(R.drawable.ic_mnemonic_phrase),
                contentDescription = null,
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.Create_Mnemonic_Phrase),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontSize = 18.sp, fontWeight = FontWeight.W600, color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            NumberedText(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(), numberStr = " 1", instructionStr = stringResource(R.string.mnemonic_phrase_instruction_1)
            )
            Spacer(modifier = Modifier.height(16.dp))
            NumberedText(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(), numberStr = " 2", instructionStr = stringResource(R.string.mnemonic_phrase_instruction_2)
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = {
                },
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
                    text = stringResource(R.string.landing_have_account),
                    color = MixinAppTheme.colors.textBlue
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun NumberedText(modifier: Modifier, numberStr: String, instructionStr: String) {
    ConstraintLayout(modifier) {
        val (number, instruction) = createRefs()
        Text(
            text = numberStr,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(MixinAppTheme.colors.backgroundWindow)
                .padding(2.dp)
                .constrainAs(number) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top, 2.dp)
                },
            color = MixinAppTheme.colors.textMinor,
            fontSize = 12.sp,
            lineHeight = 12.sp
        )
        Text(
            text = instructionStr,
            modifier = Modifier.constrainAs(instruction) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                linkTo(start = number.end, end = parent.end, startMargin = 10.dp, bias = 0f)
                width = Dimension.fillToConstraints
            },
            color = MixinAppTheme.colors.textMinor,
            fontSize = 14.sp,
            lineHeight = 14.sp
        )
    }
}