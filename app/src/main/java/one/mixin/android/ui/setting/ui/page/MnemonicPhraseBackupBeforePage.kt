package one.mixin.android.ui.setting.ui.page

import PageScaffold
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.landing.components.NumberedText

@Composable
fun MnemonicPhraseBackupBeforePage(pop: () -> Unit, next: () -> Unit) {
    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.Mnemonic_Phrase),
            verticalScrollable = false,
            pop = pop,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(50.dp))
                Icon(painter = painterResource(R.drawable.ic_mnemonic_phrase), tint = Color.Unspecified, contentDescription = null)
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    stringResource(R.string.Before_you_proceed), fontSize = 18.sp,
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.Before_you_proceed_desc), fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                NumberedText(
                    modifier = Modifier
                        .fillMaxWidth(), numberStr = "1", instructionStr = stringResource(R.string.Before_you_proceed_instruction_1),
                    color = MixinAppTheme.colors.textMinor
                )
                Spacer(modifier = Modifier.height(12.dp))
                NumberedText(
                    modifier = Modifier
                        .fillMaxWidth(), numberStr = "2", instructionStr = stringResource(R.string.Before_you_proceed_instruction_2),
                    color = MixinAppTheme.colors.red
                )
                Spacer(modifier = Modifier.height(12.dp))
                NumberedText(
                    modifier = Modifier
                        .fillMaxWidth(), numberStr = "3", instructionStr = stringResource(R.string.Before_you_proceed_instruction_3),
                    color = MixinAppTheme.colors.red
                )

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = next,
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
                        text = stringResource(R.string.Show_Mnemonic_Phrase),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}