package one.mixin.android.ui.home.web3.trade

import PageScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.web3.components.OutlinedTab
import one.mixin.android.ui.wallet.alert.components.cardBackground

data class ScenarioData(
    val scenario: String,
    val change: String,
    val changeValue: String,
    val pnl: String,
    val isProfit: Boolean,
)

@Composable
fun PerpetualGuidePage(pop: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf(
        stringResource(R.string.Perpetual_Guide_Overview),
        stringResource(R.string.Perpetual_Guide_Long),
        stringResource(R.string.Perpetual_Guide_Short),
        stringResource(R.string.Perpetual_Guide_Leverage),
        stringResource(R.string.Perpetual_Guide_Position)
    )

    PageScaffold(
        title = stringResource(R.string.Trading_Guide),
        verticalScrollable = false,
        pop = pop
    ) {
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
                    .fillMaxSize()
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
    ExampleWithScenariosCard(
        title = stringResource(R.string.Perpetual_Example),
        rows = listOf(
            stringResource(R.string.Perpetual_Trading_Pair) to "BTC - USD",
            stringResource(R.string.Perpetual_Direction) to "Long",
            stringResource(R.string.Perpetual_Leverage_Times) to "10x",
            stringResource(R.string.Perpetual_Investment) to "1,000 USDT"
        ),
        scenarios = listOf(
            ScenarioData(
                stringResource(R.string.Perpetual_Scenario_1),
                stringResource(R.string.Perpetual_Price_Up),
                "10%",
                "+100 USDT (+10%)",
                true
            ),
            ScenarioData(
                stringResource(R.string.Perpetual_Scenario_2),
                stringResource(R.string.Perpetual_Price_Down),
                "10%",
                "-100 USDT (-10%)",
                false
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
    ExampleWithScenariosCard(
        title = stringResource(R.string.Perpetual_Example),
        rows = listOf(
            stringResource(R.string.Perpetual_Trading_Pair) to "ETH - USD",
            stringResource(R.string.Perpetual_Direction) to "Short",
            stringResource(R.string.Perpetual_Leverage_Times) to "10x",
            stringResource(R.string.Perpetual_Investment) to "1,000 USDT"
        ),
        scenarios = listOf(
            ScenarioData(
                stringResource(R.string.Perpetual_Scenario_1),
                stringResource(R.string.Perpetual_Price_Down),
                "10%",
                "+100 USDT (+10%)",
                true
            ),
            ScenarioData(
                stringResource(R.string.Perpetual_Scenario_2),
                stringResource(R.string.Perpetual_Price_Up),
                "10%",
                "-100 USDT (-10%)",
                false
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
    ExampleWithScenariosCard(
        title = stringResource(R.string.Perpetual_Example),
        rows = listOf(
            stringResource(R.string.Perpetual_Trading_Pair) to "SOL - USD",
            stringResource(R.string.Perpetual_Direction) to "Long",
            stringResource(R.string.Perpetual_Leverage_Times) to "10x",
            stringResource(R.string.Perpetual_Investment) to "1,000 USDT"
        ),
        scenarios = listOf(
            ScenarioData(
                stringResource(R.string.Perpetual_Scenario_1),
                stringResource(R.string.Perpetual_Price_Up),
                "10%",
                "+1,000 USDT (+100%)",
                true
            ),
            ScenarioData(
                stringResource(R.string.Perpetual_Scenario_2),
                stringResource(R.string.Perpetual_Price_Down),
                "10%",
                "-1,000 USDT (-100%)",
                false
            )
        )
    )
    Spacer(modifier = Modifier.height(16.dp))
    DescriptionWithInfoAndRiskCard(
        description = stringResource(R.string.Perpetual_Leverage_Desc),
        infoTitle = stringResource(R.string.Perpetual_PnL_Impact),
        infoContent = stringResource(R.string.Perpetual_Leverage_Impact),
        riskContent = stringResource(R.string.Perpetual_Leverage_Risk)
    )
}

@Composable
private fun PositionContent() {
    ExampleWithScenariosCard(
        title = stringResource(R.string.Perpetual_Example),
        rows = listOf(
            stringResource(R.string.Perpetual_Trading_Pair) to "SOL - USD",
            stringResource(R.string.Perpetual_Direction) to "Long",
            stringResource(R.string.Perpetual_Leverage_Times) to "10x",
            stringResource(R.string.Perpetual_Investment) to "1,000 USDT",
            stringResource(R.string.Perpetual_Position_Value) to "10,000 USDT (74.62 SOL)"
        ),
        scenarios = listOf(
            ScenarioData(
                stringResource(R.string.Perpetual_Scenario_1),
                stringResource(R.string.Perpetual_Price_Up),
                "10%",
                "+1,000 USDT (+100%)",
                true
            ),
            ScenarioData(
                stringResource(R.string.Perpetual_Scenario_2),
                stringResource(R.string.Perpetual_Price_Down),
                "10%",
                "-1,000 USDT (-100%)",
                false
            )
        )
    )
    Spacer(modifier = Modifier.height(16.dp))
    DescriptionWithInfoAndRiskCard(
        description = stringResource(R.string.Perpetual_Position_Desc),
        infoTitle = stringResource(R.string.Perpetual_Position_Usage),
        infoContent = stringResource(R.string.Perpetual_Position_Usage_Desc),
        riskContent = stringResource(R.string.Perpetual_Position_Risk)
    )
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
            fontWeight = FontWeight.SemiBold,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MixinAppTheme.colors.textAssist
        )
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
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
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(text = "• ", fontSize = 14.sp, color = MixinAppTheme.colors.textAssist)
                    Text(
                        text = feature,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MixinAppTheme.colors.textAssist,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.Perpetual_Risk_Warning),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MixinAppTheme.colors.textMinor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.Perpetual_Risk_Warning_Content),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MixinAppTheme.colors.textMinor
        )
    }
}

@Composable
private fun ExampleWithScenariosCard(
    title: String,
    rows: List<Pair<String, String>>,
    scenarios: List<ScenarioData>,
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
            fontWeight = FontWeight.SemiBold,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        rows.forEachIndexed { index, (label, value) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MixinAppTheme.colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        scenarios.forEachIndexed { index, scenario ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(12.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = scenario.scenario,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = scenario.change,
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = scenario.changeValue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (scenario.isProfit) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.Perpetual_PnL),
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = scenario.pnl,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (scenario.isProfit) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
        }
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
            fontWeight = FontWeight.SemiBold,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MixinAppTheme.colors.textAssist
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.Perpetual_PnL_Rules),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        rules.forEach { (condition, result) ->
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(text = "$condition：", fontSize = 14.sp, color = MixinAppTheme.colors.textAssist)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = result,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MixinAppTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun DescriptionWithInfoAndRiskCard(
    description: String,
    infoTitle: String,
    infoContent: String,
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
            fontWeight = FontWeight.SemiBold,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MixinAppTheme.colors.textAssist
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = infoTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MixinAppTheme.colors.textMinor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = infoContent,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = MixinAppTheme.colors.textMinor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.Perpetual_Risk_Warning),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MixinAppTheme.colors.textMinor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = riskContent,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = MixinAppTheme.colors.textMinor
            )
        }
    }
}

