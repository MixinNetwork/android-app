@file:OptIn(FlowPreview::class)

package one.mixin.android.ui.home.web3.trade

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import one.mixin.android.api.response.LimitOrder
import one.mixin.android.api.response.LimitOrderCategory
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.toast
import one.mixin.android.extension.putString
import one.mixin.android.ui.home.web3.components.FloatingActions
import one.mixin.android.ui.home.web3.components.InputArea
import one.mixin.android.ui.home.web3.components.OpenOrderItem
import one.mixin.android.ui.home.web3.components.PriceDisplay
import one.mixin.android.ui.home.web3.components.TradeLayout
import one.mixin.android.ui.tip.wc.compose.Loading
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.handleMixinError
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

enum class FocusedField { NONE, AMOUNT, PRICE }

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
    var isSubmitting by remember { mutableStateOf(false) }

    var limitOrders by remember { mutableStateOf<List<LimitOrder>>(emptyList()) }

    var expiryOption by remember { mutableStateOf(ExpiryOption.NEVER) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                viewModel.getLimitOrders(category = LimitOrderCategory.ALL.value, offset = null).data?.let {
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
        KeyboardAwareBox(modifier = Modifier.fillMaxHeight(), content = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
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
                                    val oldInput = inputText
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
                            if (it.isFocused) FocusedField.AMOUNT
                        }, token = fromToken, text = inputText, title = stringResource(id = R.string.swap_send), readOnly = false, selectClick = { onSelectToken(isReverse, if (isReverse) SelectTokenType.To else SelectTokenType.From) }, onInputChanged = { inputText = it }, onDeposit = onDeposit, onMax = {
                            val balance = fromBalance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            if (balance > BigDecimal.ZERO) {
                                inputText = balance.stripTrailingZeros().toPlainString()
                            } else {
                                inputText = ""
                            }
                        })
                    },
                    bottomCompose = {
                        InputArea(
                            modifier = Modifier,
                            token = toToken,
                            text = outputText,
                            title = stringResource(id = R.string.swap_receive),
                            readOnly = true,
                            selectClick = { onSelectToken(isReverse, if (isReverse) SelectTokenType.From else SelectTokenType.To) },
                            onDeposit = null,
                        )
                    },
                    tailCompose = {
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
                    },
                    margin = 6.dp,
                )


                if (inputText.isBlank()) {
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
                                OpenOrderItem(order = order, onClick = { onLimitOrderClick(order.limitOrderId) })
                            }
                        }
                    } else {
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
                } else {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Spacer(modifier = Modifier.height(14.dp))
                        PriceDisplay(
                            fromToken = fromToken, toToken = toToken, limitPrice = limitPriceText, expiryOption = expiryOption, onExpiryChange = { option -> expiryOption = option })

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
                                    isSubmitting = true
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    scope.launch {
                                        runCatching {
                                            val request = LimitOrderRequest(
                                                assetId = requireNotNull(fromToken).assetId,
                                                amount = inputText,
                                                receiveAssetId = requireNotNull(toToken).assetId,
                                                expectedReceiveAmount = outputText,
                                                expiredAt = Instant.now().plus(expiryOption.toDuration()).toString(),
                                            )
                                            val response = viewModel.createLimitOrder(request)
                                            if (response.isSuccess) {
                                                onLimitReview.invoke(response.data!!)
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
                }
            }
        }, floating = {
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            FloatingActions(
                focusedField = focusedField,
                fromBalance = fromBalance,
                fromToken = fromToken,
                toToken = toToken,
                onSetInput = { inputText = it },
                onSetLimitPrice = { limitPriceText = it },
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )
        })
    } ?: run {
        Loading()
    }
}
