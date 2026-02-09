package one.mixin.android.ui.home.web3.trade

import PageScaffold
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.api.response.perps.MarketView
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.web3.components.OutlinedTab
import one.mixin.android.ui.wallet.alert.components.cardBackground
import java.math.BigDecimal

@Composable
fun MarketDetailPage(
    marketId: String,
    marketSymbol: String,
    onBack: () -> Unit
) {
    val viewModel = hiltViewModel<PerpetualViewModel>()
    var market by remember { mutableStateOf<MarketView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTimeFrame by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val timeFrames = listOf("1h", "1d", "1w", "1M")

    LaunchedEffect(marketId) {
        viewModel.loadMarketDetail(
            marketId = marketId,
            onSuccess = { data ->
                market = data
                isLoading = false
            },
            onError = {
                isLoading = false
            }
        )
    }

    PageScaffold(
        title = marketSymbol,
        verticalScrollable = false,
        pop = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 价格信息卡片
            if (market != null) {
                PriceInfoCard(market = market!!)
            } else if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading...",
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                timeFrames.forEachIndexed { index, timeFrame ->
                    OutlinedTab(
                        text = timeFrame,
                        selected = selectedTimeFrame == index,
                        showBadge = false,
                        onClick = { coroutineScope.launch { selectedTimeFrame = index } }
                    )
                    if (index < timeFrames.size - 1) Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
                    .padding(16.dp)
            ) {
                if (market != null) {
                    CandleChart(
                        marketId = marketId,
                        timeFrame = timeFrames[selectedTimeFrame]
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading chart...",
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.textAssist
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PriceInfoCard(market: MarketView) {
    val change = try {
        BigDecimal(market.change)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }

    val isPositive = change >= BigDecimal.ZERO
    val changeColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
    val changeText = "${if (isPositive) "+" else ""}${market.change}%"

    val formattedPrice = try {
        val price = BigDecimal(market.markPrice)
        if (price >= BigDecimal("1000")) {
            String.format("%.2f", price)
        } else if (price >= BigDecimal("1")) {
            String.format("%.4f", price)
        } else {
            String.format("%.6f", price)
        }
    } catch (e: Exception) {
        market.markPrice
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Text(
            text = "Mark Price",
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "$$formattedPrice",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = changeText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = changeColor,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }
    }
}
