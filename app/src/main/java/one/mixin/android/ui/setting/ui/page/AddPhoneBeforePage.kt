package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.landing.components.HighlightedTextWithClick
import one.mixin.android.ui.landing.components.NumberedText

@Composable
fun AddPhoneBeforePage(hasPhone: Boolean, next: () -> Unit) {
    val context = LocalContext.current
    MixinAppTheme {
        Column {
            MixinTopAppBar(
                title = {
                },
                actions = {
                    IconButton(onClick = {
                        context.openUrl(Constants.HelpLink.TIP)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_support),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.icon,
                        )
                    }
                },
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(50.dp))
                Icon(modifier = Modifier.align(Alignment.CenterHorizontally), painter = painterResource(R.drawable.ic_moblie_number), tint = Color.Unspecified, contentDescription = null)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    stringResource(
                        if (hasPhone) {
                            R.string.Change_Mobile_Number
                        } else {
                            R.string.Add_Mobile_Number
                        }
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontSize = 18.sp, fontWeight = FontWeight.W600, color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                HighlightedTextWithClick(
                    stringResource(R.string.Add_Phone_desc, stringResource(R.string.Set_up_Pin_more)),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    stringResource(R.string.Set_up_Pin_more),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                ) {
                    context.openUrl(Constants.HelpLink.TIP)
                }
                Spacer(modifier = Modifier.height(16.dp))
                NumberedText(
                    modifier = Modifier
                        .fillMaxWidth(), numberStr = "1", instructionStr = stringResource(R.string.add_phone_instruction_1)
                )
                Spacer(modifier = Modifier.height(16.dp))
                NumberedText(
                    modifier = Modifier
                        .fillMaxWidth(), numberStr = "2", instructionStr = stringResource(R.string.add_phone_instruction_2)
                )
                Spacer(modifier = Modifier.height(16.dp))
                NumberedText(
                    modifier = Modifier
                        .fillMaxWidth(), numberStr = "3", instructionStr = stringResource(R.string.add_phone_instruction_3)
                )
                Spacer(modifier = Modifier.height(16.dp))
                NumberedText(
                    modifier = Modifier
                        .fillMaxWidth(), numberStr = "4", instructionStr = stringResource(R.string.add_phone_instruction_4),
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
                        text = stringResource(R.string.Start),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}