package one.mixin.android.ui.landing.components

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.ExportRequest
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.crypto.getMatchingWords
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.isMnemonicValid
import one.mixin.android.crypto.mnemonicChecksum
import one.mixin.android.extension.copySensitiveTextToClipboard
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.toHex
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.ui.home.web3.trade.KeyboardAwareBox
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.Account
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
    inputWordCounts: Pair<Int, Int> = 13 to 25,
    compactInput: Boolean = false,
    validate: ((List<String>) -> String?)? = null,
    saltExport: (suspend (ExportRequest) -> MixinResponse<Account>)? = null,
    getEncryptedTipBody: (suspend (String, String) -> String)? = null,
) {
    val shortWordCount = if (state == MnemonicState.Import) 12 else inputWordCounts.first
    val legacyWordCount = if (state == MnemonicState.Import) 24 else inputWordCounts.second
    val showImportSafety = state == MnemonicState.Input && compactInput && shortWordCount == 12 && legacyWordCount == 24
    fun wordCountForLegacy(useLegacy: Boolean) = if (useLegacy) legacyWordCount else shortWordCount
    var legacy by remember(shortWordCount, legacyWordCount) { mutableStateOf(mnemonicList.size > shortWordCount) }
    var inputs by remember {
        mutableStateOf(
            List(wordCountForLegacy(legacy)) { "" }
        )
    }

    if (state == MnemonicState.Input || state == MnemonicState.Import) {
        LaunchedEffect(mnemonicList, shortWordCount, legacyWordCount) {
            if (mnemonicList.isEmpty()) return@LaunchedEffect
            val useLegacy = mnemonicList.size > shortWordCount
            legacy =
                if (mnemonicList.size == shortWordCount) false else if (useLegacy) true else legacy
            val targetCount = wordCountForLegacy(useLegacy)
            inputs = mnemonicList.take(targetCount) + List((targetCount - mnemonicList.size).coerceAtLeast(0)) { "" }
        }
    }

    var loading by remember { mutableStateOf(false) }
    var errorInfo by remember { mutableStateOf("") }
    val context = LocalContext.current
    val invalidMnemonicPhrase = stringResource(R.string.invalid_mnemonic_phrase)
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var currentText by remember { mutableStateOf("") }
    var focusIndex by remember { mutableIntStateOf(-1) }
    fun hideInputMethod() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    fun isValidMnemonicForCurrentMode(words: List<String>): Boolean {
        if (words.any { MnemonicCode.INSTANCE.wordList.contains(it).not() }) return false
        fun isValidBip39(value: List<String>) = runCatching { isMnemonicValid(value) }.getOrDefault(false)
        return when (state) {
            MnemonicState.Input -> {
                if (words.size != shortWordCount && words.size != legacyWordCount) return false
                when (words.size) {
                    12, 24 -> isValidBip39(words)
                    13 -> mnemonicChecksum(words) && isValidBip39(words.subList(0, 12))
                    25 -> mnemonicChecksum(words) && isValidBip39(words.subList(0, 24))
                    else -> false
                }
            }
            MnemonicState.Import -> {
                (words.size == 12 || words.size == 24) && isValidBip39(words)
            }
            else -> true
        }
    }
    val suggestedWords = if (
        focusIndex >= 0 &&
        currentText.isNotBlank() &&
        (state == MnemonicState.Input || state == MnemonicState.Verify || state == MnemonicState.Import)
    ) {
        getMatchingWords(currentText.trim()).orEmpty()
    } else {
        emptyList()
    }
    MixinAppTheme {
        KeyboardAwareBox(
            modifier = Modifier.fillMaxSize(),
            showFloating = suggestedWords.isNotEmpty(),
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
                        if (state != MnemonicState.Input || !compactInput) {
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
                                    MnemonicState.Input -> stringResource(R.string.enter_mnemonic_phrase, wordCountForLegacy(legacy))
                                    MnemonicState.Import -> stringResource(R.string.enter_mnemonic_phrase, wordCountForLegacy(legacy))
                                    MnemonicState.Display -> stringResource(R.string.write_down_mnemonic_phrase_desc)
                                    MnemonicState.Verify -> stringResource(R.string.check_mnemonic_phrase_desc)
                                }, fontSize = 14.sp,
                                color = MixinAppTheme.colors.textAssist,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (state == MnemonicState.Input || state == MnemonicState.Import) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.Start, modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                WordCountButton(
                                    onClick = {
                                        legacy = false
                                        inputs = List(shortWordCount) { "" }
                                    },
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (!legacy) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGray,
                                    ),
                                ) {
                                    Text(stringResource(R.string.words, shortWordCount), color = if (!legacy) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                WordCountButton(
                                    onClick = {
                                        legacy = true
                                        inputs = List(legacyWordCount) { "" }
                                    },
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (legacy) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGray
                                    ),
                                ) {
                                    Text(stringResource(R.string.words, legacyWordCount), color = if (legacy) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Spacer(modifier = Modifier.height(44.dp))
                        }
                        val inputCount = inputs.size
                        val gridSize = if (state == MnemonicState.Display) {
                            if (mnemonicList.size > 12) 27 else 15
                        } else {
                            inputCount + 3
                        }
                        InputGrid(gridSize, 10.dp) { index ->
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
                                            val content = mnemonicList.joinToString(" ")
                                            context.copySensitiveTextToClipboard(content, coroutineScope)
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
                                val inputShape = RoundedCornerShape(4.dp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(inputShape)
                                        .background(MixinAppTheme.colors.background)
                                        .border(1.dp, MixinAppTheme.colors.borderColor, inputShape)
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
                                                    if (index == inputCount - 1) {
                                                        keyboardController?.hide()
                                                        focusManager.moveFocus(FocusDirection.Right)
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
                                                        MnemonicState.Input -> {
                                                            hideInputMethod()
                                                            onComplete.invoke(words)
                                                        }
                                                        MnemonicState.Import -> {
                                                            val valid = (!legacy && words.size == 12 && isMnemonicValid(words)) ||
                                                                    (legacy && words.size == 24 && isMnemonicValid(words))
                                                            if (!valid) {
                                                                errorInfo = invalidMnemonicPhrase
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
                                                                        errorInfo = invalidMnemonicPhrase
                                                                    } else if (state == MnemonicState.Verify) {
                                                                        val selfId = Session.getAccountId()!!
                                                                        val seed = tip?.getOrRecoverTipPriv(context, pin!!)?.getOrThrow()
                                                                        val edKey = tip?.getMnemonicEdKey(context, pin!!, seed!!)
                                                                        val export = requireNotNull(saltExport) { "saltExport is required for MnemonicState.Verify" }
                                                                        val encryptedTipBody = requireNotNull(getEncryptedTipBody) { "getEncryptedTipBody is required for MnemonicState.Verify" }
                                                                        val r = export(
                                                                            ExportRequest(
                                                                                publicKey = edKey!!.publicKey.toHex(),
                                                                                signature = initFromSeedAndSign(edKey.privateKey, selfId.toByteArray()).toHex(),
                                                                                pinBase64 = encryptedTipBody(selfId, pin!!),
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
                                                                    errorInfo = ErrorHandler.getErrorMessage(it)
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
                            } else if ((state == MnemonicState.Input || state == MnemonicState.Import) && index == inputCount) {
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
                            } else if (index == inputCount + 1) {
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
                                                    val words = pastedText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                                                    when {
                                                        words.size == legacyWordCount && isValidMnemonicForCurrentMode(words) -> {
                                                            legacy = true
                                                            inputs = words
                                                        }

                                                        words.size == shortWordCount && isValidMnemonicForCurrentMode(words) -> {
                                                            legacy = false
                                                            inputs = words
                                                        }

                                                        else -> {
                                                            errorInfo = invalidMnemonicPhrase
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
                            } else if (index == inputCount + 2) {
                                val clearEnabled = inputs.any { it.isNotBlank() }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable(enabled = state == MnemonicState.Display || clearEnabled) {
                                            if (state == MnemonicState.Input || state == MnemonicState.Verify || state == MnemonicState.Import) {
                                                inputs = inputs.map { "" }
                                            } else if (state == MnemonicState.Display) {
                                                val content = mnemonicList.joinToString(" ")
                                                context.copySensitiveTextToClipboard(content, coroutineScope)
                                                toast(R.string.copied_to_clipboard)
                                            }
                                        }
                                        .padding(8.dp)
                                ) {
                                    if (state == MnemonicState.Input || state == MnemonicState.Verify || state == MnemonicState.Import) {
                                        val clearColor = if (clearEnabled) MixinAppTheme.colors.textPrimary else MixinAppTheme.colors.textAssist
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_action_delete),
                                            contentDescription = null,
                                            tint = clearColor,
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            stringResource(R.string.Clear), fontSize = 12.sp,
                                            fontWeight = W500,
                                            color = clearColor,
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
                                text = invalidMnemonicPhrase, fontSize = 14.sp,
                                color = MixinAppTheme.colors.tipError,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (showImportSafety) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                modifier = Modifier.align(Alignment.Start),
                                text = stringResource(R.string.mnemonic_login_security_title),
                                fontSize = 14.sp,
                                color = MixinAppTheme.colors.textAssist,
                                fontWeight = W500,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                modifier = Modifier.align(Alignment.Start),
                                text = stringResource(R.string.mnemonic_login_security_tip_1),
                                fontSize = 13.sp,
                                color = MixinAppTheme.colors.textMinor,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                modifier = Modifier.align(Alignment.Start),
                                text = stringResource(R.string.mnemonic_login_security_tip_2),
                                fontSize = 13.sp,
                                color = MixinAppTheme.colors.textMinor,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                modifier = Modifier.align(Alignment.Start),
                                text = stringResource(R.string.mnemonic_login_security_tip_3),
                                fontSize = 13.sp,
                                color = MixinAppTheme.colors.textMinor,
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val previews = listOf(
                                    R.drawable.mixin_import_safety_preview_0,
                                    R.drawable.mixin_import_safety_preview_1,
                                    R.drawable.mixin_import_safety_preview_2,
                                    R.drawable.mixin_import_safety_preview_3,
                                    R.drawable.mixin_import_safety_preview_4,
                                    R.drawable.mixin_import_safety_preview_5,
                                    R.drawable.mixin_import_safety_preview_6,
                                )
                                previews.forEachIndexed { index, res ->
                                    if (index != 0) Spacer(modifier = Modifier.width(8.dp))
                                    Image(
                                        painter = painterResource(id = res),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
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
                                    MnemonicState.Input -> {
                                        hideInputMethod()
                                        onComplete.invoke(words)
                                    }
                                    MnemonicState.Import -> {
                                        val valid = (!legacy && words.size == 12 && isMnemonicValid(words)) ||
                                                (legacy && words.size == 24 && isMnemonicValid(words))
                                        if (!valid) {
                                            errorInfo = invalidMnemonicPhrase
                                        } else {
                                            onComplete.invoke(words)
                                        }
                                    }

                                    MnemonicState.Verify -> {
                                        coroutineScope.launch {
                                            runCatching {
                                                loading = true
                                                if (mnemonicList != words) {
                                                    errorInfo = invalidMnemonicPhrase
                                                } else {
                                                    val selfId = Session.getAccountId()!!
                                                    val seed = tip?.getOrRecoverTipPriv(context, pin!!)?.getOrThrow()
                                                    val edKey = tip?.getMnemonicEdKey(context, pin!!, seed!!)
                                                    val export = requireNotNull(saltExport) { "saltExport is required for MnemonicState.Verify" }
                                                    val encryptedTipBody = requireNotNull(getEncryptedTipBody) { "getEncryptedTipBody is required for MnemonicState.Verify" }
                                                    val r = export(
                                                        ExportRequest(
                                                            publicKey = edKey!!.publicKey.toHex(),
                                                            signature = initFromSeedAndSign(edKey.privateKey, selfId.toByteArray()).toHex(),
                                                            pinBase64 = encryptedTipBody(selfId, pin!!),
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
                                                errorInfo = ErrorHandler.getErrorMessage(it)
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
                                            state == MnemonicState.Input -> R.string.Sign_in_with_Mnemonic_Phrase
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
            }, floating = {
                if (suggestedWords.isNotEmpty()) {
                    InputBar(suggestedWords) { word ->
                        val maxInput = inputs.size
                        if (focusIndex in 0 until maxInput) {
                            inputs = inputs.toMutableList().also { it[focusIndex] = word }
                        }
                        if (focusIndex == maxInput - 1) {
                            keyboardController?.hide()
                            focusManager.moveFocus(FocusDirection.Right)
                        } else if ((focusIndex + 1) % 3 == 0) {
                            focusManager.moveFocus(FocusDirection.Down)
                            focusManager.moveFocus(FocusDirection.Left)
                            focusManager.moveFocus(FocusDirection.Left)
                        } else {
                            focusManager.moveFocus(FocusDirection.Right)
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
fun InputBar(list: List<String>, callback: (String) -> Unit) {
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
