@file:OptIn(FlowPreview::class)

package one.mixin.android.ui.home.web3.swap

import PageScaffold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_SWAP_LAST_PAIR
import one.mixin.android.Constants.Account.PREF_WEB3_SWAP_LAST_PAIR
import one.mixin.android.R
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.rate
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putString
import one.mixin.android.ui.tip.wc.compose.Loading
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.WalletCategory
import java.math.BigDecimal
import java.math.RoundingMode

@FlowPreview
@Composable
fun SwapPage(
    walletId: String?,
    from: SwapToken?,
    to: SwapToken?,
    inMixin: Boolean,
    orderBadge: Boolean,
    initialAmount: String?,
    lastOrderTime: Long?,
    reviewing: Boolean,
    source: String,
    onSelectToken: (Boolean, SelectTokenType) -> Unit,
    onReview: (QuoteResult, SwapToken, SwapToken, String) -> Unit,
    onDeposit: (SwapToken) -> Unit,
    onOrderList: () -> Unit,
    pop: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel = hiltViewModel<SwapViewModel>()
    var walletDisplayName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(walletId) {
        if (walletId != null) {
            viewModel.findWeb3WalletById(walletId)?.let {
                if (it.category == WalletCategory.IMPORTED_MNEMONIC.value ||
                    it.category == WalletCategory.IMPORTED_PRIVATE_KEY.value ||
                    it.category == WalletCategory.WATCH_ADDRESS.value) {
                    walletDisplayName = it.name
                }
            }
        }
    }

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
                        if (text.isNotBlank() && runCatching { BigDecimal(text) }.getOrDefault(BigDecimal.ZERO) > BigDecimal.ZERO && !reviewing) {
                            isLoading = true
                            errorInfo = null
                            quoteMin = null
                            quoteMax = null
                            val amount = if (source == "") from.toLongAmount(text).toString() else text
                            viewModel.quote(context, from.symbol, from.assetId, to.assetId, amount, source)
                                .onSuccess { value ->
                                    AnalyticsTracker.trackSwapQuote("success")
                                    quoteResult = value
                                    isLoading = false
                                }
                                .onFailure { exception ->
                                    AnalyticsTracker.trackSwapQuote("failure")
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

    PageScaffold(
        title = stringResource(id = R.string.Swap),
        subtitle = {
            val text = if (walletId == null) {
                stringResource(id = R.string.Privacy_Wallet)
            } else {
                walletDisplayName ?: stringResource(id = R.string.Common_Wallet)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (walletId == null) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_wallet_privacy),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = text,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MixinAppTheme.colors.textAssist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        verticalScrollable = true,
        pop = pop,
        actions = {
            if (source != "web3") {
                Box {
                    IconButton(onClick = {
                        onOrderList()
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_order),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.icon,
                        )
                    }
                    if (orderBadge) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .offset(x = (-12).dp, y = (12).dp)
                                .background(
                                    color = MixinAppTheme.colors.badgeRed,
                                    shape = CircleShape
                                )
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
            IconButton(onClick = {
                context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_support),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
            }
        },
    ) {
        fromToken?.let { from ->
            val fromBalance = if (walletId.isNullOrBlank()) {
                from.balance
            } else {
                viewModel.tokenExtraFlow(from).collectAsStateWithLifecycle(from.balance).value
            }

            KeyboardAwareBox(
                modifier = Modifier.fillMaxHeight(),
                content = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .imePadding(),
                    ) {
                        SwapLayout(
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
                                                    val tokenPair =
                                                        if (isReverse) listOf(t, f) else listOf(
                                                            f,
                                                            t
                                                        )
                                                    val serializedPair =
                                                        GsonHelper.customGson.toJson(tokenPair)
                                                    context.defaultSharedPreferences.putString(
                                                        if (inMixin) PREF_SWAP_LAST_PAIR else PREF_WEB3_SWAP_LAST_PAIR,
                                                        serializedPair
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
                                    inMixin = inMixin,
                                    text = inputText,
                                    title = stringResource(id = R.string.swap_send),
                                    readOnly = false,
                                    selectClick = { onSelectToken(isReverse, if (isReverse) SelectTokenType.To else SelectTokenType.From) },
                                    onInputChanged = { inputText = it },
                                    onDeposit = onDeposit,
                                    walletId = walletId,
                                    onMax = {
                                        val balance = fromBalance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                        if (balance > BigDecimal.ZERO) {
                                            inputText = balance.stripTrailingZeros().toPlainString()
                                        } else {
                                            inputText = ""
                                        }
                                    }
                                )
                            },
                            bottomCompose = {
                                InputArea(
                                    modifier = Modifier,
                                    token = toToken,
                                    inMixin = inMixin,
                                    text = toToken?.toStringAmount(quoteResult?.outAmount ?: "0") ?: "",
                                    title = stringResource(id = R.string.swap_receive),
                                    readOnly = true,
                                    selectClick = { onSelectToken(isReverse, if (isReverse) SelectTokenType.From else SelectTokenType.To) },
                                    onDeposit = null,
                                    walletId = walletId,
                                )
                            },
                            margin = 6.dp,
                        )

                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Box(modifier = Modifier.heightIn(min = 68.dp)) {
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
                                                        invalidFlag = !invalidFlag
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
                                            .alpha(if (errorInfo.isNullOrBlank()) 0f else 1f)
                                    ) {
                                        Text(
                                            text = errorInfo ?: "",
                                            modifier = Modifier
                                                .clickable {
                                                    if (quoteMax != null || quoteMin != null) {
                                                        if (quoteMax != null && runCatching { BigDecimal(inputText) }.getOrDefault(BigDecimal.ZERO) > runCatching { BigDecimal(quoteMax!!) }.getOrDefault(BigDecimal.ZERO)) {
                                                            inputText = quoteMax!!
                                                        } else if (quoteMin != null) {
                                                            inputText = quoteMin!!
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
                            Spacer(modifier = Modifier.height(14.dp))
                            val keyboardController = LocalSoftwareKeyboardController.current
                            val focusManager = LocalFocusManager.current
                            val checkBalance = checkBalance(inputText, fromBalance)
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                onClick = {
                                    if (isButtonEnabled) {
                                        isButtonEnabled = false
                                        quoteResult?.let { onReview(it, fromToken!!, toToken!!, inputText) }
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        scope.launch {
                                            delay(1000)
                                            isButtonEnabled = true
                                        }
                                    }
                                },
                                enabled = quoteResult != null && errorInfo == null && !isLoading && checkBalance == true,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = if (quoteResult != null && errorInfo == null && checkBalance == true) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight,
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
                },
                floating = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MixinAppTheme.colors.backgroundWindow)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val keyboardController = LocalSoftwareKeyboardController.current
                        val focusManager = LocalFocusManager.current
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
                }
            )
        } ?: run {
            Loading()
        }
    }
}

@Composable
fun InputArea(
    modifier: Modifier = Modifier,
    token: SwapToken?,
    inMixin: Boolean,
    text: String,
    title: String,
    readOnly: Boolean,
    selectClick: () -> Unit,
    onInputChanged: ((String) -> Unit)? = null,
    onDeposit: ((SwapToken) -> Unit)? = null,
    onMax: (() -> Unit)? = null,
    walletId: String? = null,
) {
    val viewModel = hiltViewModel<SwapViewModel>()
    val balance = if (token == null) {
        null
    } else {
        if (walletId.isNullOrBlank()) {
            token.balance
        } else {
            viewModel.tokenExtraFlow(token).collectAsStateWithLifecycle(token.balance).value
        }
    }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(8.dp))
                .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 14.sp, color = MixinAppTheme.colors.textPrimary)
                Spacer(modifier = Modifier.weight(1f))
                token?.let {
                    Text(text = it.chain.name, fontSize = 12.sp, color = MixinAppTheme.colors.textAssist)
                } ?: run {
                    Text(text = stringResource(id = R.string.select_token), fontSize = 14.sp, color = MixinAppTheme.colors.textMinor)
                }
            }
        }
        Box(modifier = Modifier.height(10.dp))
        InputContent(token = token, text = text, selectClick = selectClick, onInputChanged = onInputChanged, readOnly = readOnly)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            token?.let { t->
                Text(
                    text = stringResource(id = R.string.Deposit),
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textBlue,
                    ),
                    modifier = Modifier
                        .alpha(
                            if (!readOnly && onDeposit != null && (balance?.toBigDecimalOrNull()
                                    ?.compareTo(BigDecimal.ZERO) ?: 0) == 0
                            ) 1f
                            else 0f
                        )
                        .clickable { onDeposit?.invoke(t) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_web3_wallet),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textAssist,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (token.isWeb3) {
                        balance?.numberFormat() ?: "0"
                    } else {
                        balance?.numberFormat8() ?: "0"
                    },
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist,
                        textAlign = TextAlign.End,
                    ),
                    modifier = Modifier.clickable { onMax?.invoke() }
                )
            } ?: run {
                Text(
                    text = "0",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist,
                        textAlign = TextAlign.End,
                    ),
                )
            }
        }
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

    LaunchedEffect("${fromToken.assetId}-${toToken?.assetId}") {
        isPriceReverse = fromToken.assetId in Constants.AssetId.usdcAssets || fromToken.assetId in Constants.AssetId.usdtAssets
    }

    LaunchedEffect("${fromToken.assetId}-${toToken?.assetId}-${exchangeRate}") {
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
    showBorder: Boolean = true,
    onAction: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = if (showBorder) {
            Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(MixinAppTheme.colors.background)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    onAction.invoke()
                }
                .padding(32.dp, 6.dp)
        } else {
            Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    onAction.invoke()
                }
                .padding(8.dp, 6.dp)
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                color = if (isPressed) MixinAppTheme.colors.textAssist else MixinAppTheme.colors.textPrimary,
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
        margin = 2.dp,
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
