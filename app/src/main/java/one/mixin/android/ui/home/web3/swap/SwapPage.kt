package one.mixin.android.ui.home.web3.swap

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.clickVibrate
import one.mixin.android.ui.tip.wc.compose.Loading
import java.math.BigDecimal

@Composable
fun SwapPage(
    isLoading: Boolean,
    fromToken: SwapToken?,
    toToken: SwapToken?,
    inputText: MutableState<String>,
    outputText: String,
    exchangeRate: Float,
    autoSlippage: Boolean,
    slippageBps: Int,
    switch: () -> Unit,
    selectCallback: (Int) -> Unit,
    onInputChanged: (String) -> Unit,
    onShowSlippage: () -> Unit,
    onHalf: () -> Unit,
    onMax: () -> Unit,
    onSwap: () -> Unit,
    pop: () -> Unit,
) {
    SwapPageScaffold(
        title = stringResource(id = R.string.Swap),
        verticalScrollable = true,
        pop = pop,
    ) {
        val context = LocalContext.current
        var isReverse by remember { mutableStateOf(false) }
        val rotation by animateFloatAsState(if (isReverse) 180f else 0f, label = "rotation")

        if (fromToken == null) {
            Loading()
        } else {
            Column {
                SwapLayout(
                    center = {
                        Box(
                            modifier =
                            Modifier
                                .width(40.dp)
                                .height(40.dp)
                                .clip(CircleShape)
                                .border(width = 6.dp, color = MixinAppTheme.colors.background, shape = CircleShape)
                                .background(MixinAppTheme.colors.backgroundGray)
                                .clickable {
                                    isReverse = !isReverse
                                    switch.invoke()
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
                    content = {
                        InputArea(token = fromToken, text = inputText.value, title = stringResource(id = R.string.From), readOnly = false, { selectCallback(0) }, onHalf, onMax) {
                            inputText.value = it
                            onInputChanged.invoke(it)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        InputArea(token = toToken, text = outputText, title = stringResource(id = R.string.To), readOnly = true, { selectCallback(1) })
                    },
                )
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Column(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .alpha(
                                if (exchangeRate == 0f) 0f else 1f,
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(MixinAppTheme.colors.backgroundGray)
                            .padding(20.dp),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(id = R.string.Best_price),
                                maxLines = 1,
                                style =
                                    TextStyle(
                                        fontWeight = FontWeight.W400,
                                        color = MixinAppTheme.colors.textSubtitle,
                                    ),
                            )
                            Text(
                                text = "1 ${fromToken.symbol} ≈ $exchangeRate ${toToken?.symbol}",
                                maxLines = 1,
                                style =
                                    TextStyle(
                                        fontWeight = FontWeight.W400,
                                        color = MixinAppTheme.colors.textPrimary,
                                    ),
                            )
                        }
                        SlippageInfo(autoSlippage, slippageBps, onShowSlippage)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    val checkBalance = checkBalance(inputText.value, fromToken.balance)
                    if (inputText.value.isNotEmpty()) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && checkBalance == true,
                            onClick = {
                                onSwap.invoke()
                            },
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = if (checkBalance != true) MixinAppTheme.colors.backgroundGray else MixinAppTheme.colors.accent,
                                ),
                            shape = RoundedCornerShape(32.dp),
                            contentPadding = PaddingValues(vertical = 16.dp),
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
                                    color = Color.White,
                                )
                            } else {
                                Text(
                                    text = if (checkBalance == false) "${fromToken.symbol} ${stringResource(R.string.insufficient_balance)}" else stringResource(R.string.Review_Order),
                                    color = if (checkBalance != true) MixinAppTheme.colors.textSubtitle else Color.White,
                                )
                            }
                        }
                    }
                }
            }
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
    onHalf: (() -> Unit)? = null,
    onMax: (() -> Unit)? = null,
    onInputChanged: ((String) -> Unit)? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(MixinAppTheme.colors.backgroundGray)
                .padding(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 13.sp, color = MixinAppTheme.colors.textSubtitle)
                Spacer(modifier = Modifier.width(4.dp))
                AsyncImage(
                    model = token?.chain?.chainLogoURI ?: "",
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape),
                    placeholder = painterResource(id = R.drawable.ic_avatar_place_holder),
                )
                Spacer(modifier = Modifier.width(4.dp))
                if (token == null) {
                    Text(text = stringResource(id = R.string.select_token), fontSize = 13.sp, color = MixinAppTheme.colors.textMinor)
                } else {
                    Text(text = token.chain.name, fontSize = 13.sp, color = MixinAppTheme.colors.textSubtitle)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = token?.balance ?: "0",
                    style =
                        TextStyle(
                            color = MixinAppTheme.colors.textMinor,
                            textAlign = TextAlign.End,
                        ),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_web3_wallet),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
                if (!readOnly) {
                    Spacer(modifier = Modifier.width(10.dp))
                    InputAction(text = stringResource(id = R.string.balance_half)) {
                        onHalf?.invoke()
                    }
                    Spacer(modifier = Modifier.width(6.dp))
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
fun SwapPageScaffold(
    title: String,
    verticalScrollable: Boolean = true,
    pop: () -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        backgroundColor = MixinAppTheme.colors.background,
        topBar = {
            MixinTopAppBar(
                title = {
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = { pop() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.icon,
                        )
                    }
                },
            )
        },
    ) {
        Column(
            Modifier
                .padding(it)
                .apply {
                    if (verticalScrollable) {
                        verticalScroll(rememberScrollState())
                    }
                },
        ) {
            body()
        }
    }
}

@Composable
private fun SlippageInfo(
    autoSlippage: Boolean,
    slippageBps: Int,
    onShowSlippage: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val descText =
        if (autoSlippage) {
            "${LocalContext.current.getString(R.string.slippage_auto)} (${slippageBps.slippageBpsDisplay()}%)"
        } else {
            "${slippageBps.slippageBpsDisplay()}%"
        }
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
                    color = MixinAppTheme.colors.textSubtitle,
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
                tint = MixinAppTheme.colors.textSubtitle,
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
                .border(1.dp, color = if (isPressed) MixinAppTheme.colors.accent else MixinAppTheme.colors.textMinor, RoundedCornerShape(12.dp))
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
                    fontWeight = FontWeight.Bold,
                    color = if (isPressed) MixinAppTheme.colors.accent else MixinAppTheme.colors.textMinor,
                ),
        )
    }
}

@Composable
fun SwapLayout(
    content: @Composable ColumnScope.() -> Unit,
    center: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .wrapContentHeight()
                .wrapContentWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            content()
        }
        center()
    }
}

@Preview
@Composable
fun PreviewSlippageInfo() {
    SlippageInfo(autoSlippage = true, slippageBps = 50) {}
}

@Preview
@Composable
fun PreviewSlippageInfoWarning() {
    SlippageInfo(autoSlippage = true, slippageBps = 600) {}
}

@Preview
@Composable
fun PreviewInputActionMax() {
    InputAction("MAX") {}
}

@Preview
@Composable
fun PreviewInputActionHalf() {
    InputAction("HALF") {}
}

/*
 * @return True if the input was successful, false if the balance is insufficient, or null if the input is invalid.
 */
private fun checkBalance(
    inputText: String,
    balance: String?,
): Boolean? {
    if (balance.isNullOrEmpty()) return false
    val inputValue =
        try {
            BigDecimal(inputText)
        } catch (e: Exception) {
            null
        } ?: return null
    if (inputValue <= BigDecimal.ZERO) return null
    val balanceValue =
        try {
            BigDecimal(balance)
        } catch (e: Exception) {
            null
        } ?: return null
    return inputValue <= balanceValue
}
