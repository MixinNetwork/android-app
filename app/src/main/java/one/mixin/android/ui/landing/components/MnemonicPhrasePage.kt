package one.mixin.android.ui.landing.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.landing.MobileViewModel
import one.mixin.android.ui.landing.mnemonic.MnemonicPhraseState

@Composable
fun MnemonicPhrasePage(
    onSuccess: () -> Unit
) {
    val viewModel = hiltViewModel<MobileViewModel>()
    val coroutineScope = rememberCoroutineScope()
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
                    } else {
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
                        "This shouldn’t take long.", //  todo
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        fontSize = 14.sp, color = MixinAppTheme.colors.textMinor
                    )
                }

                MnemonicPhraseState.Failure -> {
                    Text(
                        "Failure", // todo
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        fontSize = 14.sp, color = MixinAppTheme.colors.red
                    )
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
                        coroutineScope.launch {
                            val state = viewModel.mockCreateMnemonicPhrase()
                            if (state == MnemonicPhraseState.Success) {
                                onSuccess.invoke()
                            }
                        }
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

                .constrainAs(number) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top, 2.dp)
                },
            color = MixinAppTheme.colors.textMinor,
            fontSize = 12.sp,
            lineHeight = 12.sp,
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
            color = MixinAppTheme.colors.textMinor,
            fontSize = 14.sp,
            lineHeight = 14.sp
        )
    }
}