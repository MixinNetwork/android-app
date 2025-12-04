@file:OptIn(FlowPreview::class)

package one.mixin.android.ui.home.web3.trade

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_SWAP_LAST_PAIR
import one.mixin.android.Constants.Account.PREF_WEB3_SWAP_LAST_PAIR
import one.mixin.android.R
import one.mixin.android.api.request.LimitOrderRequest
import one.mixin.android.api.response.CreateLimitOrderResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.home.web3.components.ExpirySelector
import one.mixin.android.ui.home.web3.components.FloatingActions
import one.mixin.android.ui.home.web3.components.InputArea
import one.mixin.android.ui.home.web3.components.OpenOrderItem
import one.mixin.android.ui.home.web3.components.PriceInputArea
import one.mixin.android.ui.home.web3.components.TradeLayout
import one.mixin.android.ui.tip.wc.compose.Loading
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.handleMixinError
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.route.Order
import one.mixin.android.vo.route.OrderState
import one.mixin.android.web3.js.Web3Signer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

enum class FocusedField { NONE, IN_AMOUNT, OUT_AMOUNT, PRICE }

enum class ExpiryOption(@get:StringRes val labelRes: Int) {
    NEVER(R.string.expiry_never), MIN_10(R.string.expiry_10_min), HOUR_1(R.string.expiry_1_hour), DAY_1(R.string.expiry_1_day), DAY_3(R.string.expiry_3_days), WEEK_1(R.string.expiry_1_week), MONTH_1(R.string.expiry_1_month);

    fun toDuration(): Duration {
        return when (this) {
            NEVER -> Duration.ofDays(365L * 100L)
            MIN_10 -> Duration.ofMinutes(10)
            HOUR_1 -> Duration.ofHours(1)
            DAY_1 -> Duration.ofDays(1)
            DAY_3 -> Duration.ofDays(3)
            WEEK_1 -> Duration.ofDays(7)
            MONTH_1 -> Duration.ofDays(30)
        }
    }
}

@Composable
fun LimitOrderContent(
    from: SwapToken?,
    to: SwapToken?,
    inMixin: Boolean,
    initialAmount: String?,
    lastOrderTime: Long?,
    onSelectToken: (Boolean, SelectTokenType) -> Unit,
    onLimitReview: (SwapToken, SwapToken, CreateLimitOrderResponse) -> Unit,
    onDeposit: (SwapToken) -> Unit,
    onLimitOrderClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel = hiltViewModel<SwapViewModel>()

    var inputText by remember { mutableStateOf(initialAmount ?: "") }
    var outputText by remember { mutableStateOf("") }

    LaunchedEffect(lastOrderTime) {
        inputText = initialAmount ?: ""
        outputText = ""
    }

    var limitPriceText by remember { mutableStateOf("") }
    var marketPriceClickTime by remember { mutableStateOf(lastOrderTime) }
    var priceMultiplier by remember { mutableStateOf<Float?>(null) }

    var isReverse by remember { mutableStateOf(false) }
    val walletId = if (inMixin) Session.getAccountId()!! else Web3Signer.currentWalletId

    var focusedField by remember { mutableStateOf(FocusedField.PRICE) }

    var fromToken by remember(from, to, isReverse) {
        mutableStateOf(if (isReverse) to else from)
    }
    var toToken by remember(from, to, isReverse) {
        mutableStateOf(if (isReverse) from else to)
    }

    var isButtonEnabled by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    var limitOrders by remember { mutableStateOf<List<Order>>(emptyList()) }

    var expiryOption by remember { mutableStateOf(ExpiryOption.NEVER) }

    LaunchedEffect(lastOrderTime) {
        expiryOption = ExpiryOption.NEVER
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(walletId) {
        limitOrders = viewModel.getPendingOrdersFromDb(walletId)
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                viewModel.getLimitOrders(state = OrderState.PENDING.value, offset = null, walletId = walletId, limit = null).data?.let {
                    limitOrders = it
                }
                delay(10000)
            }
        }
    }

    LaunchedEffect(limitPriceText) {
        val fromAmount = inputText.toBigDecimalOrNull()
        val standardPrice = limitPriceText.toBigDecimalOrNull()

        if (fromAmount != null && standardPrice != null && fromAmount > BigDecimal.ZERO && standardPrice > BigDecimal.ZERO) {
            val toAmount = fromAmount.multiply(standardPrice).setScale(8, RoundingMode.DOWN)
            outputText = toAmount.stripTrailingZeros().toPlainString()
        } else {
            outputText = ""
        }
    }

    val rotation by animateFloatAsState(if (isReverse) 180f else 0f, label = "rotation")

    fromToken?.let {
        val fromBalance = viewModel.tokenExtraFlow(it).collectAsStateWithLifecycle(it.balance).value
        KeyboardAwareBox(modifier = Modifier.fillMaxHeight(), content = { availableHeight ->
            Column(
                modifier = if (availableHeight != null) {
                    Modifier
                        .fillMaxWidth()
                        .height(availableHeight)
                } else {
                    Modifier.fillMaxSize()
                }
            ) {

                val scrollState = rememberScrollState()
                Box(
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
                                        isReverse = !isReverse
                                        inputText = outputText

                                        val oldPrice = limitPriceText.toBigDecimalOrNull()
                                        if (oldPrice != null && oldPrice > BigDecimal.ZERO) {
                                            limitPriceText = BigDecimal.ONE.divide(
                                                oldPrice, 8, RoundingMode.HALF_UP
                                            ).stripTrailingZeros().toPlainString()
                                        }

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
                            InputArea(modifier = Modifier.onFocusChanged {
                                if (it.isFocused) {
                                    focusedField = FocusedField.IN_AMOUNT
                                }
                            }, token = fromToken, text = inputText, title = stringResource(id = R.string.swap_send), readOnly = false, selectClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onSelectToken(isReverse, if (isReverse) SelectTokenType.To else SelectTokenType.From)
                            }, onInputChanged = { 
                                inputText = it
                                // When IN_AMOUNT is focused and user inputs, calculate output
                                if (focusedField == FocusedField.IN_AMOUNT) {
                                    val fromAmount = it.toBigDecimalOrNull()
                                    val standardPrice = limitPriceText.toBigDecimalOrNull()
                                    if (fromAmount != null && standardPrice != null && fromAmount > BigDecimal.ZERO && standardPrice > BigDecimal.ZERO) {
                                        val calculatedOutput = fromAmount.multiply(standardPrice).setScale(8, RoundingMode.DOWN)
                                        outputText = calculatedOutput.stripTrailingZeros().toPlainString()
                                    } else if (fromAmount == null || fromAmount == BigDecimal.ZERO) {
                                        outputText = ""
                                    }
                                }
                            }, onDeposit = onDeposit, onMax = {
                                val balance = fromBalance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                if (balance > BigDecimal.ZERO) {
                                    inputText = balance.setScale(8, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
                                } else {
                                    inputText = ""
                                }
                            })
                        },
                        bottomCompose = {
                            InputArea(
                                modifier = Modifier.onFocusChanged {
                                    if (it.isFocused) {
                                        focusedField = FocusedField.OUT_AMOUNT
                                    }
                                },
                                token = toToken,
                                text = outputText,
                                title = stringResource(id = R.string.swap_receive),
                                readOnly = false,
                                selectClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onSelectToken(isReverse, if (isReverse) SelectTokenType.From else SelectTokenType.To)
                                },
                                onInputChanged = { 
                                    outputText = it
                                    // When OUT_AMOUNT is focused and user inputs, calculate input
                                    if (focusedField == FocusedField.OUT_AMOUNT) {
                                        val toAmount = it.toBigDecimalOrNull()
                                        val standardPrice = limitPriceText.toBigDecimalOrNull()
                                        if (toAmount != null && standardPrice != null && toAmount > BigDecimal.ZERO && standardPrice > BigDecimal.ZERO) {
                                            val calculatedInput = toAmount.divide(standardPrice, 8, RoundingMode.DOWN)
                                            inputText = calculatedInput.stripTrailingZeros().toPlainString()
                                        } else if (toAmount == null || toAmount == BigDecimal.ZERO) {
                                            inputText = ""
                                        }
                                    }
                                },
                                onDeposit = null,
                            )
                        },
                        tailCompose = {
                            PriceInputArea(
                                modifier = Modifier.onFocusChanged {
                                    if (it.isFocused) focusedField = FocusedField.PRICE
                                },
                                fromToken = fromToken,
                                toToken = toToken,
                                lastOrderTime = marketPriceClickTime,
                                priceMultiplier = priceMultiplier,
                                onStandardPriceChanged = { limitPriceText = it },
                            )
                        },
                        margin = 6.dp,
                    )
                }

                if (availableHeight != null || inputText.isNotBlank()) {
                    Column(modifier = Modifier
                        .wrapContentHeight()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ExpirySelector(
                            expiryOption = expiryOption,
                            onExpiryChange = { option -> expiryOption = option }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        if (availableHeight == null) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        val checkBalance = checkBalance(inputText, fromBalance)
                        val isInputValid = inputText.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
                        val isPriceValid = limitPriceText.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
                        val isOutputValid = outputText.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
                        val isEnabled = isInputValid && isPriceValid && isOutputValid && checkBalance == true && toToken != null
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                if (isButtonEnabled && toToken != null) {
                                    isButtonEnabled = false
                                    isSubmitting = true
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    scope.launch {
                                        runCatching {
                                            val fromTokenValue = requireNotNull(fromToken)
                                            val toTokenValue = requireNotNull(toToken)
                                            val fromAddress = if (!inMixin) {
                                                viewModel.getAddressesByChainId(Web3Signer.currentWalletId, fromTokenValue.chain.chainId)?.destination
                                            } else null

                                            val toAddress = if (!inMixin) {
                                                viewModel.getAddressesByChainId(Web3Signer.currentWalletId, toTokenValue.chain.chainId)?.destination
                                            } else null

                                            val scaledAmount = inputText.toBigDecimalOrNull()?.setScale(8, RoundingMode.DOWN)?.stripTrailingZeros()?.toPlainString() ?: inputText
                                            val scaledExpected = outputText.toBigDecimalOrNull()?.setScale(8, RoundingMode.DOWN)?.stripTrailingZeros()?.toPlainString() ?: outputText
                                            val request = LimitOrderRequest(
                                                walletId = walletId,
                                                assetId = fromTokenValue.assetId,
                                                amount = scaledAmount,
                                                receiveAssetId = toTokenValue.assetId,
                                                expectedReceiveAmount = scaledExpected,
                                                expiredAt = Instant.now().plus(expiryOption.toDuration()).toString(),
                                                assetDestination = fromAddress,
                                                receiveAssetDestination = toAddress,
                                            )
                                            val response = viewModel.createLimitOrder(request)
                                            if (response.isSuccess) {
                                                onLimitReview.invoke(fromTokenValue, toTokenValue, response.data!!)
                                            } else {
                                                handleMixinError(response.errorCode, response.errorDescription)
                                            }
                                        }.onFailure { e ->
                                            toast(ErrorHandler.getErrorMessage(e))
                                        }
                                        delay(1000)
                                        isSubmitting = false
                                        isButtonEnabled = true
                                    }
                                }
                            },
                            enabled = isEnabled,
                            colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = if (isEnabled) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight,
                            ),
                            shape = RoundedCornerShape(32.dp),
                            elevation = ButtonDefaults.elevation(
                                pressedElevation = 0.dp,
                                defaultElevation = 0.dp,
                                hoveredElevation = 0.dp,
                                focusedElevation = 0.dp,
                            ),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .height(18.dp),
                                        color = if (isEnabled) Color.White else MixinAppTheme.colors.textAssist,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text(
                                        text = if (checkBalance == false) "${fromToken?.symbol} ${stringResource(R.string.insufficient_balance)}" else stringResource(R.string.Review_Order),
                                        color = if (isEnabled) Color.White else MixinAppTheme.colors.textAssist,
                                    )
                                }
                            }
                        }
                    }
                    if (availableHeight != null) {
                        Spacer(modifier = Modifier.height(108.dp))
                    }
                } else {
                    if (limitOrders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                                .padding(16.dp),
                        ) {
                            Text(text = "${stringResource(id = R.string.open_orders)} (${limitOrders.size})", color = MixinAppTheme.colors.textPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            limitOrders.forEach { order ->
                                OpenOrderItem(order = order, onClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onLimitOrderClick(order.orderId)
                                })
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 20.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                                .padding(16.dp),
                        ) {
                            Text(text = "${stringResource(id = R.string.open_orders)} (${limitOrders.size})", color = MixinAppTheme.colors.textPrimary)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .heightIn(min = 120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_empty_file),
                                        contentDescription = null,
                                        tint = MixinAppTheme.colors.iconGray,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(id = R.string.no_order),
                                        color = MixinAppTheme.colors.textAssist,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }, floating = {
            FloatingActions(
                focusedField = focusedField,
                fromBalance = fromBalance,
                fromToken = fromToken,
                toToken = toToken,
                onSetPriceMultiplier = { priceMultiplier = it },
                onSetInput = {
                    inputText = it
                    val fromAmount = it.toBigDecimalOrNull()
                    val standardPrice = limitPriceText.toBigDecimalOrNull()
                    if (fromAmount != null && standardPrice != null && fromAmount > BigDecimal.ZERO && standardPrice > BigDecimal.ZERO) {
                        val calculatedOutput = fromAmount.multiply(standardPrice).setScale(8, RoundingMode.DOWN)
                        outputText = calculatedOutput.stripTrailingZeros().toPlainString()
                    } else if (fromAmount == null || fromAmount == BigDecimal.ZERO) {
                        outputText = ""
                    }
                },
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                onMarketPriceClick = {
                    marketPriceClickTime = System.currentTimeMillis()
                }
            )
        })
    } ?: run {
        Loading()
    }
}

@Composable
fun Modifier.verticalScrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    color: Color = MixinAppTheme.colors.accent.copy(alpha = 0.3f),
): Modifier {
    return drawWithContent {
        drawContent()

        val canScroll = state.maxValue > 0
        if (canScroll) {
            val viewportHeight = this.size.height
            val contentHeight = state.maxValue + viewportHeight
            
            val scrollRatio = viewportHeight / contentHeight
            
            val scrollbarHeight = (scrollRatio * viewportHeight)
                .coerceAtLeast(20.dp.toPx())
                .coerceAtMost(viewportHeight * 0.5f)
            
            val scrollProgress = state.value.toFloat() / state.maxValue
            val scrollbarOffsetY = scrollProgress * (viewportHeight - scrollbarHeight)

            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2),
                topLeft = Offset(this.size.width - width.toPx() - 4.dp.toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight),
            )
        }
    }
}

