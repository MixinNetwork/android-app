@file:OptIn(FlowPreview::class)

package one.mixin.android.ui.home.web3.trade

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_SWAP_LAST_PAIR
import one.mixin.android.Constants.Account.PREF_WEB3_SWAP_LAST_PAIR
import one.mixin.android.R
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.rate
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.ui.home.web3.components.InputAction
import one.mixin.android.ui.home.web3.components.InputArea
import one.mixin.android.ui.home.web3.components.PriceInfo
import one.mixin.android.ui.home.web3.components.TradeLayout
import one.mixin.android.ui.tip.wc.compose.Loading
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.GsonHelper
import java.math.BigDecimal

@Composable
fun SwapContent(
    from: SwapToken?,
    to: SwapToken?,
    inMixin: Boolean,
    initialAmount: String?,
    lastOrderTime: Long?,
    reviewing: Boolean,
    source: String,
    onSelectToken: (Boolean, SelectTokenType) -> Unit,
    onReview: (QuoteResult, SwapToken, SwapToken, String) -> Unit,
    onDeposit: (SwapToken) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel = hiltViewModel<SwapViewModel>()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var quoteResult by remember { mutableStateOf<QuoteResult?>(null) }
    var errorInfo by remember { mutableStateOf<String?>(null) }
    var quoteMin by remember { mutableStateOf<String?>(null) }
    var quoteMax by remember { mutableStateOf<String?>(null) }

    var inputText by remember { mutableStateOf(initialAmount ?: "") }
    LaunchedEffect(lastOrderTime) {
        inputText = initialAmount ?: ""
    }

    var isLoading by remember { mutableStateOf(false) }
    var isReverse by remember { mutableStateOf(false) }
    var invalidFlag by remember { mutableStateOf(false) }

    var fromToken by remember(from, to, isReverse) {
        mutableStateOf(if (isReverse) to else from)
    }
    var toToken by remember(from, to, isReverse) {
        mutableStateOf(if (isReverse) from else to)
    }

    val shouldRefreshQuote = remember { MutableStateFlow(inputText) }
    var isButtonEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(inputText, invalidFlag, reviewing, fromToken, toToken) {
        shouldRefreshQuote.emit(inputText)
    }

    LaunchedEffect(inputText, invalidFlag, reviewing, fromToken, toToken) {
        shouldRefreshQuote
            .debounce(300L)
            .collectLatest { text ->
                fromToken?.let { from ->
                    toToken?.let { to ->
                        if (text.isNotBlank() && runCatching { BigDecimal(text) }.getOrDefault(BigDecimal.ZERO) > BigDecimal.ZERO && !reviewing) {
                            isLoading = true
                            errorInfo = null
                            quoteMin = null
                            quoteMax = null
                            val amount = if (source == "") from.toLongAmount(text).toString() else text
                            viewModel.quote(context, from.symbol, from.assetId, to.assetId, amount, source)
                                .onSuccess { value ->
                                    quoteResult = value
                                    isLoading = false
                                }
                                .onFailure { exception ->
                                    if (exception is CancellationException) return@onFailure
                                    if (exception is AmountException) {
                                        quoteMin = exception.min
                                        quoteMax = exception.max
                                    }
                                    errorInfo = ErrorHandler.getErrorMessage(exception)
                                    quoteResult = null
                                    isLoading = false
                                }
                        } else {
                            errorInfo = null
                            quoteResult = null
                            quoteMin = null
                            quoteMax = null
                            isLoading = false
                        }
                    }
                }
            }
    }

    val rotation by animateFloatAsState(if (isReverse) 180f else 0f, label = "rotation")

    fromToken?.let { from ->
        val fromBalance = viewModel.tokenExtraFlow(from).collectAsStateWithLifecycle(from.balance).value
        KeyboardAwareBox(modifier = Modifier.fillMaxHeight(), content = { availableHeight ->
            Column(
                modifier = if (availableHeight != null) {
                    Modifier
                        .fillMaxWidth()
                        .height(availableHeight/5*4)
                } else {
                    Modifier.fillMaxSize()
                }
            ) {

                val scrollState = rememberScrollState()
                Column(
                    modifier = if (availableHeight != null) {
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .verticalScrollbar(scrollState)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    }
                ) {
                    TradeLayout(
                        centerCompose = {
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height(32.dp)
                                    .clip(CircleShape)
                                    .background(MixinAppTheme.colors.accent)
                                    .clickable {
                                        isLoading = true
                                        isReverse = !isReverse
                                        invalidFlag = !invalidFlag
                                        fromToken?.let { f ->
                                            toToken?.let { t ->
                                                val tokenPair = if (isReverse) listOf(t, f) else listOf(
                                                    f, t
                                                )
                                                val serializedPair = GsonHelper.customGson.toJson(tokenPair)
                                                context.defaultSharedPreferences.putString(
                                                    if (inMixin) PREF_SWAP_LAST_PAIR else PREF_WEB3_SWAP_LAST_PAIR, serializedPair
                                                )
                                            }
                                        }
                                        quoteResult?.let {
                                            inputText = it.outAmount
                                            quoteResult = null
                                        }
                                        context.clickVibrate()
                                    }
                                    .rotate(rotation),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_switch),
                                    contentDescription = null,
                                    tint = Color.White,
                                )
                            }
                        },
                        headerCompose = {
                            InputArea(
                                modifier = Modifier,
                                token = fromToken,
                                text = inputText,
                                title = stringResource(id = R.string.swap_send),
                                readOnly = false,
                                selectClick = { onSelectToken(isReverse, if (isReverse) SelectTokenType.To else SelectTokenType.From) },
                                onInputChanged = { inputText = it },
                                onDeposit = onDeposit,
                                onMax = {
                                    val balance = fromBalance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                    if (balance > BigDecimal.ZERO) {
                                        inputText = balance.stripTrailingZeros().toPlainString()
                                    } else {
                                        inputText = ""
                                    }
                                },
                            )
                        },
                        bottomCompose = {
                            InputArea(
                                modifier = Modifier,
                                token = toToken,
                                text = toToken?.toStringAmount(quoteResult?.outAmount ?: "0") ?: "",
                                title = stringResource(id = R.string.swap_receive),
                                readOnly = true,
                                selectClick = { onSelectToken(isReverse, if (isReverse) SelectTokenType.From else SelectTokenType.To) },
                                onDeposit = null,
                            )
                        },
                        margin = 6.dp,
                    )
                }
                QuoteInfoBox(
                    availableHeight = availableHeight,
                    errorInfo = errorInfo,
                    quoteResult = quoteResult,
                    fromToken = fromToken,
                    toToken = toToken,
                    isLoading = isLoading,
                    inputText = inputText,
                    quoteMin = quoteMin,
                    quoteMax = quoteMax,
                    onInputTextChange = { inputText = it },
                    onInvalidFlagChange = { invalidFlag = !invalidFlag }
                )
                Spacer(modifier = Modifier.height(if (availableHeight == null) 14.dp else 8.dp))
                ReviewButton(
                    inputText = inputText,
                    fromBalance = fromBalance,
                    fromToken = fromToken!!,
                    quoteResult = quoteResult,
                    errorInfo = errorInfo,
                    isLoading = isLoading,
                    isButtonEnabled = isButtonEnabled,
                    onButtonEnabledChange = { isButtonEnabled = it },
                    onReview = { onReview(it, fromToken!!, toToken!!, inputText) },
                    keyboardController = keyboardController,
                    focusManager = focusManager,
                    scope = scope
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }, floating = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MixinAppTheme.colors.backgroundWindow)
                    .padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val balance = fromBalance?.toBigDecimalOrNull() ?: BigDecimal.ZERO

                InputAction("25%", showBorder = true) {
                    if (balance > BigDecimal.ZERO) {
                        inputText = (balance * BigDecimal("0.25")).stripTrailingZeros().toPlainString()
                    } else {
                        inputText = ""
                    }
                }
                InputAction("50%", showBorder = true) {
                    if (balance > BigDecimal.ZERO) {
                        inputText = (balance * BigDecimal("0.5")).stripTrailingZeros().toPlainString()
                    } else {
                        inputText = ""
                    }
                }
                InputAction(stringResource(R.string.Max), showBorder = true) {
                    if (balance > BigDecimal.ZERO) {
                        inputText = balance.stripTrailingZeros().toPlainString()
                    } else {
                        inputText = ""
                    }
                }
                InputAction(stringResource(R.string.Done), showBorder = false) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            }
        })
    } ?: run {
        Loading()
    }
}

@Composable
fun ReviewButton(
    inputText: String,
    fromBalance: String?,
    fromToken: SwapToken,
    quoteResult: QuoteResult?,
    errorInfo: String?,
    isLoading: Boolean,
    isButtonEnabled: Boolean,
    onButtonEnabledChange: (Boolean) -> Unit,
    onReview: (QuoteResult) -> Unit,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager,
    scope: CoroutineScope
) {
    val checkBalance = checkBalance(inputText, fromBalance)
    
    Button(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(48.dp),
        onClick = {
            if (isButtonEnabled) {
                onButtonEnabledChange(false)
                quoteResult?.let { onReview(it) }
                keyboardController?.hide()
                focusManager.clearFocus()
                scope.launch {
                    delay(1000)
                    onButtonEnabledChange(true)
                }
            }
        },
        enabled = quoteResult != null && errorInfo == null && !isLoading && checkBalance == true,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = if (quoteResult != null && errorInfo == null && checkBalance == true) {
                MixinAppTheme.colors.accent
            } else {
                MixinAppTheme.colors.backgroundGrayLight
            },
        ),
        shape = RoundedCornerShape(32.dp),
        elevation = ButtonDefaults.elevation(
            pressedElevation = 0.dp,
            defaultElevation = 0.dp,
            hoveredElevation = 0.dp,
            focusedElevation = 0.dp,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = if (quoteResult != null && errorInfo == null && checkBalance == true) {
                    Color.White
                } else {
                    MixinAppTheme.colors.textAssist
                },
            )
        } else {
            Text(
                text = if (checkBalance == false) {
                    "${fromToken.symbol} ${stringResource(R.string.insufficient_balance)}"
                } else {
                    stringResource(R.string.Review_Order)
                },
                color = if (checkBalance != true || errorInfo != null) {
                    MixinAppTheme.colors.textAssist
                } else {
                    Color.White
                },
            )
        }
    }
}

@Composable
fun QuoteInfoBox(
    availableHeight: Dp?,
    errorInfo: String?,
    quoteResult: QuoteResult?,
    fromToken: SwapToken?,
    toToken: SwapToken?,
    isLoading: Boolean,
    inputText: String,
    quoteMin: String?,
    quoteMax: String?,
    onInputTextChange: (String) -> Unit,
    onInvalidFlagChange: () -> Unit
) {
    Box(
        modifier = if (availableHeight == null) {
            Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 20.dp)
        } else {
            Modifier
                .padding(vertical = 14.dp)
                .padding(horizontal = 20.dp)
        }
    ) {
        if (errorInfo.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 20.dp)
                    .alpha(if (quoteResult == null) 0f else 1f),
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
                                onInvalidFlagChange()
                            }
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .alpha(if (errorInfo.isBlank()) 0f else 1f)
            ) {
                Text(
                    text = errorInfo,
                    modifier = Modifier.clickable {
                        if (quoteMax != null || quoteMin != null) {
                            if (quoteMax != null && runCatching { BigDecimal(inputText) }.getOrDefault(BigDecimal.ZERO) > runCatching { BigDecimal(quoteMax) }.getOrDefault(BigDecimal.ZERO)) {
                                onInputTextChange(quoteMax)
                            } else if (quoteMin != null) {
                                onInputTextChange(quoteMin)
                            }
                        }
                    },
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.tipError,
                    ),
                )
            }
        }
    }
}
