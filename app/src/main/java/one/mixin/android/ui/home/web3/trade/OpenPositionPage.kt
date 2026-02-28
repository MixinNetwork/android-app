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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormat8
import one.mixin.android.session.Session
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import kotlin.math.abs

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OpenPositionPage(
    marketId: String,
    marketSymbol: String,
    displaySymbol: String,
    isLong: Boolean,
    onBack: () -> Unit,
    onTokenSelect: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    var market by remember { mutableStateOf<PerpsMarket?>(null) }
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
        sheetState = bottomSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MixinAppTheme.colors.background,
        sheetContent = {
            LeverageBottomSheet(
                currentLeverage = leverage,
                maxLeverage = maxLeverage,
                onLeverageChange = {
                    leverage = it
                    coroutineScope.launch { bottomSheetState.hide() }
                }
            )
        }
    ) {
        MixinAppTheme {
            PageScaffold(
                title = stringResource(R.string.Open_Position),
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
                                    text = "${if (isLong) stringResource(R.string.Long) else stringResource(R.string.Short)} ${m.tokenSymbol}",
                                    fontSize = 16.sp,
                                    color = MixinAppTheme.colors.textPrimary
                                )
                                Text(
                                    text = "${stringResource(R.string.Current_price, "${m.markPrice} USD")} ",
                                    fontSize = 13.sp,
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
                            text = stringResource(R.string.Amount),
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InputContent(
                            token = selectedToken?.toSwapToken(),
                            text = usdtAmount,
                            selectClick = {
                                onTokenSelect()
                            },
                            onInputChanged = { usdtAmount = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_web3_wallet),
                                contentDescription = null,
                                tint = MixinAppTheme.colors.textAssist,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedToken?.balance?.numberFormat8() ?: "0",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = MixinAppTheme.colors.textAssist,
                                    textAlign = TextAlign.Start,
                                ),
                                modifier = Modifier.clickable {
                                    usdtAmount = selectedToken?.balance ?: "0"
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
                            .padding(16.dp)
                    ) {


                        Text(
                            text = stringResource(R.string.Leverage),
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
                                val isSelected = leverage.toInt() == lev
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { leverage = lev.toFloat() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${lev}x",
                                        fontSize = 12.sp,
                                        color = if (isSelected) MixinAppTheme.colors.accent else MixinAppTheme.colors.textPrimary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

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

                    }

                    Spacer(modifier = Modifier.height(16.dp))


                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = stringResource(R.string.Order_Value),
                                fontSize = 14.sp,
                                color = MixinAppTheme.colors.textAssist
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${calculateOrderValue(usdtAmount, leverage, market?.markPrice ?: "0")} ${market?.tokenSymbol}",
                                fontSize = 14.sp,
                                color = MixinAppTheme.colors.textAssist
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = stringResource(R.string.Liquidation_Price),
                                fontSize = 14.sp,
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
                                color = MixinAppTheme.colors.textAssist
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = {
                            val token = selectedToken ?: return@Button
                            val amount = usdtAmount.toBigDecimalOrNull() ?: return@Button

                            if (amount <= BigDecimal.ZERO) return@Button

                            val m = market ?: return@Button
                            val walletId = Session.getAccountId() ?: "" // Privacy Wallet
                            if (walletId.isEmpty()) return@Button

                            val activity = context as? FragmentActivity ?: return@Button

                            val price = m.markPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            if (price == BigDecimal.ZERO) return@Button

                            val orderValue = amount * BigDecimal(leverage.toDouble())

                            viewModel.openPerpsOrder(
                                assetId = token.assetId,
                                productId = marketId,
                                side = if (isLong) "long" else "short",
                                amount = orderValue.stripTrailingZeros().toPlainString(),
                                leverage = leverage.toInt(),
                                walletId = walletId,
                                marketSymbol = marketSymbol,
                                entryPrice = m.markPrice,
                                onSuccess = { response ->
                                    PerpsConfirmBottomSheetDialogFragment.newInstance(
                                        marketSymbol = m.displaySymbol,
                                        marketIcon = m.iconUrl,
                                        isLong = isLong,
                                        amount = response.payAmount ?: "",
                                        leverage = leverage.toInt(),
                                        entryPrice = m.markPrice,
                                        tokenSymbol = token.symbol,
                                        payUrl = response.payUrl
                                    ).setOnDone {
                                        onBack()
                                    }.show(activity.supportFragmentManager, PerpsConfirmBottomSheetDialogFragment.TAG)
                                },
                                onError = { error ->
                                    // TODO: Show error toast or dialog
                                }
                            )
                        },
                        enabled = usdtAmount.isNotBlank() && (usdtAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO,
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = if (usdtAmount.isNotBlank() && (usdtAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO) {
                                MixinAppTheme.colors.accent
                            } else {
                                MixinAppTheme.colors.backgroundGrayLight
                            }
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
                            text = stringResource(R.string.Review),
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
            text = stringResource(R.string.Select_Leverage),
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
                text = stringResource(R.string.Confirm),
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
    val baseOptions = listOf(1, 2, 5, 10, 20, 100)

    baseOptions.forEach { option ->
        if (option <= maxLeverage) {
            options.add(option)
        }
    }

    return options.take(7)
}

@Composable
private fun calculateProfitInfo(
    amount: String,
    leverage: Float,
    isLong: Boolean,
    priceChangePercent: Double,
): String {
    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    if (amountValue == BigDecimal.ZERO) {
        return if (isLong) {
            stringResource(R.string.Price_Up_Profit, "1", "0.0", "0.00")
        } else {
            stringResource(R.string.Price_Down_Profit, "1", "0.0", "0.00")
        }
    }

    val profitPercent = priceChangePercent * leverage
    val profitAmount = amountValue * BigDecimal(profitPercent / 100)

    return if (isLong) {
        stringResource(
            R.string.Price_Up_Profit,
            String.format("%.0f", abs(priceChangePercent)),
            String.format("%.1f", profitPercent),
            String.format("%.2f", profitAmount)
        )
    } else {
        stringResource(
            R.string.Price_Down_Profit,
            String.format("%.0f", abs(priceChangePercent)),
            String.format("%.1f", profitPercent),
            String.format("%.2f", profitAmount)
        )
    }
}

private fun calculateOrderValue(amount: String, leverage: Float, price: String): String {
    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val priceValue = price.toBigDecimalOrNull() ?: BigDecimal.ZERO


    if (priceValue == BigDecimal.ZERO) {
        return "0"
    }

    val orderValue = (amountValue * BigDecimal(leverage.toDouble())).divide(priceValue, 8, java.math.RoundingMode.HALF_UP)
    val result = orderValue.stripTrailingZeros().toPlainString()

    return result
}
private fun calculateLiquidationPrice(
    currentPrice: String,
    leverage: Float,
    isLong: Boolean,
): String {
    val price = currentPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
    

    if (price == BigDecimal.ZERO) {
        return "$0"
    }

    val liquidationPercent = BigDecimal(100.0 / leverage)
    val liquidationRatio = liquidationPercent.divide(BigDecimal(100), 8, java.math.RoundingMode.HALF_UP)
    val liquidationPrice = if (isLong) {
        price * (BigDecimal.ONE - liquidationRatio)
    } else {
        price * (BigDecimal.ONE + liquidationRatio)
    }

    val result = "$${String.format("%.2f", liquidationPrice)}"
    return result
}
