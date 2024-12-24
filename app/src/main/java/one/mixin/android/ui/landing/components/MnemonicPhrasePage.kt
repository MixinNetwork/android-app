package one.mixin.android.ui.landing.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.landing.vo.MnemonicPhraseState
import one.mixin.android.ui.landing.viewmodel.LandingViewModel

@Composable
fun MnemonicPhrasePage(
    isSign: Boolean,
    errorInfo: String?,
    requestCaptcha: () -> Unit
) {
    val viewModel = hiltViewModel<LandingViewModel>()
    val mnemonicPhraseState by viewModel.mnemonicPhraseState.observeAsState(MnemonicPhraseState.Initial)
    MixinAppTheme {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(120.dp))
            Icon(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                painter = painterResource(
                    if (mnemonicPhraseState == MnemonicPhraseState.Initial) {
                        R.drawable.ic_mnemonic_phrase
                    } else {
                        R.drawable.ic_mnemonic_phrase_creaeting
                    }
                ),
                contentDescription = null,
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(
                    if (mnemonicPhraseState == MnemonicPhraseState.Initial) {
                        R.string.Create_Mnemonic_Phrase
                    } else if (isSign){
                        R.string.Signing_in_to_your_account
                    }else{
                        R.string.Creating_your_account
                    }
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontSize = 18.sp, fontWeight = FontWeight.W600, color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            when (mnemonicPhraseState) {
                MnemonicPhraseState.Initial -> {
                    NumberedText(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(), numberStr = "1", instructionStr = stringResource(R.string.mnemonic_phrase_instruction_1)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NumberedText(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(), numberStr = "2", instructionStr = stringResource(R.string.mnemonic_phrase_instruction_2)
                    )
                }

                MnemonicPhraseState.Creating -> {
                    Text(
                        stringResource(R.string.mnemonic_phrase_take_long),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        fontSize = 14.sp, color = MixinAppTheme.colors.textMinor
                    )
                }

                MnemonicPhraseState.Failure -> {
                    if (!errorInfo.isNullOrBlank()) {
                        Text(
                            errorInfo,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            fontSize = 14.sp, color = MixinAppTheme.colors.red
                        )
                    }
                }

                MnemonicPhraseState.Success -> {
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            if (mnemonicPhraseState == MnemonicPhraseState.Creating) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                        .wrapContentSize(Alignment.Center)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = Color(0xFFE8E5EE),
                    )
                }
            } else if (mnemonicPhraseState == MnemonicPhraseState.Failure || mnemonicPhraseState == MnemonicPhraseState.Initial) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        requestCaptcha.invoke()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = MixinAppTheme.colors.accent
                    ),
                    shape = RoundedCornerShape(32.dp),
                    elevation = ButtonDefaults.elevation(
                        pressedElevation = 0.dp,
                        defaultElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp,
                    ),
                ) {
                    Text(
                        stringResource(
                            if (mnemonicPhraseState == MnemonicPhraseState.Initial) {
                                R.string.Create
                            } else {
                                R.string.Retry
                            }
                        ),
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

