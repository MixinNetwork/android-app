package one.mixin.android.ui.home.web3.trade

import PageScaffold
import android.graphics.drawable.Icon
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.wallet.alert.components.cardBackground
import java.math.BigDecimal

@Composable
fun MarketDetailPage(
    marketId: String,
    marketSymbol: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<PerpetualViewModel>()
    var market by remember { mutableStateOf<PerpsMarket?>(null) }
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
                    .padding(16.dp)
            ) {
                if (market != null) {
                    MarketDetailCard(
                        market = market!!,
                        marketSymbol = marketSymbol,
                        selectedTimeFrame = selectedTimeFrame,
                        timeFrames = timeFrames,
                        onTimeFrameChange = { index ->
                            coroutineScope.launch { selectedTimeFrame = index }
                        }
                    )
                } else if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MixinAppTheme.colors.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (market != null) {
                MarketInfoCard(
                    market = market!!,
                    onLearnClick = { /* TODO: Navigate to guide */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (market != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = {
                            PerpsActivity.showOpenPosition(
                                context = context,
                                marketId = marketId,
                                marketSymbol = marketSymbol,
                                isLong = true
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = MixinAppTheme.colors.walletGreen
                        ),
                        shape = RoundedCornerShape(32.dp),
                        elevation = ButtonDefaults.elevation(
                            pressedElevation = 0.dp,
                            defaultElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = "Long",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = {
                            PerpsActivity.showOpenPosition(
                                context = context,
                                marketId = marketId,
                                marketSymbol = marketSymbol,
                                isLong = false
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = MixinAppTheme.colors.walletRed
                        ),
                        shape = RoundedCornerShape(32.dp),
                        elevation = ButtonDefaults.elevation(
                            pressedElevation = 0.dp,
                            defaultElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = "Short",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MarketInfoCard(
    market: PerpsMarket,
    onLearnClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLearnClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painter = painterResource(id = R.drawable.ic_perps_help), contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "How perps works?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MixinAppTheme.colors.textPrimary
                )
                Text(
                    text = "Learn how to trade perps",
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Text(
            text = "24H VOLUME",
            fontSize = 12.sp,
            color = MixinAppTheme.colors.textAssist
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$${formatVolume(market.volume)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MixinAppTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))


        Column {
            Text(
                text = "Open Interest",
                fontSize = 12.sp,
                color = MixinAppTheme.colors.textAssist
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "-",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Column {
            Text(
                text = "Funding Rate",
                fontSize = 12.sp,
                color = MixinAppTheme.colors.textAssist
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${market.fundingRate}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.textPrimary
            )
        }
    }
}

private fun formatVolume(volume: String): String {
    return try {
        val vol = BigDecimal(volume)
        when {
            vol >= BigDecimal("1000000000") -> String.format("%.2fB", vol.divide(BigDecimal("1000000000")))
            vol >= BigDecimal("1000000") -> String.format("%.2fM", vol.divide(BigDecimal("1000000")))
            vol >= BigDecimal("1000") -> String.format("%.2fK", vol.divide(BigDecimal("1000")))
            else -> String.format("%.2f", vol)
        }
    } catch (e: Exception) {
        volume
    }
}

@Composable
private fun MarketDetailCard(
    market: PerpsMarket,
    marketSymbol: String,
    selectedTimeFrame: Int,
    timeFrames: List<String>,
    onTimeFrameChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val quoteColorPref = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)

    val change = try {
        BigDecimal(market.change)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }

    val isPositive = change >= BigDecimal.ZERO
    val changeColor = if (isPositive) {
        if (quoteColorPref) {
            MixinAppTheme.colors.walletRed
        } else {
            MixinAppTheme.colors.walletGreen
        }
    } else {
        if (quoteColorPref) {
            MixinAppTheme.colors.walletGreen
        } else {
            MixinAppTheme.colors.walletRed
        }
    }
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = marketSymbol,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$$formattedPrice",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = changeText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = changeColor
                )
            }

            CoilImage(
                model = market.iconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)) {
            CandleChart(
                symbol = marketSymbol,
                timeFrame = timeFrames[selectedTimeFrame]
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            timeFrames.forEachIndexed { index, timeFrame ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .then(
                            if (selectedTimeFrame == index) {
                                Modifier.background(MixinAppTheme.colors.backgroundWindow)
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onTimeFrameChange(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timeFrame,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedTimeFrame == index) {
                            MixinAppTheme.colors.textPrimary
                        } else {
                            MixinAppTheme.colors.textAssist
                        }
                    )
                }
            }
        }
    }
}
