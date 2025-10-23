@file:OptIn(FlowPreview::class)

package one.mixin.android.ui.home.web3.swap

import PageScaffold
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.WalletCategory
import java.math.BigDecimal
import java.math.RoundingMode

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import one.mixin.android.api.response.CreateLimitOrderResponse
import one.mixin.android.ui.components.TabItem

@OptIn(ExperimentalFoundationApi::class)
@FlowPreview
@Composable
fun TradePage(
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
    onLimitReview: (CreateLimitOrderResponse) -> Unit,
    onDeposit: (SwapToken) -> Unit,
    onOrderList: () -> Unit,
    pop: () -> Unit,
) {
    val context = LocalContext.current

    val viewModel = hiltViewModel<SwapViewModel>()
    var walletDisplayName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(walletId) {
        if (walletId != null) {
            viewModel.findWeb3WalletById(walletId)?.let {
                if (it.category == WalletCategory.CLASSIC.value ||
                    it.category == WalletCategory.IMPORTED_MNEMONIC.value ||
                    it.category == WalletCategory.IMPORTED_PRIVATE_KEY.value ||
                    it.category == WalletCategory.WATCH_ADDRESS.value) {
                    walletDisplayName = it.name
                }
            }
        }
    }

    val tabs = listOf(
        TabItem(stringResource(id = R.string.Trade_Simple)) {
            SwapContent(
                from = from,
                to = to,
                inMixin = inMixin,
                initialAmount = initialAmount,
                lastOrderTime = lastOrderTime,
                reviewing = reviewing,
                source = source,
                onSelectToken = onSelectToken,
                onReview = onReview,
                onDeposit = onDeposit,
            )
        },
        TabItem(stringResource(id = R.string.Trade_Advanced)) {
            LimitOrderContent(
                from = from,
                to = to,
                inMixin = inMixin,
                reviewing = reviewing,
                onSelectToken = onSelectToken,
                onLimitReview = onLimitReview,
                onDeposit = onDeposit,
            )
        }
    )
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    PageScaffold(
        title = stringResource(id = R.string.Swap),
        subtitle = {
            val text = if (walletId == null) {
                stringResource(id = R.string.Privacy_Wallet)
            } else {
                walletDisplayName ?: stringResource(id = R.string.Common_Wallet)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MixinAppTheme.colors.textAssist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (walletId == null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_wallet_privacy),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(12.dp)
                    )
                }
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
        if (walletId.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                tabs.forEachIndexed { index, tab ->
                    OutlinedTab(
                        text = tab.title,
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                    if (index < tabs.size - 1) {
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                tabs[page].screen()
            }
        } else {
            SwapContent(
                from = from,
                to = to,
                inMixin = inMixin,
                initialAmount = initialAmount,
                lastOrderTime = lastOrderTime,
                reviewing = reviewing,
                source = source,
                onSelectToken = onSelectToken,
                onReview = onReview,
                onDeposit = onDeposit,
            )
        }
    }
}

@Composable
private fun OutlinedTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) MixinAppTheme.colors.background else Color.Transparent
    val borderColor = if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight
    val textColor = if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.textPrimary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = TextStyle(
                fontSize = 14.sp
            )
        )
    }
}

@Composable
fun InputArea(
    modifier: Modifier = Modifier,
    token: SwapToken?,
    text: String,
    title: String,
    readOnly: Boolean,
    selectClick: () -> Unit,
    onInputChanged: ((String) -> Unit)? = null,
    onDeposit: ((SwapToken) -> Unit)? = null,
    onMax: (() -> Unit)? = null,
    showTokenInfo: Boolean = true,
) {
    val viewModel = hiltViewModel<SwapViewModel>()
    val balance = if (token == null) {
        null
    } else {
        viewModel.tokenExtraFlow(token).collectAsStateWithLifecycle(token.balance).value
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
                if (showTokenInfo) {
                    Spacer(modifier = Modifier.weight(1f))
                    token?.let {
                        Text(text = it.chain.name, fontSize = 12.sp, color = MixinAppTheme.colors.textAssist)
                    } ?: run {
                        Text(text = stringResource(id = R.string.select_token), fontSize = 14.sp, color = MixinAppTheme.colors.textMinor)
                    }
                }
            }
        }
        Box(modifier = Modifier.height(10.dp))
        InputContent(token = token, text = text, selectClick = selectClick, onInputChanged = onInputChanged, readOnly = readOnly)
        if (showTokenInfo) {
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
}

@Composable
fun PriceInfo(
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
    val highSlippage = slippageBps > TradeFragment.DangerousSlippage
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
fun InputAction(
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
