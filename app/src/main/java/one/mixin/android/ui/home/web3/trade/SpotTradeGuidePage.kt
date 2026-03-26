package one.mixin.android.ui.home.web3.trade

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.home.web3.components.OutlinedTab
import one.mixin.android.ui.home.web3.trade.perps.GuideNavigationButton
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.components.DotText
import java.math.BigDecimal
import java.math.RoundingMode

private val LIMIT_PRICE_STEP = BigDecimal("1000")
private val LIMIT_PRICE_TEN_THOUSAND = BigDecimal("10000")

private enum class LimitStrategy(
    val titleRes: Int,
) {
    BuyLow(
        titleRes = R.string.Spot_Trade_Guide_Limit_Strategy_Buy_Low,
    ),
    SellHigh(
        titleRes = R.string.Spot_Trade_Guide_Limit_Strategy_Sell_High,
    ),
}

@Composable
fun SpotTradeGuidePage(
    initialTab: Int = SpotTradeGuideBottomSheetDialogFragment.TAB_OVERVIEW,
    pop: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf(
        stringResource(R.string.Brief_Introduction),
        stringResource(R.string.Trade_Simple),
        stringResource(R.string.Trade_Advanced),
    )
    val safeInitialTab = initialTab.coerceIn(0, tabs.lastIndex)
    var selectedTab by remember(safeInitialTab) { mutableIntStateOf(safeInitialTab) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(MixinAppTheme.colors.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.Spot_Trading_Guide),
                fontSize = 18.sp,
                fontWeight = FontWeight.W500,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_circle_close),
                contentDescription = stringResource(id = R.string.close),
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(onClick = pop),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                tabs.forEachIndexed { index, tab ->
                    OutlinedTab(
                        text = tab,
                        selected = selectedTab == index,
                        showBadge = false,
                        onClick = { coroutineScope.launch { selectedTab = index } }
                    )
                    if (index < tabs.lastIndex) {
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    SpotTradeGuideBottomSheetDialogFragment.TAB_OVERVIEW -> OverviewContent()
                    SpotTradeGuideBottomSheetDialogFragment.TAB_SWAP -> SimpleSwapContent()
                    SpotTradeGuideBottomSheetDialogFragment.TAB_LIMIT -> LimitTradeContent()
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
            SpotTradeGuideBottomNavigation(
                selectedTab = selectedTab,
                tabs = tabs,
                onSelect = { targetTab ->
                    coroutineScope.launch { selectedTab = targetTab }
                },
                onClose = pop,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OverviewContent() {
    TradeGuideInfoCard(
        title = stringResource(R.string.Overview),
        description = stringResource(R.string.Spot_Trade_Guide_Overview_Desc),
        sections = listOf(
            stringResource(R.string.Product_Features) to listOf(
                stringResource(R.string.Spot_Trade_Guide_Feature_1),
                stringResource(R.string.Spot_Trade_Guide_Feature_2),
            ),
            stringResource(R.string.Spot_Trade_Guide_Fees) to listOf(
                stringResource(R.string.Spot_Trade_Guide_Note_1),
            ),
        )
    )
}

@Composable
private fun SimpleSwapContent() {
    SpotTradeExampleCard(limitStrategy = null)
    Spacer(modifier = Modifier.height(16.dp))
    TradeGuideInfoCard(
        title = stringResource(R.string.Overview),
        description = stringResource(R.string.Spot_Trade_Guide_Swap_Desc),
        sections = listOf(
            stringResource(R.string.Spot_Trade_Guide_Use_Cases) to listOf(
                stringResource(R.string.Spot_Trade_Guide_Swap_Scenario_1),
                stringResource(R.string.Spot_Trade_Guide_Swap_Scenario_2),
                stringResource(R.string.Spot_Trade_Guide_Swap_Scenario_3),
            ),
            stringResource(R.string.Spot_Trade_Guide_Pricing) to listOf(
                stringResource(R.string.Spot_Trade_Guide_Swap_Quote_1),
                stringResource(R.string.Spot_Trade_Guide_Swap_Quote_2),
            ),
            stringResource(R.string.Risk_Notice) to listOf(
                stringResource(R.string.Spot_Trade_Guide_Swap_Risk),
            ),
        )
    )
}

@Composable
private fun LimitTradeContent() {
    SpotTradeExampleCard(limitStrategy = LimitStrategy.BuyLow)
    Spacer(modifier = Modifier.height(16.dp))
    TradeGuideInfoCard(
        title = stringResource(R.string.Overview),
        description = stringResource(R.string.Spot_Trade_Guide_Limit_Desc),
        sections = listOf(
            stringResource(R.string.Spot_Trade_Guide_Use_Cases) to listOf(
                stringResource(R.string.Spot_Trade_Guide_Limit_Scenario_1),
                stringResource(R.string.Spot_Trade_Guide_Limit_Scenario_2),
                stringResource(R.string.Spot_Trade_Guide_Limit_Scenario_3),
            ),
            stringResource(R.string.Risk_Notice) to listOf(
                stringResource(R.string.Spot_Trade_Guide_Limit_Risk),
            ),
        )
    )
}

@Composable
private fun SpotTradeExampleCard(
    limitStrategy: LimitStrategy?,
) {
    val viewModel = hiltViewModel<SwapViewModel>()
    val usdtToken by viewModel.assetItemFlow(Constants.AssetId.USDT_ASSET_ETH_ID).collectAsStateWithLifecycle(initialValue = null)
    val btcToken by viewModel.assetItemFlow(Constants.ChainId.BITCOIN_CHAIN_ID).collectAsStateWithLifecycle(initialValue = null)
    var priceRefreshFlag by remember { mutableStateOf(false) }
    val marketPrice = remember(usdtToken?.priceUsd, btcToken?.priceUsd, priceRefreshFlag) {
        calculateMarketPrice(usdtToken, btcToken)
    }
    var isPriceDisplayReversed by remember(limitStrategy) { mutableStateOf(false) }
    var strategy by remember(limitStrategy) { mutableStateOf(limitStrategy ?: LimitStrategy.BuyLow) }
    var payAmount by remember(limitStrategy, strategy) {
        mutableStateOf(defaultPayAmount(limitStrategy, strategy))
    }
    var limitPriceOffset by remember(limitStrategy, strategy) { mutableStateOf(BigDecimal.ZERO) }

    val isPairReversed = limitStrategy != null && strategy == LimitStrategy.SellHigh
    val fromToken = if (isPairReversed) btcToken else usdtToken
    val toToken = if (isPairReversed) usdtToken else btcToken
    val amountStep = remember(limitStrategy, strategy, isPairReversed) {
        if (limitStrategy != null) {
            when (strategy) {
                LimitStrategy.BuyLow -> BigDecimal("100")
                LimitStrategy.SellHigh -> BigDecimal.ONE
            }
        } else if (isPairReversed) {
            BigDecimal("0.001")
        } else {
            BigDecimal("100")
        }
    }
    val limitBasePrice = remember(marketPrice, strategy) {
        calculateLimitBasePrice(
            marketPrice = marketPrice,
            strategy = strategy,
        )
    }
    val effectivePrice = if (limitStrategy == null) {
        marketPrice
    } else {
        limitBasePrice.add(limitPriceOffset).max(LIMIT_PRICE_STEP)
    }
    val estimatedReceive = remember(payAmount, effectivePrice, isPairReversed) {
        calculateReceiveAmount(
            amount = payAmount,
            price = effectivePrice,
            reversed = isPairReversed,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.Perpetual_Example),
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            color = MixinAppTheme.colors.textPrimary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (limitStrategy != null) {
            StrategyRow(
                strategy = strategy,
                onStrategySelected = { strategy = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        ExampleValueRow(
            title = stringResource(R.string.Trade_Guide_Trading_Pair),
            value = {
                PairDisplay(
                    fromToken = fromToken,
                    toToken = toToken,
                    fromFallbackSymbol = if (isPairReversed) "BTC" else "USDT",
                    toFallbackSymbol = if (isPairReversed) "USDT" else "BTC",
                )
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
        ExampleValueRow(
            title = stringResource(R.string.Trade_Guide_Pay_Amount),
            value = {
                AmountStepper(
                    amount = payAmount,
                    symbol = fromToken?.symbol ?: if (isPairReversed) "BTC" else "USDT",
                    step = amountStep,
                    onDecrease = {
                        payAmount = (payAmount - amountStep).max(amountStep)
                    },
                    onIncrease = {
                        payAmount += amountStep
                    },
                )
            },
        )
        if (limitStrategy != null) {
            Spacer(modifier = Modifier.height(16.dp))
            ExampleValueRow(
                title = stringResource(R.string.Trade_Guide_Limit_Price),
                value = {
                    OrderPriceStepper(
                        price = effectivePrice,
                        symbol = usdtToken?.symbol ?: "USDT",
                        onDecrease = {
                            limitPriceOffset = (limitPriceOffset - LIMIT_PRICE_STEP)
                                .max(LIMIT_PRICE_STEP.subtract(limitBasePrice))
                        },
                        onIncrease = {
                            limitPriceOffset += LIMIT_PRICE_STEP
                        },
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.Trade_Guide_Market_Price),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MixinAppTheme.colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(16.dp))
        if (limitStrategy == null) {
            PriceSubtitle(
                marketPrice = effectivePrice,
                isReversed = isPriceDisplayReversed,
                onSwitchDirection = { isPriceDisplayReversed = !isPriceDisplayReversed },
                onPriceExpired = { priceRefreshFlag = !priceRefreshFlag },
            )
        } else {
            PriceSubtitle(
                marketPrice = marketPrice,
                isReversed = isPriceDisplayReversed,
                onSwitchDirection = { isPriceDisplayReversed = !isPriceDisplayReversed },
                onPriceExpired = { priceRefreshFlag = !priceRefreshFlag },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        ExampleValueRow(
            title = stringResource(R.string.Spot_Trade_Guide_You_Receive),
            value = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(id = guideTokenIconRes(toToken, if (isPairReversed) "USDT" else "BTC")),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape),
                    )
                    Text(
                        text = "${estimatedReceive.numberFormat8()} ${toToken?.symbol ?: if (isPairReversed) "USDT" else "BTC"}",
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MixinAppTheme.colors.textPrimary,
                        fontWeight = FontWeight.W500,
                        textAlign = TextAlign.End,
                    )
                }
            },
        )
    }
}

@Composable
private fun StrategyRow(
    strategy: LimitStrategy,
    onStrategySelected: (LimitStrategy) -> Unit,
) {
    ExampleValueRow(
        title = stringResource(R.string.Trade_Guide_Strategy),
        value = {
            Row(modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MixinAppTheme.colors.backgroundWindow)
                .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LimitStrategy.entries.forEach { item ->
                    val selected = item == strategy
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (selected) MixinAppTheme.colors.accent
                                else Color.Transparent
                            )
                            .clickable { onStrategySelected(item) }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(item.titleRes),
                            color = if (selected) Color.White else MixinAppTheme.colors.textAssist,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun ExampleValueRow(
    title: String,
    subtitle: (@Composable () -> Unit)? = null,
    value: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MixinAppTheme.colors.textAssist,
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                it()
            }
        }
        Box(contentAlignment = Alignment.CenterEnd) {
            value()
        }
    }
}

@Composable
private fun PairDisplay(
    fromToken: TokenItem?,
    toToken: TokenItem?,
    fromFallbackSymbol: String,
    toFallbackSymbol: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GuideTokenBadge(token = fromToken, fallbackSymbol = fromFallbackSymbol)
        Text(
            text = "->",
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist,
        )
        GuideTokenBadge(token = toToken, fallbackSymbol = toFallbackSymbol)
    }
}

@Composable
private fun GuideTokenBadge(
    token: TokenItem?,
    fallbackSymbol: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(id = guideTokenIconRes(token, fallbackSymbol)),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape),
        )
        Text(
            text = token?.symbol ?: fallbackSymbol,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MixinAppTheme.colors.textPrimary,
            fontWeight = FontWeight.W500,
        )
    }
}

@Composable
private fun AmountStepper(
    amount: BigDecimal,
    symbol: String,
    step: BigDecimal,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_perps_minus),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(16.dp)
                .clickable(enabled = amount > step, onClick = onDecrease),
        )
        Text(
            text = "${amount.numberFormat8()} $symbol",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MixinAppTheme.colors.textPrimary,
            fontWeight = FontWeight.W500,
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_perps_add),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = onIncrease),
        )
    }
}

@Composable
private fun OrderPriceStepper(
    price: BigDecimal,
    symbol: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_perps_minus),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(16.dp)
                .clickable(enabled = price > LIMIT_PRICE_STEP, onClick = onDecrease),
        )
        Text(
            text = "${price.setScale(0, RoundingMode.DOWN).numberFormat8()} $symbol",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MixinAppTheme.colors.textPrimary,
            fontWeight = FontWeight.W500,
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_perps_add),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = onIncrease),
        )
    }
}

@Composable
private fun PriceSubtitle(
    marketPrice: BigDecimal,
    isReversed: Boolean,
    onSwitchDirection: () -> Unit,
    onPriceExpired: () -> Unit = {},
) {
    var quoteCountDown by remember(marketPrice) { mutableFloatStateOf(0f) }

    LaunchedEffect(marketPrice) {
        while (isActive) {
            quoteCountDown = 0f
            while (isActive && quoteCountDown < 1f) {
                delay(100)
                quoteCountDown += 0.01f
            }
            onPriceExpired()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = if (isReversed) {
                val inverted = safeDivide(BigDecimal.ONE, marketPrice)
                "1 USDT ≈ ${inverted.numberFormat8()} BTC"
            } else {
                "1 BTC ≈ ${marketPrice.priceFormat()} USDT"
            },
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist,
        )
        CircularProgressIndicator(
            progress = quoteCountDown,
            modifier = Modifier.size(12.dp),
            strokeWidth = 2.dp,
            color = MixinAppTheme.colors.textPrimary,
            backgroundColor = MixinAppTheme.colors.textAssist,
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = R.drawable.ic_price_switch),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSwitchDirection,
                ),
        )
    }
}

@Composable
private fun TradeGuideInfoCard(
    title: String,
    description: String,
    sections: List<Pair<String, List<String>>>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            color = MixinAppTheme.colors.textPrimary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MixinAppTheme.colors.textPrimary,
        )
        sections.forEach { (sectionTitle, items) ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = sectionTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                color = MixinAppTheme.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach { item ->
                DotText(
                    text = item,
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MixinAppTheme.colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun SpotTradeGuideBottomNavigation(
    selectedTab: Int,
    tabs: List<String>,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val previousTab = (selectedTab - 1).takeIf { it >= 0 }
    val nextTab = (selectedTab + 1).takeIf { it < tabs.size }
    if (previousTab == null && nextTab == null) {
        return
    }
    if (previousTab != null && nextTab == null) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GuideNavigationButton(
                text = tabs[previousTab],
                isPrevious = true,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(previousTab) },
            )
            GuideNavigationButton(
                text = stringResource(R.string.Start),
                isPrevious = false,
                modifier = Modifier.weight(1f),
                onClick = onClose,
            )
        }
        return
    }
    if (previousTab != null && nextTab != null) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GuideNavigationButton(
                text = tabs[previousTab],
                isPrevious = true,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(previousTab) },
            )
            GuideNavigationButton(
                text = tabs[nextTab],
                isPrevious = false,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(nextTab) },
            )
        }
        return
    }
    val targetIndex = previousTab ?: nextTab ?: return
    val buttonText = tabs[targetIndex]
    val isPrevious = previousTab != null
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        GuideNavigationButton(
            text = buttonText,
            isPrevious = isPrevious,
            modifier = Modifier.fillMaxWidth(0.5f),
            onClick = { onSelect(targetIndex) },
        )
    }
}

private fun calculateMarketPrice(
    usdtToken: TokenItem?,
    btcToken: TokenItem?,
): BigDecimal {
    val usdtPrice = usdtToken?.priceUsd?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE
    val btcPrice = btcToken?.priceUsd?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: BigDecimal("95594.89")
    return safeDivide(btcPrice, usdtPrice)
}

private fun calculateLimitBasePrice(
    marketPrice: BigDecimal,
    strategy: LimitStrategy,
): BigDecimal {
    val integerPrice = marketPrice
        .max(BigDecimal.ZERO)
        .setScale(0, RoundingMode.DOWN)
    val tenThousandUnits = integerPrice.divideToIntegralValue(LIMIT_PRICE_TEN_THOUSAND)
    val basePrice = when (strategy) {
        LimitStrategy.BuyLow -> tenThousandUnits.multiply(LIMIT_PRICE_TEN_THOUSAND)
        LimitStrategy.SellHigh -> {
            if (integerPrice.remainder(LIMIT_PRICE_TEN_THOUSAND).compareTo(BigDecimal.ZERO) == 0) {
                integerPrice
            } else {
                tenThousandUnits.add(BigDecimal.ONE).multiply(LIMIT_PRICE_TEN_THOUSAND)
            }
        }
    }
    return basePrice.max(LIMIT_PRICE_STEP)
}

private fun defaultPayAmount(
    limitStrategy: LimitStrategy?,
    strategy: LimitStrategy,
): BigDecimal {
    if (limitStrategy == null) {
        return BigDecimal("1000")
    }
    return when (strategy) {
        LimitStrategy.BuyLow -> BigDecimal("1000")
        LimitStrategy.SellHigh -> BigDecimal.ONE
    }
}

private fun calculateReceiveAmount(
    amount: BigDecimal,
    price: BigDecimal,
    reversed: Boolean,
): BigDecimal {
    return if (reversed) {
        amount.multiply(price).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
    } else {
        safeDivide(amount, price).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
    }
}

private fun guideTokenIconRes(
    token: TokenItem?,
    fallbackSymbol: String,
): Int {
    return when ((token?.symbol ?: fallbackSymbol).uppercase()) {
        "USDT" -> R.drawable.ic_token_usdt
        "BTC" -> R.drawable.ic_chain_btc
        else -> R.drawable.ic_avatar_place_holder
    }
}

private fun safeDivide(
    dividend: BigDecimal,
    divisor: BigDecimal,
): BigDecimal {
    if (divisor.compareTo(BigDecimal.ZERO) == 0) {
        return BigDecimal.ZERO
    }
    return dividend.divide(divisor, 8, RoundingMode.HALF_UP).stripTrailingZeros()
}
