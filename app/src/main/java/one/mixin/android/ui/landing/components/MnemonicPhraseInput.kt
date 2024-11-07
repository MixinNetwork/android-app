package one.mixin.android.ui.landing.components

import android.content.ClipData
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.SemiBold
import androidx.compose.ui.text.font.FontWeight.Companion.W500
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.ExportRequest
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.newKeyPairFromMnemonic
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.hexString
import one.mixin.android.extension.toHex
import one.mixin.android.session.Session
import one.mixin.android.session.Session.getEd25519KeyPair
import one.mixin.android.tip.Tip
import one.mixin.android.ui.landing.vo.MnemonicPhrases
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.ui.wallet.alert.AlertViewModel
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode

@Composable
fun MnemonicPhraseInput(
    state: MnemonicState,
    mnemonicList: List<String> = emptyList(),
    onComplete: (List<String>) -> Unit,
    tip: Tip? = null,
    pin: String? = null
) {
    var inputs by remember { mutableStateOf(List(13) { "" }) }
    var loading by remember { mutableStateOf(false) }
    var errorInfo by remember { mutableStateOf("") }
    val context = LocalContext.current
    val walletViewModel = hiltViewModel<WalletViewModel>()
    val coroutineScope = rememberCoroutineScope()
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
                    MnemonicState.Input -> stringResource(R.string.log_in_whit_mnemonic_phrase)
                    MnemonicState.Display -> stringResource(R.string.write_down_mnemonic_phrase)
                    MnemonicState.Verify -> stringResource(R.string.check_mnemonic_phrase)
                }, fontSize = 18.sp,
                color = MixinAppTheme.colors.textPrimary,
                fontWeight = SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (state) {
                    MnemonicState.Input -> stringResource(R.string.enter_mnemonic_phrase)
                    MnemonicState.Display -> stringResource(R.string.write_down_mnemonic_phrase_desc)
                    MnemonicState.Verify -> stringResource(R.string.check_mnemonic_phrase_desc)
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
                                        mnemonicList.getOrNull(index) ?: "",
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
                                    },
                                    singleLine = true,
                                    cursorBrush = SolidColor(MixinAppTheme.colors.accent),
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
                                .clickable {
                                    val clipboard = context.getClipboardManager()
                                    val clipData: ClipData? = clipboard.primaryClip

                                    if (clipData != null && clipData.itemCount > 0) {
                                        val pastedText = clipData.getItemAt(0).text.toString()
                                        val words = pastedText.split(" ")
                                        if (words.size == 13 && words.all { MnemonicPhrases.contains(it) }) {
                                            inputs = words
                                        } else {
                                            errorInfo = context.getString(R.string.Invalid_mnemonic)
                                        }
                                    }
                                }
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
            if (state == MnemonicState.Display) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    modifier = Modifier.align(Alignment.Start),
                    text = stringResource(R.string.mnemonic_phrase_tip_1), fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier.align(Alignment.Start),
                    text = stringResource(R.string.mnemonic_phrase_tip_2), fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                )
            }

            if (errorInfo.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier.align(Alignment.Start),
                    text = errorInfo, fontSize = 14.sp,
                    color = MixinAppTheme.colors.tipError,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = state == MnemonicState.Display || inputs.all { it.isNotEmpty() },
                onClick = {
                    when (state) {
                        MnemonicState.Input -> onComplete.invoke(inputs)
                        MnemonicState.Verify -> {
                            coroutineScope.launch {
                                runCatching {
                                    loading = true
                                    if (mnemonicList != inputs) {
                                        // Todo
                                        errorInfo = context.getString(R.string.Invalid_mnemonic)
                                    } else {
                                        val selfId = Session.getAccountId()!!
                                        val edKey = tip!!.getMnemonicEdKey(context)
                                        val r = walletViewModel.saltExport(
                                            ExportRequest(
                                                publicKey = edKey.publicKey.toHex(),
                                                signature = initFromSeedAndSign(edKey.privateKey, selfId.toByteArray()).toHex(),
                                                pinBase64 = walletViewModel.getEncryptedTipBody(selfId, pin!!),
                                            )
                                        )

                                        if (!r.isSuccess) {
                                            errorInfo = context.getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                                        }
                                    }
                                }.onSuccess {
                                    loading = false
                                    if (errorInfo.isBlank()) onComplete.invoke(inputs)
                                }.onFailure {
                                    errorInfo = it.message ?: ""
                                    loading = false
                                }
                            }
                        }

                        MnemonicState.Display -> {
                            onComplete.invoke(mnemonicList)
                        }
                    }
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
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                    )
                } else {
                    Text(stringResource(R.string.Complete), color = Color.White)
                }
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
