package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.session.Session
import one.mixin.android.ui.landing.components.NumberedText

@Composable
fun AddRecoveryContactBeforePage(back: () -> Unit, next: () -> Unit) {
    MixinAppTheme {
        Column {
            MixinTopAppBar(
                title = {

                },
                actions = {

                },
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(14.dp))
                Icon(modifier = Modifier.align(Alignment.CenterHorizontally), painter = painterResource(R.drawable.ic_emergency), tint = Color.Unspecified, contentDescription = null)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    stringResource(
                        R.string.Recovery_Contact
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontSize = 18.sp, fontWeight = FontWeight.W600, color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(30.dp))
                NumberedText(
                    modifier = Modifier
                        .fillMaxWidth(), numberStr = "1", instructionStr = stringResource(R.string.add_recovery_contact_before_instruction_1)
                )
                Spacer(modifier = Modifier.height(16.dp))
                NumberedText(
                    modifier = Modifier
                        .fillMaxWidth(), numberStr = "2", instructionStr = stringResource(R.string.add_recovery_contact_before_instruction_2)
                )
                Spacer(modifier = Modifier.height(16.dp))
                NumberedText(
                    modifier = Modifier
                        .fillMaxWidth(), numberStr = "3", instructionStr = stringResource(R.string.add_recovery_contact_before_instruction_3, Session.getAccount()!!.identityNumber),
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
                        text = stringResource(R.string.Im_Ready),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally).clickable {
                        back.invoke()
                    },
                    text = stringResource(R.string.Later),
                    color = MixinAppTheme.colors.textBlue
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}