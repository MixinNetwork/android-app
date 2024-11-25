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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.ExportRequest
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.crypto.getMatchingWords
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.isMnemonicValid
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.toHex
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.getMixinErrorStringByCode

@Composable
fun MnemonicPhraseInput(
    state: MnemonicState,
    mnemonicList: List<String> = emptyList(),
    onComplete: (List<String>) -> Unit,
    tip: Tip? = null,
    pin: String? = null
) {
    var legacy by remember { mutableStateOf(mnemonicList.size > 13) }
    var inputs by remember { mutableStateOf(List(if (!legacy) 13 else 25) { "" }) }
    var loading by remember { mutableStateOf(false) }
    var errorInfo by remember { mutableStateOf("") }
    val context = LocalContext.current
    val walletViewModel = hiltViewModel<WalletViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var currentText by remember { mutableStateOf("") }
    var focusIndex by remember { mutableIntStateOf(-1) }
    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
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
                        MnemonicState.Display -> stringResource(R.string.write_down_mnemonic_phrase)
                        MnemonicState.Verify -> stringResource(R.string.check_mnemonic_phrase)
                    }, fontSize = 18.sp,
                    color = MixinAppTheme.colors.textPrimary,
                    fontWeight = SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when (state) {
                        MnemonicState.Input -> stringResource(R.string.enter_mnemonic_phrase, if (legacy) 25 else 13)
                        MnemonicState.Display -> stringResource(R.string.write_down_mnemonic_phrase_desc)
                        MnemonicState.Verify -> stringResource(R.string.check_mnemonic_phrase_desc)
                    }, fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                    textAlign = TextAlign.Center
                )
                if (state == MnemonicState.Input) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.Start, modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        WordCountButton(
                            onClick = {
                                legacy = false
                                inputs = List(13) { "" }
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (!legacy) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGray,
                            ),
                        ) {
                            Text("13 words", color = if (!legacy) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        WordCountButton(
                            onClick = {
                                legacy = true
                                inputs = List(25) { "" }
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (legacy) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGray
                            ),
                        ) {
                            Text("25 words", color = if (legacy) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Spacer(modifier = Modifier.height(44.dp))
                }
                InputGrid(if (legacy) 27 else 15, 10.dp) { index ->
                    if (index < inputs.size) {
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
                                        color = MixinAppTheme.colors.textMinor,
                                        fontSize = 13.sp,
                                        fontWeight = W500
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = if (inputs.all { it.isNotEmpty() }) ImeAction.Done else ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = {
                                            focusManager.moveFocus(FocusDirection.Right)
                                        },
                                        onDone = {
                                            val words = inputs.map { it.trim() }
                                            when (state) {
                                                MnemonicState.Input -> onComplete.invoke(words)
                                                MnemonicState.Verify -> {
                                                    coroutineScope.launch {
                                                        runCatching {
                                                            loading = true
                                                            if (mnemonicList != words) {
                                                                errorInfo = context.getString(R.string.invalid_mnemonic_phrase)
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
                    } else if (index == if (legacy) 25 else 13) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (state == MnemonicState.Input) 1f else 0f)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    if (state == MnemonicState.Input) {
                                        val clipboard = context.getClipboardManager()
                                        val clipData: ClipData? = clipboard.primaryClip
                                        if (clipData != null && clipData.itemCount > 0) {
                                            val pastedText = clipData.getItemAt(0).text.toString()
                                            val words = pastedText.split(" ")
                                            if (legacy && words.size == 25 && isMnemonicValid(words.subList(0, 24))) {
                                                inputs = words
                                            } else if (words.size == 13 && isMnemonicValid(words.subList(0, 12))) {
                                                inputs = words
                                            } else {
                                                errorInfo = context.getString(R.string.invalid_mnemonic_phrase)
                                            }
                                        }
                                    }
                                }
                                .padding(8.dp)
                        ) {
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
                    } else if (index == if (legacy) 26 else 14) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    if (state == MnemonicState.Input || state == MnemonicState.Verify) {
                                        inputs = inputs.map { "" }
                                    } else if (state == MnemonicState.Display) {
                                        val clipboard = context.getClipboardManager()
                                        clipboard.setPrimaryClip(ClipData.newPlainText(null, mnemonicList.joinToString(" ")))
                                    }
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
                }
                Spacer(modifier = Modifier.height(if (legacy) 20.dp else 8.dp))
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = state == MnemonicState.Display || inputs.all { it.isNotEmpty() },
                    onClick = {
                        val words = inputs.map { it.trim() }
                        when (state) {
                            MnemonicState.Input -> onComplete.invoke(words)
                            MnemonicState.Verify -> {
                                coroutineScope.launch {
                                    runCatching {
                                        loading = true
                                        if (mnemonicList != words) {
                                            errorInfo = context.getString(R.string.invalid_mnemonic_phrase)
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
                        Text(
                            stringResource(
                                when (state) {
                                    MnemonicState.Display -> R.string.Check_Backup
                                    MnemonicState.Input -> R.string.Confirm
                                    MnemonicState.Verify -> R.string.Complete
                                }
                            ),
                            color = Color.White,
                            fontWeight = W500,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
        if (state == MnemonicState.Input || state == MnemonicState.Verify) {
            InputBar(currentText) { word ->
                inputs = inputs.toMutableList().also { it[focusIndex] = word }
            }
        }
    }
}

@Composable
fun InputGrid(
    size: Int,
    spacing: Dp,
    content: @Composable (index: Int) -> Unit
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
    Verify
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
fun KeyboardFloatingView(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val density = LocalDensity.current
    val windowInsets = WindowInsets.ime

    var keyboardHeight by remember { mutableStateOf(0.dp) }

    val keyboardHeightDp = with(density) {
        windowInsets
            .getBottom(density)
            .toDp()
    }

    LaunchedEffect(keyboardHeightDp) {
        keyboardHeight = keyboardHeightDp
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .offset(y = -keyboardHeight)
        ) {
            content()
        }
    }
}

@Composable
fun InputBar(string: String, callback: (String) -> Unit) {
    if (string.isBlank()) return
    val list = getMatchingWords(string.trim()) ?: return
    KeyboardFloatingView {
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
}

@Preview(backgroundColor = 0xFFFFFFFF, showSystemUi = true)
@Composable
fun MnemonicPhraseInputPreview() {
    MnemonicPhraseInput(
        state = MnemonicState.Input,
        onComplete = { mnemonicList -> /* Handle mnemonic change */ },
    )
}
