package one.mixin.android.ui.home.web3.trade.perps

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.home.web3.components.OutlinedTab
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.widget.components.DotText
import one.mixin.android.widget.components.MixinButton
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

data class ScenarioData(
    val scenario: String,
    val change: String,
    val initialPercent: Float = 10f,
    val basePnlAmount: Int,
    val basePnlPercent: Int,
    val pnlAsset: String = "USDT",
    val isProfit: Boolean,
    val isPriceIncrease: Boolean,
    val maxPercent: Float? = null,
)

data class GuideRowData(
    val label: String,
    val value: String,
    @DrawableRes val iconRes: Int? = null,
)

data class AdjusterConfig(
    val min: Int,
    val max: Int,
    val step: Int,
)

@Composable
fun PerpetualGuidePage(
    initialTab: Int = PerpetualGuideBottomSheetDialogFragment.TAB_OVERVIEW,
    pop: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val tabListState = rememberLazyListState()
    val tabs = listOf(
        stringResource(R.string.Brief_Introduction),
        stringResource(R.string.Long),
        stringResource(R.string.Short),
        stringResource(R.string.Leverage),
        stringResource(R.string.position_size),
        stringResource(R.string.perps_guide_liquidation_tab),
        stringResource(R.string.take_profit_stop_loss_label),
    )
    val safeInitialTab = initialTab.coerceIn(0, tabs.lastIndex)
    var selectedTab by remember(safeInitialTab) { mutableIntStateOf(safeInitialTab) }
    var hasCenteredInitialTab by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        if (!hasCenteredInitialTab) {
            tabListState.scrollToItem(selectedTab)
            tabListState.centerVisibleTab(selectedTab, animate = false)
            hasCenteredInitialTab = true
        } else {
            val isVisible = tabListState.layoutInfo.visibleItemsInfo.any { it.index == selectedTab }
            if (!isVisible) {
                tabListState.animateScrollToItem(selectedTab)
            }
            tabListState.centerVisibleTab(selectedTab, animate = true)
        }
    }

    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topEnd = 8.dp, topStart = 8.dp))
                .background(MixinAppTheme.colors.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Text(
                    text = stringResource(R.string.Perpetual_Futures_Guide),
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
                        .clickable {
                            pop()
                        },
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                LazyRow(
                    state = tabListState,
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(
                        items = tabs,
                        key = { index, tab -> "$tab-$index" }
                    ) { index, tab ->
                        OutlinedTab(
                            text = tab,
                            selected = selectedTab == index,
                            showBadge = false,
                            onClick = { coroutineScope.launch { selectedTab = index } }
                        )
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
                        0 -> OverviewContent()
                        1 -> LongContent()
                        2 -> ShortContent()
                        3 -> LeverageContent()
                        4 -> PositionContent()
                        5 -> LiquidationContent()
                        6 -> TpSlContent()
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
                GuideBottomNavigation(
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
}

private suspend fun LazyListState.centerVisibleTab(index: Int, animate: Boolean) {
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    val itemCenter = itemInfo.offset + (itemInfo.size / 2)
    val delta = itemCenter - viewportCenter
    if (delta == 0) return
    if (animate) {
        animateScrollBy(delta.toFloat())
    } else {
        scrollBy(delta.toFloat())
    }
}


@Composable
private fun OverviewContent() {
    GuideSection(
        title = stringResource(R.string.Overview),
        content = stringResource(R.string.perps_intro_overview)
    )
}

@Composable
private fun LongContent() {
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val leverage = 10
    val investment = 1000
    val maxLossPercent = 100f / leverage
    val btcToken by remember {
        viewModel.observeTokenByChainAndSymbol(
            chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
            symbol = "BTC",
        )
    }.collectAsStateWithLifecycle(initialValue = null)
    ExampleWithScenariosCard(
        title = stringResource(R.string.Example),
        rows = listOf(
            GuideRowData(
                label = stringResource(R.string.perps_market),
                value = "BTC - USD",
                iconRes = R.drawable.ic_chain_btc
            ),
            GuideRowData(
                label = stringResource(R.string.Direction),
                value = stringResource(R.string.Long)
            ),
            GuideRowData(
                label = stringResource(R.string.Leverage),
                value = "${leverage}x"
            ),
            GuideRowData(
                label = stringResource(R.string.example_amount),
                value = "${formatGuideInt(investment)} USDT"
            )
        ),
        scenarios = listOf(
            ScenarioData(
                scenario = stringResource(R.string.example_scene1_increasing),
                change = stringResource(R.string.Price_Change),
                initialPercent = maxLossPercent,
                basePnlAmount = 1000,
                basePnlPercent = 100,
                isProfit = true,
                isPriceIncrease = true,
                maxPercent = null
            )
        ),
        showScenarioTitle = false,
        showRowsDivider = true,
    )
    Spacer(modifier = Modifier.height(16.dp))
    DescriptionWithRulesCard(
        description = stringResource(R.string.perps_long_overview),
        rules = listOf(
            stringResource(R.string.pnl_rule_price_rise_profit),
            stringResource(R.string.pnl_rule_price_fall_loss)
        )
    )
}

@Composable
private fun ShortContent() {
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val leverage = 10
    val investment = 1000
    val maxLossPercent = 100f / leverage
    val ethToken by remember {
        viewModel.observeTokenByChainAndSymbol(
            chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
            symbol = "ETH",
        )
    }.collectAsStateWithLifecycle(initialValue = null)
    ExampleWithScenariosCard(
        title = stringResource(R.string.Example),
        rows = listOf(
            GuideRowData(
                label = stringResource(R.string.perps_market),
                value = "ETH - USD",
                iconRes = R.drawable.ic_chain_eth
            ),
            GuideRowData(
                label = stringResource(R.string.Direction),
                value = stringResource(R.string.Short)
            ),
            GuideRowData(
                label = stringResource(R.string.Leverage),
                value = "${leverage}x"
            ),
            GuideRowData(
                label = stringResource(R.string.example_amount),
                value = "${formatGuideInt(investment)} USDT"
            )
        ),
        scenarios = listOf(
            ScenarioData(
                scenario = stringResource(R.string.example_scene1_decreasing),
                change = stringResource(R.string.Price_Change),
                initialPercent = maxLossPercent,
                basePnlAmount = 1000,
                basePnlPercent = 100,
                isProfit = true,
                isPriceIncrease = false,
                maxPercent = null,
            )
        ),
        showScenarioTitle = false,
        showRowsDivider = true,
    )
    Spacer(modifier = Modifier.height(16.dp))
    DescriptionWithRulesCard(
        description = stringResource(R.string.perps_short_overview),
        rules = listOf(
            stringResource(R.string.pnl_rule_price_fall_profit),
            stringResource(R.string.pnl_rule_price_rise_loss)
        )
    )
}

@Composable
private fun LeverageContent() {
    val viewModel = hiltViewModel<PerpetualViewModel>()
    var leverage by remember { mutableIntStateOf(10) }
    val investment = 1000
    val fixedProfitPercent = 10f
    val liquidationPercent = if (leverage > 0) 100f / leverage else 100f
    val solToken by remember {
        viewModel.observeTokenByChainAndSymbol(
            chainId = Constants.ChainId.Solana,
            symbol = "SOL",
        )
    }.collectAsStateWithLifecycle(initialValue = null)
    val localSolPrice = solToken?.priceUsd?.toBigDecimalOrNull()
    val orderValueUsdt = leverage * investment
    val orderValueText = buildOrderValueText(orderValueUsdt, localSolPrice, "SOL")
    val profitPnlAmount = leverage * 100
    val profitPnlPercent = leverage * 10
    ExampleWithScenariosCard(
        title = stringResource(R.string.Example),
        rows = listOf(
            GuideRowData(
                label = stringResource(R.string.perps_market),
                value = "SOL - USD",
                iconRes = R.drawable.ic_chain_sol
            ),
            GuideRowData(
                label = stringResource(R.string.Direction),
                value = stringResource(R.string.Long)
            ),
            GuideRowData(
                label = stringResource(R.string.Leverage),
                value = "${leverage}x"
            ),
            GuideRowData(
                label = stringResource(R.string.example_amount),
                value = "${formatGuideInt(investment)} USDT"
            ),
            GuideRowData(
                label = stringResource(R.string.position_size),
                value = orderValueText,
            )
        ),
        scenarios = listOf(
            ScenarioData(
                scenario = stringResource(R.string.example_scene1_increasing),
                change = stringResource(R.string.Price_Change),
                initialPercent = fixedProfitPercent,
                basePnlAmount = profitPnlAmount,
                basePnlPercent = profitPnlPercent,
                isProfit = true,
                isPriceIncrease = true,
                maxPercent = null
            )
        ),
        leverageValue = leverage,
        onLeverageChange = { leverage = it.coerceIn(1, 200) },
        isScenarioChangeAdjustable = false,
        showScenarioTitle = false,
        showRowsDivider = true,
    )
    Spacer(modifier = Modifier.height(16.dp))
    DescriptionWithInfoAndRiskCard(
        description = stringResource(R.string.perps_leverage_overview),
        infoTitle = stringResource(R.string.PnL),
        infoContents = listOf(
            stringResource(R.string.impact_on_pnl_1),
            stringResource(R.string.impact_on_pnl_2)
        ),
        riskContents = listOf(stringResource(R.string.perps_leverage_risk_notice_1), stringResource(R.string.perps_leverage_risk_notice_2))
    )
}

@Composable
private fun PositionContent() {
    val viewModel = hiltViewModel<PerpetualViewModel>()
    var leverage by remember { mutableIntStateOf(10) }
    var investment by remember { mutableIntStateOf(1000) }
    val maxLossPercent = if (leverage > 0) 100f / leverage else 100f
    val fixedProfitPercent = 10f
    val solToken by remember {
        viewModel.observeTokenByChainAndSymbol(
            chainId = Constants.ChainId.Solana,
            symbol = "SOL",
        )
    }.collectAsStateWithLifecycle(initialValue = null)
    val localSolPrice = solToken?.priceUsd?.toBigDecimalOrNull()

    val orderValueUsdt = leverage * investment
    val orderValueText = buildOrderValueText(
        orderValueUsdt = orderValueUsdt,
        localPrice = localSolPrice,
        symbol = "SOL",
    )
    val profitPnlAmount = (orderValueUsdt * fixedProfitPercent / 100).roundToInt()
    val profitPnlPercent = (leverage * fixedProfitPercent).roundToInt()
    val lossPnlAmount = investment
    val lossPnlPercent = 100

    ExampleWithScenariosCard(
        title = stringResource(R.string.Example),
        rows = listOf(
            GuideRowData(
                label = stringResource(R.string.perps_market),
                value = "SOL - USD",
                iconRes = R.drawable.ic_chain_sol
            ),
            GuideRowData(
                label = stringResource(R.string.Direction),
                value = stringResource(R.string.Long)
            ),
            GuideRowData(
                label = stringResource(R.string.Leverage),
                value = "${leverage}x"
            ),
            GuideRowData(
                label = stringResource(R.string.example_amount),
                value = "${formatGuideInt(investment)} USDT"
            ),
            GuideRowData(
                label = stringResource(R.string.position_size),
                value = orderValueText
            )
        ),
        scenarios = listOf(
            ScenarioData(
                scenario = stringResource(R.string.example_scene1_increasing),
                change = stringResource(R.string.Price_Change),
                initialPercent = fixedProfitPercent,
                basePnlAmount = profitPnlAmount,
                basePnlPercent = profitPnlPercent,
                isProfit = true,
                isPriceIncrease = true,
                maxPercent = null
            )
        ),
        leverageValue = leverage,
        onLeverageChange = { leverage = it.coerceIn(1, 200) },
        leverageConfig = AdjusterConfig(min = 1, max = 200, step = 1),
        investmentValue = investment,
        onInvestmentChange = { investment = it.coerceIn(100, 100000) },
        investmentConfig = AdjusterConfig(min = 100, max = 100000, step = 100),
        isScenarioChangeAdjustable = false,
        showScenarioTitle = false,
        showRowsDivider = true,
    )
    Spacer(modifier = Modifier.height(16.dp))
    DescriptionWithInfoAndRiskCard(
        description = stringResource(R.string.perps_position_size_overview),
        infoTitle = stringResource(R.string.PnL),
        infoContents = listOf(
            stringResource(R.string.perps_position_size_pnl_1),
            stringResource(R.string.perps_position_size_pnl_2)
        ),
        riskContents = emptyList()
    )
}

@Composable
private fun TpSlContent() {
    ExampleWithScenariosCard(
        title = stringResource(R.string.Example),
        rows = listOf(
            GuideRowData(
                label = stringResource(R.string.perps_market),
                value = "BTC - USD",
                iconRes = R.drawable.ic_chain_btc,
            ),
            GuideRowData(
                label = stringResource(R.string.Direction),
                value = stringResource(R.string.Long),
            ),
            GuideRowData(
                label = stringResource(R.string.Leverage),
                value = "10x",
            ),
            GuideRowData(
                label = stringResource(R.string.example_amount),
                value = "1,000 USDT",
            ),
        ),
        scenarios = listOf(
            ScenarioData(
                scenario = stringResource(R.string.perps_scene_tp_triggered),
                change = stringResource(R.string.Price_Change),
                initialPercent = 10f,
                basePnlAmount = 1000,
                basePnlPercent = 100,
                isProfit = true,
                isPriceIncrease = true,
            ),
        ),
        isScenarioChangeAdjustable = false,
        showScenarioTitle = false,
        showRowsDivider = true,
    )
    Spacer(modifier = Modifier.height(16.dp))
    DescriptionWithInfoAndRiskCard(
        description = stringResource(R.string.perps_tpsl_overview),
        infoTitle = stringResource(R.string.PnL),
        infoContents = listOf(
            stringResource(R.string.perps_tpsl_key_point_1),
            stringResource(R.string.perps_tpsl_key_point_2),
        ),
        riskContents = emptyList(),
    )
}

@Composable
private fun LiquidationContent() {
    val viewModel = hiltViewModel<PerpetualViewModel>()
    var isLong by remember { mutableStateOf(true) }
    var leverage by remember { mutableIntStateOf(10) }
    val solToken by remember {
        viewModel.observeTokenByChainAndSymbol(
            chainId = Constants.ChainId.Solana,
            symbol = "SOL",
        )
    }.collectAsStateWithLifecycle(initialValue = null)
    val marketPrice = remember(solToken?.priceUsd) {
        solToken?.priceUsd?.toBigDecimalOrNull() ?: BigDecimal("170.00")
    }
    val liquidationPrice = remember(marketPrice, leverage, isLong) {
        calculateGuideLiquidationPrice(
            marketPrice = marketPrice,
            leverage = leverage,
            isLong = isLong,
        )
    }

    LiquidationExampleCard(
        title = stringResource(R.string.Example),
        isLong = isLong,
        onDirectionChange = { isLong = it },
        leverage = leverage,
        onLeverageChange = { leverage = it.coerceIn(1, 200) },
        marketPrice = marketPrice,
        liquidationPrice = liquidationPrice,
    )
    Spacer(modifier = Modifier.height(16.dp))
    DescriptionWithInfoAndRiskCard(
        description = stringResource(R.string.perps_liquidation_price_overview),
        infoTitle = stringResource(R.string.Spot_Trade_Guide_Additional_Notes),
        infoContents = listOf(
            stringResource(R.string.perps_liquidation_price_key_point_1),
            stringResource(R.string.perps_liquidation_price_key_point_2),
            stringResource(R.string.perps_liquidation_price_key_point_3),
        ),
        riskContents = emptyList(),
    )
}


@Composable
private fun GuideBottomNavigation(
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
                .padding(horizontal = 18.dp)
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
                .padding(horizontal = 18.dp)
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

@Composable
private fun LiquidationExampleCard(
    title: String,
    isLong: Boolean,
    onDirectionChange: (Boolean) -> Unit,
    leverage: Int,
    onLeverageChange: (Int) -> Unit,
    marketPrice: BigDecimal,
    liquidationPrice: BigDecimal,
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
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        GuideValueRow(title = stringResource(R.string.perps_market)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chain_sol),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SOL - USD",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        GuideValueRow(title = stringResource(R.string.Direction)) {
            GuideDirectionToggle(
                isLong = isLong,
                onDirectionChange = onDirectionChange,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        GuideValueRow(title = stringResource(R.string.Leverage)) {
            GuideNumberAdjuster(
                valueText = "${leverage}x",
                canDecrease = leverage > 1,
                canIncrease = leverage < 200,
                onDecrease = { onLeverageChange(leverage - 1) },
                onIncrease = { onLeverageChange(leverage + 1) },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MixinAppTheme.colors.backgroundWindow)
        )
        Spacer(modifier = Modifier.height(16.dp))
        GuideValueRow(title = stringResource(R.string.Entry_Price)) {
            Text(
                text = "$PERPS_USD_SYMBOL${marketPrice.priceFormat()}",
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textPrimary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        GuideValueRow(title = stringResource(R.string.Liquidation_Price)) {
            Text(
                text = "$PERPS_USD_SYMBOL${liquidationPrice.priceFormat()}",
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textPrimary
            )
        }
    }
}

@Composable
fun GuideNavigationButton(
    text: String,
    isPrevious: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    MixinButton(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isPrevious) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_guide_previous),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            if (!isPrevious) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_guide_next),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            }
        }
    }
}

@Composable
private fun GuideSection(title: String, content: String) {
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
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.Product_Features),
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        listOf(
            stringResource(R.string.product_features_1),
            stringResource(R.string.product_features_2),
            stringResource(R.string.product_features_3, 200),
            stringResource(R.string.product_features_4),
        )
            .forEach { feature ->
                DotText(
                    text = feature,
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MixinAppTheme.colors.textPrimary,
                )
            }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.Risk_Notice),
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        DotText(
            text = stringResource(R.string.perps_intro_risk_notice_1),
            modifier = Modifier.padding(vertical = 4.dp),
            color = MixinAppTheme.colors.textPrimary
        )
        DotText(
            text = stringResource(R.string.perps_intro_risk_notice_2),
            modifier = Modifier.padding(vertical = 4.dp),
            color = MixinAppTheme.colors.textPrimary
        )
        DotText(
            text = stringResource(R.string.perps_intro_risk_notice_3),
            modifier = Modifier.padding(vertical = 4.dp),
            color = MixinAppTheme.colors.textPrimary
        )
    }
}

@Composable
private fun ExampleWithScenariosCard(
    title: String,
    rows: List<GuideRowData>,
    scenarios: List<ScenarioData>,
    leverageValue: Int? = null,
    onLeverageChange: ((Int) -> Unit)? = null,
    leverageConfig: AdjusterConfig = AdjusterConfig(min = 1, max = 200, step = 1),
    investmentValue: Int? = null,
    onInvestmentChange: ((Int) -> Unit)? = null,
    investmentConfig: AdjusterConfig = AdjusterConfig(min = 10, max = 1000, step = 10),
    isScenarioChangeAdjustable: Boolean = true,
    showScenarioTitle: Boolean = true,
    showRowsDivider: Boolean = false,
) {
    val context = LocalContext.current
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val orderedScenarios = scenarios.sortedByDescending { it.isProfit }
    val changePercents = remember(scenarios.hashCode(), leverageValue, investmentValue) {
        mutableStateListOf<Float>().apply {
            addAll(
                orderedScenarios.map { scenario ->
                    val nonNegativePercent = scenario.initialPercent.coerceAtLeast(0f)
                    scenario.maxPercent?.let { maxPercent ->
                        nonNegativePercent.coerceAtMost(maxPercent)
                    } ?: nonNegativePercent
                }
            )
        }
    }
    val directionLabel = stringResource(R.string.Direction)
    val leverageLabel = stringResource(R.string.Leverage)
    val investmentLabel = stringResource(R.string.example_amount)
    val longDirection = stringResource(R.string.Long)
    val shortDirection = stringResource(R.string.Short)
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
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        rows.forEachIndexed { index, row ->
            val label = row.label
            val value = row.value
            if (index > 0) {
                Spacer(modifier = Modifier.height(16.dp))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.weight(1f)
                )
                if (label == directionLabel && (value == longDirection || value == shortDirection)) {
                    val directionColor = if (value == longDirection) risingColor else fallingColor
                    Text(
                        text = value,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(directionColor)
                            .padding(horizontal = 8.dp, vertical = 1.dp),
                    )
                } else if (label == leverageLabel && leverageValue != null && onLeverageChange != null) {
                    GuideNumberAdjuster(
                        valueText = "${leverageValue}x",
                        canDecrease = leverageValue > leverageConfig.min,
                        canIncrease = leverageValue < leverageConfig.max,
                        onDecrease = {
                            onLeverageChange((leverageValue - leverageConfig.step).coerceAtLeast(leverageConfig.min))
                        },
                        onIncrease = {
                            onLeverageChange((leverageValue + leverageConfig.step).coerceAtMost(leverageConfig.max))
                        },
                    )
                } else if (label == investmentLabel && investmentValue != null && onInvestmentChange != null) {
                    GuideNumberAdjuster(
                        valueText = "${formatGuideInt(investmentValue)} USDT",
                        canDecrease = investmentValue > investmentConfig.min,
                        canIncrease = investmentValue < investmentConfig.max,
                        onDecrease = {
                            onInvestmentChange((investmentValue - investmentConfig.step).coerceAtLeast(investmentConfig.min))
                        },
                        onIncrease = {
                            onInvestmentChange((investmentValue + investmentConfig.step).coerceAtMost(investmentConfig.max))
                        },
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        row.iconRes?.let { iconRes ->
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = value,
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.textPrimary
                        )
                    }
                }
            }
        }

        if (showRowsDivider) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MixinAppTheme.colors.backgroundWindow)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        orderedScenarios.forEachIndexed { index, scenario ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(16.dp))
            }
            val priceChangeColor = if (scenario.isPriceIncrease) risingColor else fallingColor
            val pnlColor = if (scenario.isProfit) risingColor else fallingColor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                if (showScenarioTitle) {
                    Text(
                        text = scenario.scenario,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        color = MixinAppTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = scenario.change,
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist,
                        modifier = Modifier.weight(1f)
                    )
                    val percent = changePercents[index]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    )
                    {
                        if (isScenarioChangeAdjustable) {
                            val maxPercent = scenario.maxPercent
                            val step = if (percent < 1f) 0.1f else 1f
                            val canDecrease = percent > 0f
                            val canIncrease = if (maxPercent == null) {
                                true
                            } else {
                                percent < maxPercent
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MixinAppTheme.colors.backgroundWindow)
                                    .alpha(if (canDecrease) 1f else 0.5f)
                                    .clickable(enabled = canDecrease) {
                                        changePercents[index] = (percent - step).coerceAtLeast(0f)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_perps_minus),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Text(
                                text = scenario.formatChangePercent(percent),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = priceChangeColor,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MixinAppTheme.colors.backgroundWindow)
                                    .alpha(if (canIncrease) 1f else 0.5f)
                                    .clickable(enabled = canIncrease) {
                                        val nextPercent = if (maxPercent == null) {
                                            percent + step
                                        } else {
                                            (percent + step).coerceAtMost(maxPercent)
                                        }
                                        changePercents[index] = nextPercent
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_perps_add),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else {
                            Text(
                                text = scenario.formatChangePercent(percent),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = priceChangeColor,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.PnL),
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = scenario.formatPnl(changePercents[index]),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = pnlColor
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideNumberAdjuster(
    valueText: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MixinAppTheme.colors.backgroundWindow)
                .alpha(if (canDecrease) 1f else 0.5f)
                .clickable(enabled = canDecrease, onClick = onDecrease),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_perps_minus),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = valueText,
            fontSize = 14.sp,
            fontWeight = FontWeight.W500,
            color = MixinAppTheme.colors.textPrimary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MixinAppTheme.colors.backgroundWindow)
                .alpha(if (canIncrease) 1f else 0.5f)
                .clickable(enabled = canIncrease, onClick = onIncrease),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_perps_add),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun GuideValueRow(
    title: String,
    value: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist,
            modifier = Modifier.weight(1f)
        )
        Box(contentAlignment = Alignment.CenterEnd) {
            value()
        }
    }
}

@Composable
private fun GuideDirectionToggle(
    isLong: Boolean,
    onDirectionChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val longColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val shortColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MixinAppTheme.colors.backgroundWindow)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(true, false).forEach { itemIsLong ->
            val selected = itemIsLong == isLong
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (selected) {
                            if (itemIsLong) longColor else shortColor
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable { onDirectionChange(itemIsLong) }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(if (itemIsLong) R.string.Long else R.string.Short),
                    color = if (selected) Color.White else MixinAppTheme.colors.textAssist,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

private fun formatGuideInt(value: Int): String {
    return String.format("%,d", value)
}

private fun calculateGuideLiquidationPrice(
    marketPrice: BigDecimal,
    leverage: Int,
    isLong: Boolean,
): BigDecimal {
    val safeLeverage = leverage.coerceAtLeast(1)
    val liquidationOffset = BigDecimal.ONE.divide(
        safeLeverage.toBigDecimal(),
        8,
        RoundingMode.HALF_UP,
    )
    val multiplier = if (isLong) {
        BigDecimal.ONE.subtract(liquidationOffset)
    } else {
        BigDecimal.ONE.add(liquidationOffset)
    }
    return marketPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP)
}

private fun buildOrderValueText(
    orderValueUsdt: Int,
    localPrice: BigDecimal?,
    symbol: String,
): String {
    val price = localPrice ?: return "-- $symbol"
    if (price <= BigDecimal.ZERO) {
        return "-- $symbol"
    }
    val amount = BigDecimal(orderValueUsdt.toString())
        .divide(price, 2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
    return "$amount $symbol"
}

private fun formatPercent(percent: Float): String {
    return if (percent % 1 == 0f) {
        "${percent.toInt()}%"
    } else {
        val formatted = String.format("%.2f", percent)
        val trimmed = formatted.trimEnd('0').trimEnd('.')
        "$trimmed%"
    }
}

private fun ScenarioData.formatChangePercent(currentPercent: Float): String {
    val sign = if (isPriceIncrease) "+" else "-"
    return "$sign${formatPercent(currentPercent)}"
}

private fun ScenarioData.formatPnl(currentPercent: Float): String {
    val safeInitialPercent = initialPercent.coerceAtLeast(0.01f)
    val safeCurrentPercent = currentPercent.coerceAtLeast(0f)
    val amount = (basePnlAmount.toFloat() * safeCurrentPercent / safeInitialPercent).roundToInt()
    val percent = (basePnlPercent.toFloat() * safeCurrentPercent / safeInitialPercent).roundToInt()
    val sign = if (isProfit) "+" else "-"
    val amountText = String.format("%,d", amount)
    return if (isProfit) {
        "$sign$amountText $pnlAsset ($sign$percent%)"
    } else {
        "$sign$amountText $pnlAsset"
    }
}

@Composable
private fun DescriptionWithRulesCard(
    description: String,
    rules: List<String>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()

            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.Overview),
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MixinAppTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.PnL),
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        rules.forEach { rule ->
            DotText(
                text = rule,
                modifier = Modifier.padding(vertical = 4.dp),
                color = MixinAppTheme.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun DescriptionWithInfoAndRiskCard(
    description: String,
    infoTitle: String,
    infoContents: List<String>,
    riskContents: List<String>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.Overview),
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MixinAppTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = infoTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                color = MixinAppTheme.colors.textMinor
            )
            Spacer(modifier = Modifier.height(6.dp))
            infoContents.forEach { content ->
                DotText(
                    text = content,
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MixinAppTheme.colors.textPrimary,
                )
            }
        }

        if (riskContents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.Risk_Notice),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W500,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                riskContents.forEach { riskContent ->
                    DotText(
                        text = riskContent,
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MixinAppTheme.colors.textPrimary
                    )
                }
            }
        }

    }
}
