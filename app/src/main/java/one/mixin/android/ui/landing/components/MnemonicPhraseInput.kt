package one.mixin.android.ui.landing.components

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.SemiBold
import androidx.compose.ui.text.font.FontWeight.Companion.W500
import androidx.compose.ui.text.font.FontWeight.Companion.W600
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.checkout.threedsobfuscation.le
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.MixinApp
import one.mixin.android.R
import one.mixin.android.api.response.ExportRequest
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.crypto.getMatchingWords
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.isMnemonicValid
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toHex
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.ui.home.web3.trade.KeyboardAwareBox
import one.mixin.android.ui.landing.components.HighlightedTextWithClick
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.DotTextLayout
import org.bitcoinj.crypto.MnemonicCode

@Composable
fun MnemonicPhraseInput(
    state: MnemonicState,
    mnemonicList: List<String> = emptyList(),
    onComplete: (List<String>) -> Unit,
    tip: Tip? = null,
    pin: String? = null,
    onQrCode: ((List<String>) -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    onScan: (() -> Unit)? = null,
    validate: ((List<String>) -> String?)? = null,
    onCreate: (() -> Unit)? = null,
) {
    var legacy by remember { mutableStateOf(mnemonicList.size > 13) }
    var other by remember { mutableStateOf(false) }
    var inputs by remember {
        mutableStateOf(
            when (state) {
                MnemonicState.Import -> List(if (!legacy) 12 else 24) { "" }
                else -> List(if (!legacy) 13 else 25) { "" }
            }
        )
    }

    if (state == MnemonicState.Import) {
        LaunchedEffect(mnemonicList) {
            legacy =
                if (mnemonicList.size == 12) false else if (mnemonicList.size > 12) true else legacy
            inputs =
                if (mnemonicList.size <= 12) {
                    mnemonicList + List(12 - mnemonicList.size) { "" }
                } else {
                    mnemonicList + List(24 - mnemonicList.size) { "" }
                }
        }
    }

    var loading by remember { mutableStateOf(false) }
    var errorInfo by remember { mutableStateOf("") }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val walletViewModel = hiltViewModel<WalletViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var currentText by remember { mutableStateOf("") }
    var focusIndex by remember { mutableIntStateOf(-1) }
    MixinAppTheme {
        KeyboardAwareBox(
            modifier = Modifier.fillMaxSize(),
            content = { availableHeight ->
                Column(
                    modifier = if (availableHeight != null) {
                        Modifier
                            .fillMaxWidth()
                            .height(availableHeight)
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    title?.invoke()
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp)
                            .verticalScroll(rememberScrollState()),
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
                                MnemonicState.Import -> stringResource(R.string.import_mnemonic_phrase)
                                MnemonicState.Display -> stringResource(R.string.write_down_mnemonic_phrase)
                                MnemonicState.Verify -> stringResource(R.string.check_mnemonic_phrase)
                            },
                            fontSize = 18.sp,
                            color = MixinAppTheme.colors.textPrimary,
                            fontWeight = SemiBold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = when (state) {
                                MnemonicState.Input -> stringResource(R.string.enter_mnemonic_phrase, if (legacy) 25 else 13)
                                MnemonicState.Import -> stringResource(R.string.enter_mnemonic_phrase, if (legacy) 24 else 12)
                                MnemonicState.Display -> stringResource(R.string.write_down_mnemonic_phrase_desc)
                                MnemonicState.Verify -> stringResource(R.string.check_mnemonic_phrase_desc)
                            }, fontSize = 14.sp,
                            color = MixinAppTheme.colors.textAssist,
                            textAlign = TextAlign.Center
                        )
                        if (state == MnemonicState.Input || state == MnemonicState.Import) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.Start, modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                WordCountButton(
                                    onClick = {
                                        legacy = false
                                        other = false
                                        inputs = List(if (state == MnemonicState.Input) 13 else 12) { "" }
                                    },
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (!legacy && !other) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGray,
                                    ),
                                ) {
                                    Text(stringResource(R.string.words, if (state == MnemonicState.Input) 13 else 12), color = if (!legacy && !other) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                WordCountButton(
                                    onClick = {
                                        legacy = true
                                        other = false
                                        inputs = List(if (state == MnemonicState.Input) 25 else 24) { "" }
                                    },
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (legacy && !other) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGray
                                    ),
                                ) {
                                    Text(stringResource(R.string.words, if (state == MnemonicState.Input) 25 else 24), color = if (legacy && !other) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist)
                                }
                                if (state == MnemonicState.Input) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    WordCountButton(
                                        onClick = {
                                            other = true
                                        },
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (other) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGray
                                        ),
                                    ) {
                                        Text(stringResource(R.string.other_words), color = if (other) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Spacer(modifier = Modifier.height(44.dp))
                        }
                        if (other) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MixinAppTheme.colors.backgroundWindow)
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.mnemonic_12_24_instruction_title),
                                        fontSize = 14.sp,
                                        color = MixinAppTheme.colors.textPrimary,
                                        fontWeight = W600
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    BulletPointText(
                                        text = stringResource(R.string.mnemonic_12_24_step_1),
                                        textColor = MixinAppTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    BulletPointText(
                                        text = stringResource(R.string.mnemonic_12_24_step_2),
                                        textColor = MixinAppTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    BulletPointText(
                                        text = stringResource(R.string.mnemonic_12_24_step_3),
                                        textColor = MixinAppTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    BulletPointText(
                                        text = stringResource(R.string.mnemonic_12_24_step_4),
                                        textColor = MixinAppTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HighlightedTextWithClick(
                                        stringResource(
                                            R.string.mnemonic_12_24_privacy_info_with_link,
                                            stringResource(R.string.Learn_More)
                                        ),
                                        modifier = Modifier,
                                        stringResource(R.string.Learn_More),
                                        textAlign = TextAlign.Start,
                                        color = MixinAppTheme.colors.textPrimary
                                    ) {
                                        context.openUrl(context.getString(R.string.url_privacy_wallet))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                onClick = {
                                    onCreate?.invoke()
                                },
                                colors =
                                    ButtonDefaults.outlinedButtonColors(MixinAppTheme.colors.accent),
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
                                    stringResource(
                                        R.string.Create_Account
                                    ),
                                    color = Color.White,
                                    fontWeight = W500,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.register_in_15_seconds),
                                fontSize = 12.sp,
                                color = MixinAppTheme.colors.textAssist,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        } else {
                            InputGrid(if (legacy) 27 else 15, 10.dp) { index ->
                                if (state == MnemonicState.Display && ((mnemonicList.size == 12 && index == 12) || (mnemonicList.size == 24 && index == 24))) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                onQrCode?.invoke(mnemonicList)
                                            }
                                            .padding(8.dp)) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_mnemonic_qrcode),
                                            contentDescription = null,
                                            tint = MixinAppTheme.colors.textPrimary,
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            stringResource(R.string.QR_Code), fontSize = 12.sp,
                                            fontWeight = W500,
                                            color = MixinAppTheme.colors.textPrimary,
                                        )
                                    }
                                } else if (state == MnemonicState.Display && ((mnemonicList.size == 12 && index == 13) || (mnemonicList.size == 24 && index == 25))) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                val clipboard = context.getClipboardManager()
                                                clipboard.setPrimaryClip(
                                                    ClipData.newPlainText(
                                                        null, mnemonicList.joinToString(" ")
                                                    )
                                                )
                                                toast(R.string.copied_to_clipboard)
                                            }
                                            .padding(8.dp)) {

                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_copy_gray),
                                            contentDescription = null,
                                            tint = MixinAppTheme.colors.textPrimary,
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            stringResource(R.string.Copy), fontSize = 12.sp,
                                            fontWeight = W500,
                                            color = MixinAppTheme.colors.textPrimary,
                                        )

                                    }
                                } else if (state == MnemonicState.Display && ((mnemonicList.size == 12 && index == 14) || (mnemonicList.size == 24 && index == 26))) {
                                    // placeholder
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {}
                                } else if (index < inputs.size) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
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
                                            val text = inputs[index]
                                            val textColor = if (index != focusIndex && text.isNotEmpty() && MnemonicCode.INSTANCE.wordList.contains(text).not()) {
                                                MixinAppTheme.colors.tipError
                                            } else {
                                                MixinAppTheme.colors.textMinor
                                            }
                                            BasicTextField(
                                                modifier = Modifier.onFocusChanged { focusState ->
                                                    focusIndex = if (focusState.isFocused) {
                                                        currentText = inputs[index]
                                                        index
                                                    } else {
                                                        -1
                                                    }
                                                },
                                                value = inputs[index],
                                                onValueChange = { newText ->
                                                    inputs = inputs.toMutableList().also { it[index] = newText }
                                                    currentText = newText
                                                },
                                                singleLine = true,
                                                cursorBrush = SolidColor(MixinAppTheme.colors.accent),
                                                textStyle = LocalTextStyle.current.copy(
                                                    color = textColor,
                                                    fontSize = 13.sp,
                                                    fontWeight = W500
                                                ),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Text,
                                                    imeAction = if (inputs.all { it.isNotEmpty() }) ImeAction.Done else ImeAction.Next
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onNext = {
                                                        if (!legacy && index == 12) {
                                                            repeat(4) {
                                                                focusManager.moveFocus(FocusDirection.Up)
                                                            }
                                                        } else if (index == 24) {
                                                            repeat(8) {
                                                                focusManager.moveFocus(FocusDirection.Up)
                                                            }
                                                        } else if ((index + 1) % 3 == 0) {
                                                            focusManager.moveFocus(FocusDirection.Down)
                                                            focusManager.moveFocus(FocusDirection.Left)
                                                            focusManager.moveFocus(FocusDirection.Left)
                                                        } else {
                                                            focusManager.moveFocus(FocusDirection.Right)
                                                        }
                                                    },
                                                    onDone = {
                                                        val words = inputs.map { it.trim() }
                                                        validate?.invoke(words)?.let {
                                                            errorInfo = it
                                                            return@KeyboardActions
                                                        }
                                                        when (state) {
                                                            MnemonicState.Input -> onComplete.invoke(words)
                                                            MnemonicState.Import -> {
                                                                val valid = (!legacy && words.size == 12 && isMnemonicValid(words)) ||
                                                                        (legacy && words.size == 24 && isMnemonicValid(words))
                                                                if (!valid) {
                                                                    errorInfo =
                                                                        context.getString(R.string.invalid_mnemonic_phrase)
                                                                } else {
                                                                    onComplete.invoke(words)
                                                                }
                                                            }

                                                            MnemonicState.Verify -> {
                                                                coroutineScope.launch {
                                                                    runCatching {
                                                                        loading = true
                                                                        val valid = when (state) {
                                                                            MnemonicState.Import ->
                                                                                (!legacy && words.size == 24 && isMnemonicValid(words)) ||
                                                                                        (legacy && words.size == 12 && isMnemonicValid(words))

                                                                            MnemonicState.Verify -> mnemonicList == words
                                                                            else -> false
                                                                        }
                                                                        if (!valid) {
                                                                            errorInfo = context.getString(R.string.invalid_mnemonic_phrase)
                                                                        } else if (state == MnemonicState.Verify) {
                                                                            val selfId = Session.getAccountId()!!
                                                                            val seed = tip?.getOrRecoverTipPriv(context, pin!!)?.getOrThrow()
                                                                            val edKey = tip?.getMnemonicEdKey(context, pin!!, seed!!)
                                                                            val r = walletViewModel.saltExport(
                                                                                ExportRequest(
                                                                                    publicKey = edKey!!.publicKey.toHex(),
                                                                                    signature = initFromSeedAndSign(edKey.privateKey, selfId.toByteArray()).toHex(),
                                                                                    pinBase64 = walletViewModel.getEncryptedTipBody(selfId, pin!!),
                                                                                )
                                                                            )
                                                                            r.data?.let {
                                                                                Session.storeAccount(it)
                                                                            }

                                                                            errorInfo = if (!r.isSuccess) {
                                                                                context.getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                                                                            } else {
                                                                                ""
                                                                            }
                                                                        }
                                                                    }.onSuccess {
                                                                        loading = false
                                                                        if (errorInfo.isBlank()) onComplete.invoke(words)
                                                                    }.onFailure {
                                                                        errorInfo = it.message ?: ""
                                                                        loading = false
                                                                    }
                                                                }
                                                            }

                                                            MnemonicState.Display -> {
                                                                // do nothing
                                                            }
                                                        }
                                                    }
                                                ),
                                                decorationBox = { innerTextField ->
                                                    Row {
                                                        Text(
                                                            "${index + 1}",
                                                            color = textColor,
                                                            fontSize = 13.sp,
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        innerTextField()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } else if (state == MnemonicState.Import && index == if (legacy) 24 else 12) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onScan?.invoke() }
                                            .padding(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_scan),
                                            contentDescription = null,
                                            tint = MixinAppTheme.colors.textPrimary,
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.Scan), fontSize = 12.sp,
                                            fontWeight = W500,
                                            color = MixinAppTheme.colors.textPrimary,
                                        )
                                    }
                                } else if (index == if (legacy) 25 else 13) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .alpha(if (state == MnemonicState.Input || state == MnemonicState.Display || state == MnemonicState.Import) 1f else 0f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                if (state == MnemonicState.Input || state == MnemonicState.Import) {
                                                    val clipboard = context.getClipboardManager()
                                                    val clipData: ClipData? = clipboard.primaryClip
                                                    if (clipData != null && clipData.itemCount > 0) {
                                                        val pastedText = clipData.getItemAt(0).text.toString()
                                                        val words = pastedText.split(" ")
                                                        when {
                                                            words.size == (if (state == MnemonicState.Import) 24 else 25) && isMnemonicValid(
                                                                if (state == MnemonicState.Import) words else words.subList(0, 24)
                                                            ) -> {
                                                                legacy = true
                                                                inputs = words
                                                            }

                                                            words.size == (if (state == MnemonicState.Import) 12 else 13) && isMnemonicValid(
                                                                if (state == MnemonicState.Import) words else words.subList(0, 12)
                                                            ) -> {
                                                                legacy = false
                                                                inputs = words
                                                            }

                                                            else -> {
                                                                errorInfo =
                                                                    context.getString(R.string.invalid_mnemonic_phrase)
                                                            }
                                                        }
                                                    }
                                                } else if (state == MnemonicState.Display) {
                                                    onQrCode?.invoke(mnemonicList)
                                                }
                                            }
                                            .padding(8.dp)
                                    ) {
                                        if (state == MnemonicState.Input || state == MnemonicState.Import) {
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
                                        } else {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_mnemonic_qrcode),
                                                contentDescription = null,
                                                tint = MixinAppTheme.colors.textPrimary,
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                stringResource(R.string.QR_Code), fontSize = 12.sp,
                                                fontWeight = W500,
                                                color = MixinAppTheme.colors.textPrimary,
                                            )
                                        }
                                    }
                                } else if (index == if (legacy) 26 else 14) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                if (state == MnemonicState.Input || state == MnemonicState.Verify || state == MnemonicState.Import) {
                                                    inputs = inputs.map { "" }
                                                } else if (state == MnemonicState.Display) {
                                                    val clipboard = context.getClipboardManager()
                                                    clipboard.setPrimaryClip(
                                                        ClipData.newPlainText(
                                                            null, mnemonicList.joinToString(" ")
                                                        )
                                                    )
                                                    toast(R.string.copied_to_clipboard)
                                                }
                                            }
                                            .padding(8.dp)
                                    ) {
                                        if (state == MnemonicState.Input || state == MnemonicState.Verify || state == MnemonicState.Import) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_action_delete),
                                                contentDescription = null,
                                                tint = MixinAppTheme.colors.textPrimary,
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                stringResource(R.string.Clear), fontSize = 12.sp,
                                                fontWeight = W500,
                                                color = MixinAppTheme.colors.textPrimary,
                                            )
                                        } else if (state == MnemonicState.Display) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_copy_gray),
                                                contentDescription = null,
                                                tint = MixinAppTheme.colors.textPrimary,
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                stringResource(R.string.Copy), fontSize = 12.sp,
                                                fontWeight = W500,
                                                color = MixinAppTheme.colors.textPrimary,
                                            )
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
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (errorInfo.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    modifier = Modifier.align(Alignment.Start),
                                    text = errorInfo, fontSize = 14.sp,
                                    color = MixinAppTheme.colors.tipError,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            } else if (inputs.any { it.isNotEmpty() && MnemonicCode.INSTANCE.wordList.contains(it).not() }) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    modifier = Modifier.align(Alignment.Start),
                                    text = context.getString(R.string.invalid_mnemonic_phrase), fontSize = 14.sp,
                                    color = MixinAppTheme.colors.tipError,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Spacer(modifier = Modifier.height(if (legacy) 20.dp else 8.dp))
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                enabled = state == MnemonicState.Display || (inputs.all { it.isNotEmpty() && MnemonicCode.INSTANCE.wordList.contains(it) }),
                                onClick = {
                                    val words = inputs.map { it.trim() }
                                    validate?.invoke(words)?.let {
                                        errorInfo = it
                                        return@Button
                                    }
                                    when (state) {
                                        MnemonicState.Input -> onComplete.invoke(words)
                                        MnemonicState.Import -> {
                                            val valid = (!legacy && words.size == 12 && isMnemonicValid(words)) ||
                                                    (legacy && words.size == 24 && isMnemonicValid(words))
                                            if (!valid) {
                                                errorInfo =
                                                    context.getString(R.string.invalid_mnemonic_phrase)
                                            } else {
                                                onComplete.invoke(words)
                                            }
                                        }

                                        MnemonicState.Verify -> {
                                            coroutineScope.launch {
                                                runCatching {
                                                    loading = true
                                                    if (mnemonicList != words) {
                                                        errorInfo = context.getString(R.string.invalid_mnemonic_phrase)
                                                    } else {
                                                        val selfId = Session.getAccountId()!!
                                                        val seed = tip?.getOrRecoverTipPriv(context, pin!!)?.getOrThrow()
                                                        val edKey = tip?.getMnemonicEdKey(context, pin!!, seed!!)
                                                        val r = walletViewModel.saltExport(
                                                            ExportRequest(
                                                                publicKey = edKey!!.publicKey.toHex(),
                                                                signature = initFromSeedAndSign(edKey.privateKey, selfId.toByteArray()).toHex(),
                                                                pinBase64 = walletViewModel.getEncryptedTipBody(selfId, pin!!),
                                                            )
                                                        )
                                                        r.data?.let {
                                                            Session.storeAccount(it)
                                                        }
                                                        errorInfo = if (!r.isSuccess) {
                                                            context.getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                                                        } else {
                                                            ""
                                                        }
                                                    }
                                                }.onSuccess {
                                                    loading = false
                                                    if (errorInfo.isBlank()) onComplete.invoke(words)
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
                                        backgroundColor = if (state == MnemonicState.Display || (inputs.all { it.isNotEmpty() && MnemonicCode.INSTANCE.wordList.contains(it) })) {
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
                                    Text(
                                        stringResource(
                                            when {
                                                state == MnemonicState.Import -> R.string.Next
                                                state == MnemonicState.Display && (mnemonicList.size == 12 || mnemonicList.size == 24) -> R.string.Done
                                                state == MnemonicState.Display -> R.string.Check_Backup
                                                state == MnemonicState.Input -> R.string.Confirm
                                                state == MnemonicState.Verify -> R.string.Complete
                                                else -> R.string.Next
                                            }
                                        ),
                                        color = Color.White,
                                        fontWeight = W500,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }, floating = {
                if (!other && (state == MnemonicState.Input || state == MnemonicState.Verify || state == MnemonicState.Import)) {
                    InputBar(currentText) { word ->
                        val maxInput = when (state) {
                            MnemonicState.Import -> if (!legacy) 24 else 12
                            else -> inputs.size
                        }
                        if (focusIndex in 0 until maxInput) {
                            inputs = inputs.toMutableList().also { it[focusIndex] = word }
                        }
                        if (state == MnemonicState.Import) {
                            if (legacy && focusIndex == 11) {
                                keyboardController?.hide()
                                focusManager.moveFocus(FocusDirection.Right)
                            } else if (!legacy && focusIndex == 23) {
                                keyboardController?.hide()
                            } else if ((focusIndex + 1) % 3 == 0) {
                                focusManager.moveFocus(FocusDirection.Down)
                                focusManager.moveFocus(FocusDirection.Left)
                                focusManager.moveFocus(FocusDirection.Left)
                            } else {
                                focusManager.moveFocus(FocusDirection.Right)
                            }
                        } else {
                            if (!legacy && focusIndex == 12) {
                                keyboardController?.hide()
                                focusManager.moveFocus(FocusDirection.Right)
                            } else if (focusIndex == 24) {
                                keyboardController?.hide()
                            } else if ((focusIndex + 1) % 3 == 0) {
                                focusManager.moveFocus(FocusDirection.Down)
                                focusManager.moveFocus(FocusDirection.Left)
                                focusManager.moveFocus(FocusDirection.Left)
                            } else {
                                focusManager.moveFocus(FocusDirection.Right)
                            }
                        }
                    }
                }
            })
    }
}

@Composable
fun InputGrid(
    size: Int,
    spacing: Dp,
    content: @Composable (index: Int) -> Unit,
) {
    Column {
        for (rowIndex in 0 until (size + 2) / 3) {
            if (rowIndex > 0) {
                Spacer(modifier = Modifier.height(spacing))
            }
            Row {
                for (columnIndex in 0 until 3) {
                    val index = rowIndex * 3 + columnIndex
                    if (columnIndex > 0) {
                        Spacer(modifier = Modifier.width(spacing))
                    }
                    if (index < size) {
                        Box(modifier = Modifier.weight(1f)) {
                            content(index)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

enum class MnemonicState {
    Input,
    Display,
    Verify,
    Import
}

@Composable
fun WordCountButton(onClick: () -> Unit, border: BorderStroke, content: @Composable RowScope.() -> Unit) {
    Button(
        modifier = Modifier
            .height(36.dp),
        onClick = onClick,
        colors =
            ButtonDefaults.outlinedButtonColors(
                Color.Transparent
            ),
        shape = RoundedCornerShape(32.dp),
        border = border,
        contentPadding = PaddingValues(horizontal = 8.dp),
        elevation =
            ButtonDefaults.elevation(
                pressedElevation = 0.dp,
                defaultElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp,
            ),
        content = content
    )
}

@Composable
fun InputBar(string: String, callback: (String) -> Unit) {
    if (string.isBlank()) return
    val list = getMatchingWords(string.trim()) ?: return
    if (list.isEmpty()) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MixinAppTheme.colors.backgroundWindow)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(list.size) { index ->
            val word = list[index]
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MixinAppTheme.colors.background)
                    .clickable { callback(word) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                text = word,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showSystemUi = true)
@Composable
fun MnemonicPhraseInputPreview() {
    MnemonicPhraseInput(
        state = MnemonicState.Input,
        onScan = {},
        onComplete = { mnemonicList -> /* Handle mnemonic change */ },
    )
}

@Composable
fun BulletPointText(
    text: String,
    bulletColor: Color = MixinAppTheme.colors.textPrimary,
    textColor: Color = MixinAppTheme.colors.textPrimary,
    fontSize: TextUnit = 14.sp,
    bulletSize: Dp = 4.dp,
    spacing: Dp = 8.dp
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .width(bulletSize + spacing)
                .padding(top = (fontSize.value * 0.65).dp),
            contentAlignment = Alignment.TopStart
        ) {
            Box(
                modifier = Modifier
                    .size(bulletSize)
                    .background(
                        color = bulletColor,
                        shape = CircleShape
                    )
            )
        }
        
        Text(
            text = text,
            fontSize = fontSize,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}