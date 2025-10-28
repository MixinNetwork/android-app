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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
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
import one.mixin.android.ui.tip.wc.compose.Loading
import one.mixin.android.util.GsonHelper
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Divider
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import one.mixin.android.api.response.LimitOrder
import one.mixin.android.api.response.LimitOrderCategory
import one.mixin.android.api.response.LimitOrderStatus
import one.mixin.android.compose.CoilImage
import one.mixin.android.extension.fullDate
import one.mixin.android.ui.wallet.alert.components.cardBackground

enum class FocusedField { NONE, AMOUNT, PRICE }

@Composable
fun LimitOrderContent(
    from: SwapToken?,
    to: SwapToken?,
    inMixin: Boolean,
    reviewing: Boolean,
    onSelectToken: (Boolean, SelectTokenType) -> Unit,
    onLimitReview: (CreateLimitOrderResponse) -> Unit,
    onDeposit: (SwapToken) -> Unit,
    onLimitOrderClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel = hiltViewModel<SwapViewModel>()

    var inputText by remember { mutableStateOf("") }
    var limitPriceText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }

    var isReverse by remember { mutableStateOf(false) }

    var focusedField by remember { mutableStateOf(FocusedField.AMOUNT) }

    var fromToken by remember(from, to, isReverse) {
        mutableStateOf(if (isReverse) to else from)
    }
    var toToken by remember(from, to, isReverse) {
        mutableStateOf(if (isReverse) from else to)
    }

    var isButtonEnabled by remember { mutableStateOf(true) }

    var limitOrders by remember { mutableStateOf<List<LimitOrder>>(emptyList()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                viewModel.getLimitOrders(category = LimitOrderCategory.ACTIVE.value, offset = null).data?.let {
                    limitOrders = it
                }
                delay(10000)
            }
        }
    }

    LaunchedEffect(fromToken, toToken) {
        val fromPrice = fromToken?.price?.toBigDecimalOrNull()
        val toPrice = toToken?.price?.toBigDecimalOrNull()

        if (fromPrice != null && toPrice != null && toPrice > BigDecimal.ZERO) {
            val price = fromPrice.divide(toPrice, 8, RoundingMode.HALF_UP)
            limitPriceText = price.stripTrailingZeros().toPlainString()
        } else {
            limitPriceText = ""
        }
    }

    LaunchedEffect(inputText, limitPriceText, fromToken, toToken) {
        val fromAmount = inputText.toBigDecimalOrNull()
        val limitPrice = limitPriceText.toBigDecimalOrNull()

        if (fromAmount != null && limitPrice != null && fromAmount > BigDecimal.ZERO && limitPrice > BigDecimal.ZERO) {
            val toAmount = fromAmount.multiply(limitPrice).setScale(8, RoundingMode.DOWN)
            outputText = toAmount.stripTrailingZeros().toPlainString()
        } else {
            outputText = ""
        }
    }

    val rotation by animateFloatAsState(if (isReverse) 180f else 0f, label = "rotation")

    fromToken?.let {
        val fromBalance = viewModel.tokenExtraFlow(it).collectAsStateWithLifecycle(it.balance).value
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
                                        isReverse = !isReverse
                                        val oldInput = inputText
                                        inputText = outputText

                                        val oldPrice = limitPriceText.toBigDecimalOrNull()
                                        if (oldPrice != null && oldPrice > BigDecimal.ZERO) {
                                            limitPriceText =
                                                BigDecimal.ONE
                                                    .divide(
                                                        oldPrice,
                                                        8,
                                                        RoundingMode.HALF_UP
                                                    )
                                                    .stripTrailingZeros().toPlainString()
                                        }

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
                                modifier = Modifier.onFocusChanged {
                                    if (it.isFocused) focusedField = FocusedField.AMOUNT
                                },
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
                                }
                            )
                        },
                        bottomCompose = {
                            InputArea(
                                modifier = Modifier,
                                token = toToken,
                                text = outputText,
                                title = stringResource(id = R.string.Price),
                                readOnly = true,
                                selectClick = { onSelectToken(isReverse, if (isReverse) SelectTokenType.From else SelectTokenType.To) },
                                onDeposit = null,
                            )
                        },
                        margin = 6.dp,
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        InputArea(
                            modifier = Modifier.onFocusChanged {
                                if (it.isFocused) focusedField = FocusedField.PRICE
                            },
                            token = toToken,
                            text = limitPriceText,
                            title = stringResource(id = R.string.limit_price, toToken?.symbol ?: "", fromToken?.symbol ?: ""),
                            readOnly = false,
                            selectClick = null,
                            onInputChanged = { limitPriceText = it },
                            showTokenInfo = false,
                        )
                    }

                    if (inputText.isBlank()) {
                        if (limitOrders.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                                    .padding(16.dp),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.open_orders),
                                    color = MixinAppTheme.colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                limitOrders.forEach { order ->
                                    OpenOrderItem(order = order, onClick = { onLimitOrderClick(order.limitOrderId) })
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(24.dp))
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                                    .padding(16.dp),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.open_orders),
                                    color = MixinAppTheme.colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_empty_file),
                                    contentDescription = null,
                                    tint = MixinAppTheme.colors.iconGray,
                                    modifier = Modifier
                                        .padding(vertical = 40.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(id = R.string.no_order),
                                    color = MixinAppTheme.colors.textAssist,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Spacer(modifier = Modifier.height(14.dp))
                            PriceDisplay(fromToken, toToken, limitPriceText)

                            Spacer(modifier = Modifier.height(14.dp))
                            val keyboardController = LocalSoftwareKeyboardController.current
                            val focusManager = LocalFocusManager.current
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
                                    if (isButtonEnabled && toToken != null) {
                                        isButtonEnabled = false
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        scope.launch {
                                            runCatching {
                                                val request = LimitOrderRequest(
                                                    assetId = requireNotNull(fromToken).assetId,
                                                    amount = inputText,
                                                    receiveAssetId = requireNotNull(toToken).assetId,
                                                    expectedReceiveAmount = outputText,
                                                    expiredAt = Instant.now().plus(Duration.ofDays(7)).toString(),
                                                )
                                                viewModel.createLimitOrder(request).data?.let {
                                                    onLimitReview.invoke(it)
                                                }
                                            }
                                            delay(1000)
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
                                Text(
                                    text = if (checkBalance == false) "${fromToken?.symbol} ${stringResource(R.string.insufficient_balance)}" else stringResource(R.string.Review_Order),
                                    color = if (isEnabled) Color.White else MixinAppTheme.colors.textAssist,
                                )
                            }
                        }
                    }
                }
            },
            floating = {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current

                when (focusedField) {
                    FocusedField.AMOUNT -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MixinAppTheme.colors.backgroundWindow)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
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
                    }

                    FocusedField.PRICE -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MixinAppTheme.colors.backgroundWindow)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val marketPrice = remember(fromToken, toToken) {
                                val fromPrice = fromToken?.price?.toBigDecimalOrNull()
                                val toPrice = toToken?.price?.toBigDecimalOrNull()
                                if (fromPrice != null && toPrice != null && toPrice > BigDecimal.ZERO) {
                                    fromPrice.divide(toPrice, 8, RoundingMode.HALF_UP)
                                } else {
                                    null
                                }
                            }

                            InputAction(stringResource(R.string.market_price), showBorder = true) {
                                marketPrice?.let {
                                    limitPriceText = it.stripTrailingZeros().toPlainString()
                                }
                            }
                            InputAction("+10%", showBorder = true) {
                                marketPrice?.let {
                                    val newPrice = it.multiply(BigDecimal("1.1"))
                                    limitPriceText = newPrice.stripTrailingZeros().toPlainString()
                                }
                            }
                            InputAction("+20%", showBorder = true) {
                                marketPrice?.let {
                                    val newPrice = it.multiply(BigDecimal("1.2"))
                                    limitPriceText = newPrice.stripTrailingZeros().toPlainString()
                                }
                            }
                            InputAction(stringResource(R.string.Done), showBorder = false) {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        }
                    }

                    else -> {}
                }
            })
    } ?: run {
        Loading()
    }
}

@Composable
private fun PriceDisplay(
    fromToken: SwapToken?,
    toToken: SwapToken?,
    limitPrice: String,
) {
    var isPriceInverted by remember { mutableStateOf(false) }
    val price = limitPrice.toBigDecimalOrNull()

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val priceText = if (price != null && price > BigDecimal.ZERO) {
                if (!isPriceInverted) {
                    "1 ${fromToken?.symbol} ≈ ${price.stripTrailingZeros().toPlainString()} ${toToken?.symbol}"
                } else {
                    val invertedPrice = BigDecimal.ONE.divide(price, 8, RoundingMode.HALF_UP)
                    "1 ${toToken?.symbol} ≈ ${invertedPrice.stripTrailingZeros().toPlainString()} ${fromToken?.symbol}"
                }
            } else {
                "..."
            }
            Text(
                text = priceText,
                color = MixinAppTheme.colors.textAssist,
                fontSize = 16.sp
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_price_switch),
                contentDescription = "Switch price",
                tint = MixinAppTheme.colors.iconGray,
                modifier = Modifier.clickable { isPriceInverted = !isPriceInverted }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.expiry),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.forever),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_type_down),
                    contentDescription = "Select expiry",
                    tint = MixinAppTheme.colors.iconGray
                )
            }
        }
    }
}

@Composable
private fun OpenOrderItem(order: LimitOrder, onClick: () -> Unit) {
    val viewModel = hiltViewModel<SwapViewModel>()
    val fromToken by viewModel.assetItemFlow(order.assetId).collectAsStateWithLifecycle(null)
    val toToken by viewModel.assetItemFlow(order.receiveAssetId).collectAsStateWithLifecycle(null)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.wrapContentSize()) {
            CoilImage(
                fromToken?.iconUrl,
                modifier = Modifier
                    .width(30.dp)
                    .height(30.dp),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
            CoilImage(
                toToken?.iconUrl,
                modifier = Modifier
                    .offset(x = 10.dp, y = 10.dp)
                    .width(34.dp)
                    .height(34.dp)
                    .clip(CircleShape)
                    .border(2.dp, MixinAppTheme.colors.background, CircleShape),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
        }

        Spacer(modifier = Modifier.width(22.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${fromToken?.symbol ?: "..."} → ${toToken?.symbol ?: "..."}",
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = order.createdAt.fullDate(),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                    textAlign = TextAlign.End
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "-${order.amount} ${fromToken?.symbol ?: "..."}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.walletRed,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.limit_price),
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    color = MixinAppTheme.colors.textAssist,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+${order.expectedReceiveAmount} ${toToken?.symbol ?: "..."}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.walletGreen,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = order.state.value.replaceFirstChar { it.uppercase() },
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    color = when (order.state) {
                        LimitOrderStatus.CREATED, LimitOrderStatus.PRICING, LimitOrderStatus.QUOTING -> MixinAppTheme.colors.textAssist
                        LimitOrderStatus.SETTLED -> MixinAppTheme.colors.green
                        else -> MixinAppTheme.colors.red
                    }
                )
            }
        }
    }
}
