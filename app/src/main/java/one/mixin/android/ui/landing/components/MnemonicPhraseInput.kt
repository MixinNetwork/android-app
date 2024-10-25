package one.mixin.android.ui.landing.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.SemiBold
import androidx.compose.ui.text.font.FontWeight.Companion.W500
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun MnemonicPhraseInput(
    state: MnemonicState,
    mnemonicList: List<String> = emptyList(),
    onComplete: (List<String>) -> Unit,
) {
    var inputs by remember { mutableStateOf(List(13) { "" }) }
    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_mnemonic),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = when (state) {
                    MnemonicState.Input -> "Log in with Mnemonic Phrase"
                    MnemonicState.Display -> "Write down your Mnemonic Phrase"
                    MnemonicState.Verify -> "Check your Mnemonic Phrase"
                }, fontSize = 18.sp,
                color = MixinAppTheme.colors.textPrimary,
                fontWeight = SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (state) {
                    MnemonicState.Input -> "Enter or paste your 13 word phrase."
                    MnemonicState.Display -> "Write it down on a piece of paper and keep it in a safe place."
                    MnemonicState.Verify -> "Select each word in the order it was presented to you."
                }, fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(44.dp))

            LazyVerticalGrid(
                modifier = Modifier.fillMaxWidth(),
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(15) { index ->
                    if (index < 13) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MixinAppTheme.colors.backgroundWindow)
                                .padding(8.dp)
                        ) {
                            if (state == MnemonicState.Display) {
                                Row {
                                    Text(
                                        "${index + 1}",
                                        color = MixinAppTheme.colors.textMinor,
                                        fontSize = 13.sp,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        mnemonicList.getOrNull(index)?:"",
                                        color = MixinAppTheme.colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = W500
                                    )
                                }
                            } else {
                                BasicTextField(
                                    value = inputs[index],
                                    onValueChange = { newText ->
                                        inputs = inputs.toMutableList().also { it[index] = newText }

                                        if (inputs.all { it.isNotEmpty() }) {
                                            // todo check
                                            // onComplete(inputs)
                                        }
                                    },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        color = MixinAppTheme.colors.textMinor,
                                        fontSize = 13.sp,
                                        fontWeight = W500
                                    ),
                                    decorationBox = { innerTextField ->
                                        Row {
                                            Text(
                                                "${index + 1}",
                                                color = MixinAppTheme.colors.textPrimary,
                                                fontSize = 13.sp,
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        }
                    } else if (index == 13) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            if (state == MnemonicState.Input) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_paste),
                                    contentDescription = null,
                                    tint = MixinAppTheme.colors.textPrimary,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.Paste), fontSize = 12.sp,
                                    fontWeight = W500,
                                    color = MixinAppTheme.colors.textPrimary,
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    inputs = inputs.map { "" }
                                }
                                .padding(8.dp)
                        ) {
                            if (state == MnemonicState.Input || state == MnemonicState.Verify) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_action_delete),
                                    contentDescription = null,
                                    tint = MixinAppTheme.colors.textPrimary,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.Delete), fontSize = 12.sp,
                                    fontWeight = W500,
                                    color = MixinAppTheme.colors.textPrimary,
                                )
                            }
                        }
                    }
                }
            }
            if (state == MnemonicState.Display){
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    modifier = Modifier.align(Alignment.Start),
                    text = "· Store in vault or safe place.", fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier.align(Alignment.Start),
                    text = "· Don’t share it with a network.", fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = state == MnemonicState.Display || inputs.all { it.isNotEmpty() },
                onClick = {
                    onComplete.invoke(if (state == MnemonicState.Display) mnemonicList else inputs)
                },
                colors =
                ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (state == MnemonicState.Display || inputs.all { it.isNotEmpty() }) {
                        MixinAppTheme.colors.accent
                    } else {
                        MixinAppTheme.colors.backgroundGray
                    }
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
                Text(stringResource(R.string.Complete), color = Color.White)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

enum class MnemonicState {
    Input,
    Display,
    Verify
}

@Preview(backgroundColor = 0xFFFFFFFF, showSystemUi = true)
@Composable
fun MnemonicPhraseInputPreview() {
    MnemonicPhraseInput(
        state = MnemonicState.Input,
        onComplete = { mnemonicList -> /* Handle mnemonic change */ },
    )
}
