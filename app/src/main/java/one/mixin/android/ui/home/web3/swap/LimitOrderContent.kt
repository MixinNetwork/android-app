@file:OptIn(FlowPreview::class)

package one.mixin.android.ui.home.web3.swap

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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_SWAP_LAST_PAIR
import one.mixin.android.Constants.Account.PREF_WEB3_SWAP_LAST_PAIR
import one.mixin.android.R
import one.mixin.android.api.request.LimitOrderRequest
import one.mixin.android.api.response.CreateLimitOrderResponse
import one.mixin.android.api.response.web3.QuoteResult
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
import java.util.UUID

@Composable
fun LimitOrderContent(
    from: SwapToken?,
    to: SwapToken?,
    inMixin: Boolean,
    reviewing: Boolean,
    onSelectToken: (Boolean, SelectTokenType) -> Unit,
    onLimitReview: (CreateLimitOrderResponse) -> Unit,
    onDeposit: (SwapToken) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel = hiltViewModel<SwapViewModel>()

    var inputText by remember { mutableStateOf("") }
    var limitPriceText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }

    var isReverse by remember { mutableStateOf(false) }

    var fromToken by remember(from, to, isReverse) {
        mutableStateOf(if (isReverse) to else from)
    }
    var toToken by remember(from, to, isReverse) {
        mutableStateOf(if (isReverse) from else to)
    }

    var isButtonEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(inputText, limitPriceText, fromToken, toToken) {
        val fromAmount = inputText.toBigDecimalOrNull()
        val limitPrice = limitPriceText.toBigDecimalOrNull()
        val toTokenDecimals = toToken?.decimals ?: 8

        if (fromAmount != null && limitPrice != null && fromAmount > BigDecimal.ZERO && limitPrice > BigDecimal.ZERO) {
            val toAmount = fromAmount.multiply(limitPrice).setScale(toTokenDecimals, RoundingMode.DOWN)
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
                                            limitPriceText = BigDecimal.ONE.divide(oldPrice, 8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
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
                                }
                            )
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
                        margin = 6.dp,
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        InputArea(
                            token = null,
                            text = limitPriceText,
                            title = stringResource(id = R.string.limit_price, toToken?.symbol ?: "", fromToken?.symbol ?: ""),
                            readOnly = false,
                            selectClick = {},
                            onInputChanged = { limitPriceText = it },
                            showTokenInfo = false,
                        )
                    }

                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val keyboardController = LocalSoftwareKeyboardController.current
                        val focusManager = LocalFocusManager.current
                        val checkBalance = checkBalance(inputText, fromBalance)
                        val isInputValid = inputText.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
                        val isPriceValid = limitPriceText.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
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
                            enabled = isInputValid && isPriceValid && checkBalance == true && toToken != null,
                            colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = if (isInputValid && isPriceValid && checkBalance == true) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight,
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
                                color = if (checkBalance != true || !isInputValid || !isPriceValid) MixinAppTheme.colors.textAssist else Color.White,
                            )
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
            })
    } ?: run {
        Loading()
    }
}
