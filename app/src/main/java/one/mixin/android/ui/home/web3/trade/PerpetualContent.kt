package one.mixin.android.ui.home.web3.trade

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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.compose.CoilImage
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.wallet.alert.components.cardBackground
import java.math.BigDecimal

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PerpetualContent(
    onLongClick: (PerpsMarket) -> Unit,
    onShortClick: (PerpsMarket) -> Unit,
    onShowTradingGuide: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val coroutineScope = rememberCoroutineScope()

    var markets by remember { mutableStateOf<List<PerpsMarket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var openPositionsCount by remember { mutableStateOf(0) }
    var totalPnl by remember { mutableStateOf(0.0) }
    
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    var isLongMode by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.loadMarkets(
            onSuccess = { data ->
                markets = data
                isLoading = false
            },
            onError = { error ->
                errorMessage = error
                isLoading = false
            }
        )
        
        val walletId = one.mixin.android.web3.js.Web3Signer.currentWalletId
        if (walletId.isNotEmpty()) {
            viewModel.getOpenPositions(walletId) { positions ->
                openPositionsCount = positions.size
            }
            
            viewModel.getTotalUnrealizedPnl(walletId) { pnl ->
                totalPnl = pnl
            }
        }
    }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MixinAppTheme.colors.background,
        sheetContent = {
            MarketListBottomSheetContent(
                markets = markets,
                onMarketClick = { market ->
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        PerpsActivity.showDetail(context, market.marketId, market.symbol)
                    }
                }
            )
        }
    ) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                    .padding(16.dp),
        ) {
            Text(
                text = "Total Position Value",
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$${String.format("%.2f", totalPnl)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.W600,
                color = MixinAppTheme.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$${String.format("%.2f", totalPnl)}",
                    fontSize = 14.sp,
                    color = if (totalPnl >= 0) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${if (totalPnl >= 0) "+" else ""}${String.format("%.1f", totalPnl)}%)",
                    fontSize = 14.sp,
                    color = if (totalPnl >= 0) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Open Positions($openPositionsCount)",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_empty_transaction),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Positions",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onShowTradingGuide()
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "How perps works?",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.accent,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(  Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
            .padding(16.dp)) {

            // Markets Section
            Text(
                text = "Markets",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MixinAppTheme.colors.textPrimary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading...",
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist,
                    )
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Error loading markets",
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.red,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                markets.take(2).forEach { market ->
                    MarketItem(
                        market = market,
                        onClick = {
                            PerpsActivity.showDetail(context, market.marketId, market.symbol)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Long and Short Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    isLongMode = true
                    coroutineScope.launch {
                        bottomSheetState.show()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ),
                enabled = markets.isNotEmpty()
            ) {
                Text(
                    text = "Long",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = {
                    isLongMode = false
                    coroutineScope.launch {
                        bottomSheetState.show()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFF44336),
                    contentColor = Color.White
                ),
                enabled = markets.isNotEmpty()
            ) {
                Text(
                    text = "Short",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
    }
}

@Composable
fun MarketItem(market: PerpsMarket, onClick: () -> Unit = {}) {
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

    // 使用 numberFormatCompact 格式化成交量
    val formattedVolume = try {
        BigDecimal(market.volume).numberFormatCompact()
    } catch (e: Exception) {
        market.volume
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            CoilImage(
                model = market.iconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = market.symbol,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MixinAppTheme.colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Vol $formattedVolume",
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "$$formattedPrice",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MixinAppTheme.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = changeText,
                fontSize = 12.sp,
                color = changeColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
