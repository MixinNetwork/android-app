package one.mixin.android.ui.home.web3.trade

import PageScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import one.mixin.android.R
import one.mixin.android.api.response.perps.MarketView
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import kotlin.math.abs

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OpenPositionPage(
    marketId: String,
    marketSymbol: String,
    isLong: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val tokenBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    var market by remember { mutableStateOf<MarketView?>(null) }
    var selectedToken by remember { mutableStateOf<TokenItem?>(null) }
    var availableTokens by remember { mutableStateOf<List<TokenItem>>(emptyList()) }
    var usdtAmount by remember { mutableStateOf("") }
    var leverage by remember { mutableFloatStateOf(10f) }

    LaunchedEffect(marketId) {
        viewModel.loadMarketDetail(
            marketId = marketId,
            onSuccess = { data ->
                market = data
                if (data.leverage > 0) {
                    leverage = minOf(10f, data.leverage.toFloat())
                }
            },
            onError = {}
        )

        viewModel.loadUsdTokens { tokens ->
            availableTokens = tokens
            selectedToken = tokens.firstOrNull()
        }
    }

    val maxLeverage = market?.leverage ?: 100
    val leverageOptions = generateLeverageOptions(maxLeverage)

    ModalBottomSheetLayout(
        sheetState = if (tokenBottomSheetState.isVisible) tokenBottomSheetState else bottomSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MixinAppTheme.colors.background,
        sheetContent = {
            if (tokenBottomSheetState.isVisible) {
                TokenSelectionBottomSheet(
                    tokens = availableTokens,
                    selectedToken = selectedToken,
                    onTokenSelect = { token ->
                        selectedToken = token
                        coroutineScope.launch { tokenBottomSheetState.hide() }
                    }
                )
            } else {
                LeverageBottomSheet(
                    currentLeverage = leverage,
                    maxLeverage = maxLeverage,
                    onLeverageChange = {
                        leverage = it
                        coroutineScope.launch { bottomSheetState.hide() }
                    }
                )
            }
        }
    ) {
        PageScaffold(
            title = "Open Position",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    market?.let { m ->
                        CoilImage(
                            model = m.iconUrl,
                            placeholder = R.drawable.ic_avatar_place_holder,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${if (isLong) "Long" else "Short"} $marketSymbol",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MixinAppTheme.colors.textPrimary
                            )
                            Text(
                                text = "$${m.markPrice}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
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
                        text = "Amount",
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material.TextField(
                            value = usdtAmount,
                            onValueChange = { usdtAmount = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("0.00") },
                            colors = androidx.compose.material.TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MixinAppTheme.colors.textPrimary
                            )
                        )

                        Row(
                            modifier = Modifier.clickable {
                                coroutineScope.launch { tokenBottomSheetState.show() }
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            selectedToken?.let { token ->
                                CoilImage(
                                    model = token.iconUrl,
                                    placeholder = R.drawable.ic_avatar_place_holder,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = selectedToken?.symbol ?: "USDT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MixinAppTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_down_info),
                                contentDescription = null,
                                tint = MixinAppTheme.colors.textAssist,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Balance: ${selectedToken?.balance ?: "0"} ${selectedToken?.symbol ?: ""}",
                            fontSize = 12.sp,
                            color = MixinAppTheme.colors.textAssist
                        )

                        Text(
                            text = "MAX",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MixinAppTheme.colors.accent,
                            modifier = Modifier.clickable {
                                usdtAmount = selectedToken?.balance ?: "0"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))


                    Text(
                        text = "Leverage",
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textPrimary
                    )


                    Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            modifier = Modifier.clickable {
                                coroutineScope.launch { bottomSheetState.show() }
                            },
                            text = "${leverage.toInt()}x",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MixinAppTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        leverageOptions.forEach { lev ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        width = 1.dp,
                                        color = MixinAppTheme.colors.textAssist,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { leverage = lev.toFloat() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${lev}x",
                                    fontSize = 12.sp,
                                    color = MixinAppTheme.colors.textPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val profitInfo = calculateProfitInfo(
                    amount = usdtAmount,
                    leverage = leverage,
                    isLong = isLong,
                    priceChangePercent = 1.0
                )

                Text(
                    text = profitInfo,
                    fontSize = 13.sp,
                    color = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Order Value",
                            fontSize = 12.sp,
                            color = MixinAppTheme.colors.textAssist
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = calculateOrderValue(usdtAmount, leverage),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MixinAppTheme.colors.textPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Liquidation Price",
                            fontSize = 12.sp,
                            color = MixinAppTheme.colors.textAssist
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = calculateLiquidationPrice(
                                market?.markPrice ?: "0",
                                leverage,
                                isLong
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MixinAppTheme.colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material.Button(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        // TODO: Open position
                    },
                    enabled = usdtAmount.isNotBlank() && (usdtAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO,
                    colors = androidx.compose.material.ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (usdtAmount.isNotBlank() && (usdtAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO) {
                            if (isLong) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
                        } else {
                            MixinAppTheme.colors.backgroundGrayLight
                        }
                    ),
                    shape = RoundedCornerShape(32.dp),
                    elevation = androidx.compose.material.ButtonDefaults.elevation(
                        pressedElevation = 0.dp,
                        defaultElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = "Review",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (usdtAmount.isNotBlank() && (usdtAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO) {
                            Color.White
                        } else {
                            MixinAppTheme.colors.textAssist
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TokenSelectionBottomSheet(
    tokens: List<TokenItem>,
    selectedToken: TokenItem?,
    onTokenSelect: (TokenItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Token",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MixinAppTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        tokens.forEach { token ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTokenSelect(token) }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoilImage(
                    model = token.iconUrl,
                    placeholder = R.drawable.ic_avatar_place_holder,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = token.symbol,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MixinAppTheme.colors.textPrimary
                    )
                    Text(
                        text = token.chainName ?: "",
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist
                    )
                }

                Text(
                    text = token.balance,
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary
                )

                if (selectedToken?.assetId == token.assetId) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LeverageBottomSheet(
    currentLeverage: Float,
    maxLeverage: Int,
    onLeverageChange: (Float) -> Unit,
) {
    var tempLeverage by remember { mutableFloatStateOf(currentLeverage) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Leverage",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MixinAppTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "${tempLeverage.toInt()}x",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MixinAppTheme.colors.textPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Slider(
            value = tempLeverage,
            onValueChange = { tempLeverage = it },
            valueRange = 1f..maxLeverage.toFloat(),
            steps = maxLeverage - 2,
            colors = SliderDefaults.colors(
                thumbColor = MixinAppTheme.colors.accent,
                activeTrackColor = MixinAppTheme.colors.accent,
                inactiveTrackColor = MixinAppTheme.colors.backgroundWindow
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "1x",
                fontSize = 12.sp,
                color = MixinAppTheme.colors.textAssist
            )
            Text(
                text = "${maxLeverage}x",
                fontSize = 12.sp,
                color = MixinAppTheme.colors.textAssist
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MixinAppTheme.colors.accent)
                .clickable { onLeverageChange(tempLeverage) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Confirm",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun generateLeverageOptions(maxLeverage: Int): List<Int> {
    val options = mutableListOf<Int>()
    val baseOptions = listOf(1, 2, 5, 10, 25, 50, 100)

    baseOptions.forEach { option ->
        if (option <= maxLeverage) {
            options.add(option)
        }
    }

    return options.take(7)
}

private fun calculateProfitInfo(
    amount: String,
    leverage: Float,
    isLong: Boolean,
    priceChangePercent: Double,
): String {
    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    if (amountValue == BigDecimal.ZERO) return "Price up 1% → Profit 0% (+$0.00)"

    val profitPercent = priceChangePercent * leverage
    val profitAmount = amountValue * BigDecimal(profitPercent / 100)

    val direction = if (isLong) "up" else "down"
    val sign = if (profitAmount >= BigDecimal.ZERO) "+" else ""

    return "Price $direction ${String.format("%.0f", abs(priceChangePercent))}% → Profit ${sign}${String.format("%.1f", profitPercent)}% (${sign}$${String.format("%.2f", profitAmount)})"
}

private fun calculateOrderValue(amount: String, leverage: Float): String {
    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val orderValue = amountValue * BigDecimal(leverage.toDouble())
    return "$${String.format("%.2f", orderValue)}"
}

private fun calculateLiquidationPrice(
    currentPrice: String,
    leverage: Float,
    isLong: Boolean,
): String {
    val price = currentPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
    if (price == BigDecimal.ZERO) return "$0"

    val liquidationPercent = BigDecimal(100.0 / leverage)
    val liquidationPrice = if (isLong) {
        price * (BigDecimal.ONE - liquidationPercent / BigDecimal(100))
    } else {
        price * (BigDecimal.ONE + liquidationPercent / BigDecimal(100))
    }

    return "$${String.format("%.2f", liquidationPrice)}"
}
