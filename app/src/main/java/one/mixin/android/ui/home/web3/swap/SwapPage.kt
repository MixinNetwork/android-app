@file:OptIn(FlowPreview::class)

package one.mixin.android.ui.home.web3.swap

import PageScaffold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_SWAP_LAST_SELECTED_PAIR
import one.mixin.android.R
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.rate
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.putString
import one.mixin.android.ui.tip.wc.compose.Loading
import one.mixin.android.ui.wallet.DepositFragment
import java.math.BigDecimal
import java.math.RoundingMode

@FlowPreview
@Composable
fun SwapPage(
    from: SwapToken?,
    to: SwapToken?,
    initialAmount: String?,
    lastOrderTime: Long?,
    reviewing: Boolean,
    source: String,
    slippageBps: Int,
    onSelectToken: (Boolean, SelectTokenType) -> Unit,
    onSwap: (QuoteResult, SwapToken, SwapToken, String) -> Unit,
    onShowSlippage: () -> Unit,
    pop: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val viewModel = hiltViewModel<SwapViewModel>()

    var quoteResult by remember { mutableStateOf<QuoteResult?>(null) }
    var errorInfo by remember { mutableStateOf<String?>(null) }

    var inputText by remember { mutableStateOf(initialAmount ?: "") }
    LaunchedEffect(lastOrderTime) {
        inputText = initialAmount ?: ""
    }

    var isLoading by remember { mutableStateOf(false) }
    var isReverse by remember { mutableStateOf(false) }
    var invalidFlag by remember { mutableStateOf(false) } // trigger to refresh quote

    var fromToken by remember(from, to, isReverse) {
        mutableStateOf(if (isReverse) to else from)
    }
    var toToken by remember(from, to, isReverse) {
        mutableStateOf(if (isReverse) from else to)
    }

    val shouldRefreshQuote = remember { MutableStateFlow(inputText) }
    var isButtonEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(inputText, invalidFlag, reviewing, fromToken, toToken)  {
        shouldRefreshQuote.emit(inputText)
    }

    LaunchedEffect(inputText, invalidFlag, reviewing, fromToken, toToken) {
        shouldRefreshQuote
            .debounce(300L)
            .collectLatest { text ->
                fromToken?.let { from ->
                    toToken?.let { to ->
                        if (text.isNotBlank() && !reviewing) {
                            isLoading = true
                            errorInfo = null
                            val amount = if (source == "") from.toLongAmount(text).toString() else text
                            viewModel.quote(context, from.symbol, from.getUnique(), to.getUnique(), amount, slippageBps.toString(), source)
                                .onSuccess { value ->
                                    quoteResult = value
                                    isLoading = false
                                }
                                .onFailure { exception ->
                                    if (exception is CancellationException) return@onFailure
                                    errorInfo = exception.message
                                    quoteResult = null
                                    isLoading = false
                                }
                        } else {
                            errorInfo = null
                            quoteResult = null
                            isLoading = false
                        }
                    }
                }
            }
    }

    val rotation by animateFloatAsState(if (isReverse) 180f else 0f, label = "rotation")

    PageScaffold(
        title = stringResource(id = R.string.Swap),
        verticalScrollable = true,
        pop = pop,
    ) {
        fromToken?.let { from ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
            ) {
                SwapLayout(
                    centerCompose = {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(40.dp)
                                .clip(CircleShape)
                                .border(width = 6.dp, color = MixinAppTheme.colors.background, shape = CircleShape)
                                .background(MixinAppTheme.colors.backgroundGrayLight)
                                .clickable {
                                    isLoading = true
                                    isReverse = !isReverse
                                    invalidFlag = !invalidFlag
                                    fromToken?.let { f ->
                                        toToken?.let { t ->
                                            context.defaultSharedPreferences.putString(PREF_SWAP_LAST_SELECTED_PAIR, if (isReverse) "${t.getUnique()} ${f.getUnique()}" else "${f.getUnique()} ${t.getUnique()}")
                                        }
                                    }
                                    context.clickVibrate()
                                }
                                .rotate(rotation),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_switch),
                                contentDescription = null,
                                tint = MixinAppTheme.colors.textPrimary,
                            )
                        }
                    },
                    headerCompose = {
                        InputArea(
                            token = fromToken,
                            text = inputText,
                            title = stringResource(id = R.string.Token_From),
                            readOnly = false,
                            selectClick = { onSelectToken(isReverse, if (isReverse) SelectTokenType.To else SelectTokenType.From) },
                            onInputChanged = { inputText = it },
                            onMax = {
                                inputText = fromToken?.balance ?: "0"
                            }
                        )
                    },
                    bottomCompose = {
                        InputArea(
                            token = toToken,
                            text = toToken?.toStringAmount(quoteResult?.outAmount ?: "0") ?: "",
                            title = stringResource(id = R.string.To),
                            readOnly = true,
                            selectClick = { onSelectToken(isReverse, if (isReverse) SelectTokenType.From  else SelectTokenType.To) }
                        )
                    },
                    margin = 6.dp,
                )

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    if (errorInfo.isNullOrBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .alpha(if (quoteResult == null) 0f else 1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MixinAppTheme.colors.backgroundGrayLight)
                                .padding(20.dp),
                        ) {
                            quoteResult?.let { quote ->
                                val rate = quote.rate(fromToken, toToken)
                                if (rate != BigDecimal.ZERO) {
                                    PriceInfo(
                                        fromToken = fromToken!!,
                                        toToken = toToken,
                                        isLoading = isLoading,
                                        exchangeRate = rate,
                                        onPriceExpired = {
                                            invalidFlag = !invalidFlag
                                        }
                                    )
                                }
                                if (!from.inMixin()) {
                                    SlippageInfo(slippageBps, rate != BigDecimal.ZERO, onShowSlippage)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .alpha(if (errorInfo.isNullOrBlank()) 0f else 1f)
                                .background(MixinAppTheme.colors.backgroundGrayLight)
                                .padding(20.dp),
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = errorInfo?:"",
                                style =
                                TextStyle(
                                    fontSize = 14.sp,
                                    color = MixinAppTheme.colors.tipError,
                                ),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    val keyboardController = LocalSoftwareKeyboardController.current
                    val focusManager = LocalFocusManager.current
                    val checkBalance = checkBalance(inputText, fromToken?.balance)
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = {
                            if (isButtonEnabled) {
                                isButtonEnabled = false
                                quoteResult?.let { onSwap(it, fromToken!!, toToken!!, inputText) }
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                scope.launch{
                                    delay(1000)
                                    isButtonEnabled = true
                                }
                            }
                        },
                        enabled = quoteResult != null && errorInfo == null && !isLoading && checkBalance == true,
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                backgroundColor = if (quoteResult != null && errorInfo == null && checkBalance == true) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight,
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
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = if (quoteResult != null && errorInfo == null && checkBalance == true) Color.White else MixinAppTheme.colors.textAssist,
                            )
                        } else {
                            Text(
                                text = if (checkBalance == false) "${fromToken?.symbol} ${stringResource(R.string.insufficient_balance)}" else stringResource(R.string.Review_Order),
                                color = if (checkBalance != true || errorInfo != null) MixinAppTheme.colors.textAssist else Color.White,
                            )
                        }
                    }
                }
            }
        } ?: run {
            Loading()
        }
    }
}

@Composable
fun InputArea(
    token: SwapToken?,
    text: String,
    title: String,
    readOnly: Boolean = false,
    selectClick: () -> Unit,
    onMax: (() -> Unit)? = null,
    onInputChanged: ((String) -> Unit)? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(MixinAppTheme.colors.backgroundGrayLight)
                .padding(20.dp, 20.dp, 20.dp, if (readOnly) 20.dp else 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 12.sp, color = MixinAppTheme.colors.textAssist)
                Spacer(modifier = Modifier.width(4.dp))
                token?.let { 
                    CoilImage(
                        model = it.chain.icon,
                        modifier =
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape),
                        placeholder = R.drawable.ic_avatar_place_holder,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                token?.let { 
                    Text(text = it.chain.name, fontSize = 12.sp, color = MixinAppTheme.colors.textAssist)
                } ?: run {
                    Text(text = stringResource(id = R.string.select_token), fontSize = 12.sp, color = MixinAppTheme.colors.textMinor)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_web3_wallet),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textAssist,
                )
                Spacer(modifier = Modifier.width(4.dp))
                token?.let { 
                    Text(
                        text = it.balance ?: "0",
                        style =
                            TextStyle(
                                fontSize = 12.sp,
                                color = MixinAppTheme.colors.textAssist,
                                textAlign = TextAlign.End,
                            ),
                    )
                } ?: run {
                    Text(
                        text = "0",
                        style =
                            TextStyle(
                                fontSize = 12.sp,
                                color = MixinAppTheme.colors.textAssist,
                                textAlign = TextAlign.End,
                            ),
                    )
                }
                if (!readOnly) {
                    Spacer(modifier = Modifier.width(8.dp))
                    InputAction(text = stringResource(id = R.string.balance_max)) {
                        onMax?.invoke()
                    }
                }
            }
        }
        Box(modifier = Modifier.height(16.dp))
        InputContent(token = token, text = text, selectClick = selectClick, onInputChanged = onInputChanged, readOnly = readOnly)
    }
}

@Composable
private fun PriceInfo(
    fromToken: SwapToken,
    toToken: SwapToken?,
    isLoading: Boolean,
    exchangeRate: BigDecimal,
    onPriceExpired: () -> Unit
) {
    var isPriceReverse by remember {
        mutableStateOf(
            false
        )
    }
    var quoteCountDown by remember { mutableFloatStateOf(0f) }

    LaunchedEffect("${fromToken.getUnique()}-${toToken?.getUnique()}") {
        isPriceReverse = fromToken.assetId in DepositFragment.usdcAssets || fromToken.assetId in DepositFragment.usdtAssets
    }

    LaunchedEffect("${fromToken.getUnique()}-${toToken?.getUnique()}-${exchangeRate}") {
        while (isActive) {
            quoteCountDown = 0f
            while (isActive && quoteCountDown < 1f) { // 10s
                delay(100)
                quoteCountDown += 0.01f
            }
            onPriceExpired()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isPriceReverse) {
                    "1 ${toToken?.symbol} ≈ ${BigDecimal.ONE.divide(exchangeRate,  8, RoundingMode.HALF_UP).numberFormat8()} ${fromToken.symbol}"
                } else {
                    "1 ${fromToken.symbol} ≈ ${exchangeRate.numberFormat8()} ${toToken?.symbol}"
                },
                maxLines = 1,
                style =
                TextStyle(
                    fontWeight = FontWeight.W400,
                    color = MixinAppTheme.colors.textAssist,
                ),
            )
            Spacer(modifier = Modifier.width(4.dp))
            if (!isLoading)
                CircularProgressIndicator(
                    progress = quoteCountDown,
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = MixinAppTheme.colors.textPrimary,
                    backgroundColor = MixinAppTheme.colors.textAssist,
                )
        }
        if (isLoading) {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(12.dp)
                        .height(24.dp),
                    color = MixinAppTheme.colors.accent,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            Icon(
                modifier =
                    Modifier
                        .size(24.dp)
                        .rotate(90f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            isPriceReverse = !isPriceReverse
                        },
                painter = painterResource(id = R.drawable.ic_switch),
                contentDescription = null,
                tint = MixinAppTheme.colors.icon,
            )
        }
    }
}

@Composable
private fun SlippageInfo(
    slippageBps: Int,
    enableClick: Boolean,
    onShowSlippage: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val descText = "${slippageBps.slippageBpsDisplay()}%"
    val highSlippage = slippageBps > SwapFragment.DangerousSlippage
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable(
                    interactionSource,
                    null,
                    enableClick,
                ) {
                    onShowSlippage.invoke()
                },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.Slippage),
            maxLines = 1,
            style =
                TextStyle(
                    fontWeight = FontWeight.W400,
                    color = MixinAppTheme.colors.textAssist,
                ),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = descText,
                maxLines = 1,
                style =
                    TextStyle(
                        fontWeight = FontWeight.W400,
                        color = if (highSlippage) MixinAppTheme.colors.tipError else MixinAppTheme.colors.textPrimary,
                    ),
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MixinAppTheme.colors.textAssist,
            )
        }
    }
    if (highSlippage) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = R.drawable.ic_warning),
                contentDescription = null,
                tint = MixinAppTheme.colors.tipError,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.slippage_high_warning),
                style =
                    TextStyle(
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.tipError,
                    ),
            )
        }
    }
}

@Composable
private fun InputAction(
    text: String,
    onAction: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier =
            Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .border(0.7.dp, color = if (isPressed) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist, RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    onAction.invoke()
                }
                .padding(6.dp, 3.dp),
    ) {
        Text(
            text = text,
            style =
                TextStyle(
                    fontSize = 10.sp,
                    color = if (isPressed) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist,
                ),
        )
    }
}

@Composable
fun SwapLayout(
    headerCompose: @Composable () -> Unit,
    bottomCompose: @Composable () -> Unit,
    centerCompose: @Composable () -> Unit,
    margin: Dp,
) {
    ConstraintLayout(
        modifier =
            Modifier
                .wrapContentHeight()
                .wrapContentWidth()
                .padding(horizontal = 20.dp, vertical = margin),
    ) {
        val (headerRef, bottomRef, centerRef) = createRefs()
        Box(
            modifier =
                Modifier.constrainAs(headerRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(bottomRef.top, margin)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
        ) {
            headerCompose()
        }
        Box(
            modifier =
                Modifier.constrainAs(bottomRef) {
                    top.linkTo(parent.bottom)
                    bottom.linkTo(headerRef.bottom, margin)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
        ) {
            bottomCompose()
        }
        Box(
            modifier =
                Modifier.constrainAs(centerRef) {
                    top.linkTo(headerRef.bottom)
                    bottom.linkTo(bottomRef.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
        ) {
            centerCompose()
        }
    }
}

@Preview
@Composable
fun SwapLayoutPreview() {
    SwapLayout(
        headerCompose = {
            Box(
                modifier =
                    Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .background(color = Color.Red),
            )
        },
        bottomCompose = {
            Box(
                modifier =
                    Modifier
                        .height(140.dp)
                        .fillMaxWidth()
                        .background(color = Color.Green),
            )
        },
        centerCompose = {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(color = Color.Blue),
            )
        },
        margin = 20.dp,
    )
}

@Preview
@Composable
fun PreviewSlippageInfo() {
    SlippageInfo(slippageBps = 50, true) {}
}

@Preview
@Composable
fun PreviewSlippageInfoWarning() {
    SlippageInfo(slippageBps = 600, true) {}
}

@Preview
@Composable
fun PreviewInputActionMax() {
    InputAction("MAX") {}
}

/**
 * @return True if the input was successful, false if the balance is insufficient, or null if the input is invalid.
 */
fun checkBalance(
    inputText: String,
    balance: String?,
): Boolean? {
    if (balance.isNullOrEmpty()) return false
    val inputValue =
        try {
            BigDecimal(inputText)
        } catch (_: Exception) {
            null
        } ?: return null
    if (inputValue <= BigDecimal.ZERO) return null
    val balanceValue =
        try {
            BigDecimal(balance)
        } catch (_: Exception) {
            null
        } ?: return null
    return inputValue <= balanceValue
}
