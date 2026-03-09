package one.mixin.android.ui.home.web3.trade.perps

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.home.web3.components.OutlinedTab
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.widget.components.DotText

data class ScenarioData(
    val scenario: String,
    val change: String,
    val initialPercent: Float = 10f,
    val basePnlAmount: Int,
    val basePnlPercent: Int,
    val pnlAsset: String = "USDT",
    val isProfit: Boolean,
    val maxPercent: Float? = null,
)

data class GuideRowData(
    val label: String,
    val value: String,
    @DrawableRes val iconRes: Int? = null,
)

@Composable
fun PerpetualGuidePage(
    initialTab: Int = PerpetualGuideFragment.TAB_OVERVIEW,
    pop: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf(
        stringResource(R.string.Perpetual_Guide_Overview),
        stringResource(R.string.Perpetual_Guide_Long),
        stringResource(R.string.Perpetual_Guide_Short),
        stringResource(R.string.Perpetual_Guide_Leverage),
        stringResource(R.string.Perpetual_Guide_Position)
    )
    val safeInitialTab = initialTab.coerceIn(0, tabs.lastIndex)
    var selectedTab by remember(safeInitialTab) { mutableIntStateOf(safeInitialTab) }

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
                    text = stringResource(R.string.Perpetual),
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
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    tabs.forEachIndexed { index, tab ->
                        OutlinedTab(
                            text = tab,
                            selected = selectedTab == index,
                            showBadge = false,
                            onClick = { coroutineScope.launch { selectedTab = index } }
                        )
                        if (index < tabs.size - 1) Spacer(modifier = Modifier.width(10.dp))
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


@Composable
private fun OverviewContent() {
    GuideSection(
        title = stringResource(R.string.Perpetual_Guide_Overview_Title),
        content = stringResource(R.string.Perpetual_Guide_Overview_Desc)
    )
}

@Composable
private fun LongContent() {
    val leverage = 10
    val maxLossPercent = 100f / leverage
    ExampleWithScenariosCard(
        title = stringResource(R.string.Perpetual_Example),
        rows = listOf(
            GuideRowData(
                label = stringResource(R.string.Perpetual_Trading_Pair),
                value = "BTC - USD",
                iconRes = R.drawable.ic_chain_btc
            ),
            GuideRowData(
                label = stringResource(R.string.Perpetual_Direction),
                value = stringResource(R.string.Long)
            ),
            GuideRowData(
                label = stringResource(R.string.Perpetual_Leverage_Times),
                value = "${leverage}x"
            ),
            GuideRowData(
                label = stringResource(R.string.Perpetual_Investment),
                value = "1,000 USDT"
            )
        ),
        scenarios = listOf(
            ScenarioData(
                scenario = stringResource(R.string.Perpetual_Price_Up),
                change = stringResource(R.string.Perpetual_Price_Up_Amplitude),
                initialPercent = maxLossPercent,
                basePnlAmount = 1000,
                basePnlPercent = 100,
                isProfit = true,
                maxPercent = null
            ),
            ScenarioData(
                scenario = stringResource(R.string.Perpetual_Price_Down),
                change = stringResource(R.string.Perpetual_Price_Down_Amplitude),
                initialPercent = maxLossPercent,
                basePnlAmount = 1000,
                basePnlPercent = 100,
                isProfit = false,
                maxPercent = maxLossPercent,
            )
        )
    )
    Spacer(modifier = Modifier.height(16.dp))
    DescriptionWithRulesCard(
        description = stringResource(R.string.Perpetual_Long_Desc),
        rules = listOf(
            stringResource(R.string.Perpetual_Price_Up) to stringResource(R.string.Perpetual_Profit),
            stringResource(R.string.Perpetual_Price_Down) to stringResource(R.string.Perpetual_Loss)
        )
    )
}

@Composable
private fun ShortContent() {
    val leverage = 10
    val maxLossPercent = 100f / leverage
    ExampleWithScenariosCard(
        title = stringResource(R.string.Perpetual_Example),
        rows = listOf(
            GuideRowData(
                label = stringResource(R.string.Perpetual_Trading_Pair),
                value = "ETH - USD",
                iconRes = R.drawable.ic_chain_eth
            ),
            GuideRowData(
                label = stringResource(R.string.Perpetual_Direction),
                value = stringResource(R.string.Short)
            ),
            GuideRowData(
                label = stringResource(R.string.Perpetual_Leverage_Times),
                value = "${leverage}x"
            ),
            GuideRowData(
                label = stringResource(R.string.Perpetual_Investment),
                value = "1,000 USDT"
            )
        ),
        scenarios = listOf(
            ScenarioData(
                scenario = stringResource(R.string.Perpetual_Price_Down),
                change = stringResource(R.string.Perpetual_Price_Down_Amplitude),
                initialPercent = maxLossPercent,
                basePnlAmount = 1000,
                basePnlPercent = 100,
                isProfit = true,
                maxPercent = null,
            ),
            ScenarioData(
                scenario = stringResource(R.string.Perpetual_Price_Up),
                change = stringResource(R.string.Perpetual_Price_Up_Amplitude),
                initialPercent = maxLossPercent,
                basePnlAmount = 1000,
                basePnlPercent = 100,
                isProfit = false,
                maxPercent = maxLossPercent,
            )
        )
    )
    Spacer(modifier = Modifier.height(16.dp))
    DescriptionWithRulesCard(
        description = stringResource(R.string.Perpetual_Short_Desc),
        rules = listOf(
            stringResource(R.string.Perpetual_Price_Down) to stringResource(R.string.Perpetual_Profit),
            stringResource(R.string.Perpetual_Price_Up) to stringResource(R.string.Perpetual_Loss)
        )
    )
}

@Composable
private fun LeverageContent() {
    var leverage by remember { mutableIntStateOf(10) }
    val maxLossPercent = if (leverage > 0) 100f / leverage else 100f
    val basePnlAmount = leverage * 100
    val basePnlPercent = leverage * 10
    ExampleWithScenariosCard(
        title = stringResource(R.string.Perpetual_Example),
        rows = listOf(
            GuideRowData(
                label = stringResource(R.string.Perpetual_Trading_Pair),
                value = "SOL - USD",
                iconRes = R.drawable.ic_chain_sol
            ),
            GuideRowData(
                label = stringResource(R.string.Perpetual_Direction),
                value = stringResource(R.string.Long)
            ),
            GuideRowData(
                label = stringResource(R.string.Perpetual_Leverage_Times),
                value = "${leverage}x"
            ),
            GuideRowData(
                label = stringResource(R.string.Perpetual_Investment),
                value = "1,000 USDT"
            )
        ),
        scenarios = listOf(
            ScenarioData(
                scenario = stringResource(R.string.Perpetual_Price_Up),
                change = stringResource(R.string.Perpetual_Price_Up_Amplitude),
                initialPercent = maxLossPercent,
                basePnlAmount = basePnlAmount,
                basePnlPercent = basePnlPercent,
                isProfit = true,
                maxPercent = null
            ),
            ScenarioData(
                scenario = stringResource(R.string.Perpetual_Price_Down),
                change = stringResource(R.string.Perpetual_Price_Down_Amplitude),
                initialPercent = maxLossPercent,
                basePnlAmount = basePnlAmount,
                basePnlPercent = basePnlPercent,
                isProfit = false,
                maxPercent = maxLossPercent,
            )
        ),
        leverageValue = leverage,
        onLeverageChange = { leverage = it.coerceIn(1, 200) },
        isScenarioChangeAdjustable = false,
    )
    Spacer(modifier = Modifier.height(16.dp))
    DescriptionWithInfoAndRiskCard(
        description = stringResource(R.string.Perpetual_Leverage_Desc),
        infoTitle = stringResource(R.string.Perpetual_PnL_Impact),
        infoContents = listOf(stringResource(R.string.Perpetual_Leverage_Impact)),
        riskContent = stringResource(R.string.Perpetual_Leverage_Risk)
    )
}

@Composable
private fun PositionContent() {
    val viewModel = hiltViewModel<PerpetualViewModel>()
    var leverage by remember { mutableIntStateOf(10) }
    var investment by remember { mutableIntStateOf(1000) }
    val maxLossPercent = if (leverage > 0) 100f / leverage else 100f
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
        localSolPrice = localSolPrice
    )
    val basePnlAmount = (orderValueUsdt * maxLossPercent / 100).toInt()
    val basePnlPercent = (leverage * maxLossPercent).toInt()

    ExampleWithScenariosCard(
        title = stringResource(R.string.Perpetual_Example),
        rows = listOf(
            GuideRowData(
                label = stringResource(R.string.Perpetual_Trading_Pair),
                value = "SOL - USD",
                iconRes = R.drawable.ic_chain_sol
            ),
            GuideRowData(
                label = stringResource(R.string.Perpetual_Direction),
                value = stringResource(R.string.Long)
            ),
            GuideRowData(
                label = stringResource(R.string.Perpetual_Leverage_Times),
                value = "${leverage}x"
            ),
            GuideRowData(
                label = stringResource(R.string.Perpetual_Investment),
                value = "${formatGuideInt(investment)} USDT"
            ),
            GuideRowData(
                label = stringResource(R.string.Order_Value),
                value = orderValueText
            )
        ),
        scenarios = listOf(
            ScenarioData(
                scenario = stringResource(R.string.Perpetual_Price_Up),
                change = stringResource(R.string.Perpetual_Price_Up_Amplitude),
                initialPercent = maxLossPercent,
                basePnlAmount = basePnlAmount,
                basePnlPercent = basePnlPercent,
                isProfit = true,
                maxPercent = null
            ),
            ScenarioData(
                scenario = stringResource(R.string.Perpetual_Price_Down),
                change = stringResource(R.string.Perpetual_Price_Down_Amplitude),
                initialPercent = maxLossPercent,
                basePnlAmount = basePnlAmount,
                basePnlPercent = basePnlPercent,
                isProfit = false,
                maxPercent = maxLossPercent,
            )
        ),
        leverageValue = leverage,
        onLeverageChange = { leverage = it.coerceIn(1, 100) },
        investmentValue = investment,
        onInvestmentChange = { investment = it.coerceIn(10, 1000) },
        isScenarioChangeAdjustable = false,
        maxLeverage = 100,
    )
    Spacer(modifier = Modifier.height(16.dp))
    DescriptionWithInfoAndRiskCard(
        description = stringResource(R.string.Perpetual_Position_Desc),
        infoTitle = stringResource(R.string.Perpetual_Position_Usage),
        infoContents = listOf(
            stringResource(R.string.Perpetual_Position_Usage_Support_Current_Position),
            stringResource(R.string.Perpetual_Position_Usage_Offset_Floating_Losses),
        ),
        riskContent = stringResource(R.string.Perpetual_Position_Risk)
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
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GuideNavigationButton(
                text = stringResource(R.string.Perpetual_Guide_Previous_Tab, tabs[previousTab]),
                modifier = Modifier.weight(1f),
                onClick = { onSelect(previousTab) },
            )
            GuideNavigationButton(
                text = stringResource(
                    R.string.Perpetual_Guide_Next_Tab,
                    stringResource(R.string.Start)
                ),
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
                text = stringResource(R.string.Perpetual_Guide_Previous_Tab, tabs[previousTab]),
                modifier = Modifier.weight(1f),
                onClick = { onSelect(previousTab) },
            )
            GuideNavigationButton(
                text = stringResource(R.string.Perpetual_Guide_Next_Tab, tabs[nextTab]),
                modifier = Modifier.weight(1f),
                onClick = { onSelect(nextTab) },
            )
        }
        return
    }
    val targetIndex = previousTab ?: nextTab ?: return
    val buttonText = if (previousTab != null) {
        stringResource(R.string.Perpetual_Guide_Previous_Tab, tabs[targetIndex])
    } else {
        stringResource(R.string.Perpetual_Guide_Next_Tab, tabs[targetIndex])
    }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        GuideNavigationButton(
            text = buttonText,
            modifier = Modifier.fillMaxWidth(0.5f),
            onClick = { onSelect(targetIndex) },
        )
    }
}

@Composable
private fun GuideNavigationButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier.height(48.dp),
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = MixinAppTheme.colors.accent,
            contentColor = Color.White,
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
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
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
            text = stringResource(R.string.Perpetual_Features),
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        listOf(
            stringResource(R.string.Perpetual_Feature_1),
            stringResource(R.string.Perpetual_Feature_2),
            stringResource(R.string.Perpetual_Feature_3),
            stringResource(R.string.Perpetual_Feature_4),
            stringResource(R.string.Perpetual_Feature_5)
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
            text = stringResource(R.string.Perpetual_Risk_Warning),
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        DotText(
            text = stringResource(R.string.Perpetual_Risk_Warning_Content),
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
    investmentValue: Int? = null,
    onInvestmentChange: ((Int) -> Unit)? = null,
    isScenarioChangeAdjustable: Boolean = true,
    maxLeverage: Int = 200,
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
    val directionLabel = stringResource(R.string.Perpetual_Direction)
    val leverageLabel = stringResource(R.string.Perpetual_Leverage_Times)
    val investmentLabel = stringResource(R.string.Perpetual_Investment)
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
                        canDecrease = leverageValue > 1,
                        canIncrease = leverageValue < maxLeverage,
                        onDecrease = { onLeverageChange((leverageValue - 1).coerceAtLeast(1)) },
                        onIncrease = { onLeverageChange((leverageValue + 1).coerceAtMost(maxLeverage)) },
                    )
                } else if (label == investmentLabel && investmentValue != null && onInvestmentChange != null) {
                    GuideNumberAdjuster(
                        valueText = "${formatGuideInt(investmentValue)} USDT",
                        canDecrease = investmentValue > 10,
                        canIncrease = investmentValue < 1000,
                        onDecrease = { onInvestmentChange((investmentValue - 10).coerceAtLeast(10)) },
                        onIncrease = { onInvestmentChange((investmentValue + 10).coerceAtMost(1000)) },
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

        Spacer(modifier = Modifier.height(16.dp))

        orderedScenarios.forEachIndexed { index, scenario ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(16.dp))
            }
            val scenarioTitle = stringResource(
                R.string.Perpetual_Scenario_Title_Format,
                index + 1,
                scenario.scenario,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = scenarioTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W500,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
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
                                text = formatPercent(percent),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MixinAppTheme.colors.textPrimary,
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
                                text = formatPercent(percent),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MixinAppTheme.colors.textPrimary,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.Perpetual_PnL),
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = scenario.formatPnl(changePercents[index]),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (scenario.isProfit) risingColor else fallingColor
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

private fun formatGuideInt(value: Int): String {
    return String.format("%,d", value)
}

private fun buildOrderValueText(
    orderValueUsdt: Int,
    localSolPrice: BigDecimal?,
): String {
    val usdtText = "${formatGuideInt(orderValueUsdt)} USDT"
    val solPrice = localSolPrice ?: return "$usdtText (-- SOL)"
    if (solPrice <= BigDecimal.ZERO) {
        return "$usdtText (-- SOL)"
    }
    val solAmount = BigDecimal(orderValueUsdt.toString())
        .divide(solPrice, 2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
    return "$usdtText ($solAmount SOL)"
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
    rules: List<Pair<String, String>>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()

            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.Perpetual_Detail_Desc),
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
            text = stringResource(R.string.Perpetual_PnL_Rules),
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        rules.forEach { (condition, result) ->
            DotText(
                text = "$condition：$result",
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
    riskContent: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.Perpetual_Detail_Desc),
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

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.Perpetual_Risk_Warning),
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            DotText(
                text = riskContent,
                color = MixinAppTheme.colors.textPrimary
            )
        }
    }
}
