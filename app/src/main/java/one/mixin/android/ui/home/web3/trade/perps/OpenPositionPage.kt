package one.mixin.android.ui.home.web3.trade.perps

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
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
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.putInt
import one.mixin.android.session.Session
import one.mixin.android.ui.home.web3.trade.InputContent
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.wallet.AddFeeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

private fun getLeveragePrefKey(marketId: String) = "pref_perps_leverage_$marketId"

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OpenPositionPage(
    marketId: String,
    marketSymbol: String,
    displaySymbol: String,
    isLong: Boolean,
    onBack: () -> Unit,
    selectedToken: TokenItem? = null,
    onTokenSelect: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val acceptedPerpAssetIds = remember {
        context.defaultSharedPreferences
            .getStringSet(Constants.Account.PREF_PERPS_ACCEPTED_ASSET_IDS, emptySet())
            .orEmpty()
            .filter { it.isNotBlank() }
            .toSet()
    }

    var market by remember { mutableStateOf<PerpsMarket?>(null) }
    var currentToken by remember { mutableStateOf<TokenItem?>(selectedToken) }
    var availableTokens by remember { mutableStateOf<List<TokenItem>>(emptyList()) }
    var usdtAmount by remember { mutableStateOf("") }

    val savedLeverage = context.defaultSharedPreferences.getInt(getLeveragePrefKey(marketId), 10)
    var leverage by remember { mutableFloatStateOf(savedLeverage.toFloat()) }

    LaunchedEffect(marketId) {
        viewModel.loadMarketDetail(
            marketId = marketId,
            onSuccess = { data ->
                market = data
            },
            onError = {}
        )

        viewModel.loadUsdTokens { tokens ->
            val supportedTokens = tokens.filter { it.assetId in acceptedPerpAssetIds }
            availableTokens = supportedTokens
            currentToken = selectedToken?.let { target ->
                supportedTokens.firstOrNull { it.assetId == target.assetId }
            } ?: supportedTokens.firstOrNull()
        }
    }

    LaunchedEffect(selectedToken?.assetId, availableTokens) {
        selectedToken?.let { target ->
            currentToken = availableTokens.firstOrNull { it.assetId == target.assetId } ?: target
        }
    }

    val maxLeverage = market?.leverage ?: 100
    val leverageOptions = generateLeverageOptions(maxLeverage)
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val directionColor = if (isLong) risingColor else fallingColor
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()
    val inputAmount = usdtAmount.toBigDecimalOrNull()
    val tokenBalance = currentToken?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val hasInputAmount = inputAmount != null && inputAmount > BigDecimal.ZERO
    val insufficientBalance = hasInputAmount && inputAmount > tokenBalance
    val canReview = hasInputAmount && !insufficientBalance

    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.Open_Position),
            subtitleText = stringResource(R.string.Perpetual),
            verticalScrollable = false,
            pop = onBack,
            actions = {
                IconButton(onClick = {
                    context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_support),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.icon,
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
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
                                text = stringResource(
                                    R.string.Current_price,
                                    formatFiatPrice(m.markPrice, fiatRate, fiatSymbol)
                                ),
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
                        token = currentToken?.toSwapToken(),
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
                            text = currentToken?.balance?.numberFormat8() ?: "0",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = MixinAppTheme.colors.textAssist,
                                textAlign = TextAlign.Start,
                            ),
                            modifier = Modifier.clickable {
                                usdtAmount = currentToken?.balance ?: "0"
                            }
                        )
                        if (insufficientBalance) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.Add),
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = MixinAppTheme.colors.accent,
                                ),
                                modifier = Modifier.clickable {
                                    val activity = context as? FragmentActivity ?: return@clickable
                                    val token = currentToken ?: return@clickable
                                    AddFeeBottomSheetDialogFragment.newInstance(token)
                                        .apply {
                                            onAction = { type, addToken ->
                                                if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
                                                    SwapActivity.show(
                                                        context = activity,
                                                        input = Constants.AssetId.USDT_ASSET_ETH_ID,
                                                        output = addToken.assetId,
                                                        amount = null,
                                                        referral = null
                                                    )
                                                } else if (type == AddFeeBottomSheetDialogFragment.ActionType.DEPOSIT) {
                                                    WalletActivity.showDeposit(activity, addToken)
                                                }
                                            }
                                        }
                                        .show(activity.supportFragmentManager, AddFeeBottomSheetDialogFragment.TAG)
                                }
                            )
                        }
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
                            val activity = context as? FragmentActivity ?: return@clickable
                            LeverageBottomSheetDialogFragment.newInstance(
                                currentLeverage = leverage,
                                maxLeverage = maxLeverage,
                                amount = usdtAmount,
                                isLong = isLong
                            ).setOnLeverageSelected { newLeverage ->
                                leverage = newLeverage
                                context.defaultSharedPreferences.putInt(getLeveragePrefKey(marketId), newLeverage.toInt())
                            }.show(activity.supportFragmentManager, LeverageBottomSheetDialogFragment.TAG)
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
                            val isSelected = if (lev == -1) {
                                !leverageOptions.dropLast(1).contains(leverage.toInt())
                            } else if (lev == maxLeverage) {
                                leverage.toInt() == maxLeverage
                            } else {
                                leverage.toInt() == lev
                            }

                            val displayText = when (lev) {
                                -1 -> stringResource(R.string.slippage_custom)
                                maxLeverage -> stringResource(R.string.Max)
                                else -> "${lev}x"
                            }

                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        if (lev == -1) {
                                            val activity = context as? FragmentActivity ?: return@clickable
                                            LeverageBottomSheetDialogFragment.newInstance(
                                                currentLeverage = leverage,
                                                maxLeverage = maxLeverage,
                                                amount = usdtAmount,
                                                isLong = isLong
                                            ).setOnLeverageSelected { newLeverage ->
                                                leverage = newLeverage
                                                context.defaultSharedPreferences.putInt(getLeveragePrefKey(marketId), newLeverage.toInt())
                                            }.show(activity.supportFragmentManager, LeverageBottomSheetDialogFragment.TAG)
                                        } else {
                                            leverage = lev.toFloat()
                                            context.defaultSharedPreferences.putInt(getLeveragePrefKey(marketId), lev)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp)
                                        .widthIn(min = 20.dp),
                                    textAlign = TextAlign.Center,
                                    text = displayText,
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
                        priceChangePercent = 1.0,
                        fiatRate = fiatRate,
                        fiatSymbol = fiatSymbol,
                    )

                    Text(
                        text = profitInfo,
                        fontSize = 13.sp,
                        color = MixinAppTheme.colors.textMinor,
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
                        Row (verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.Order_Value),
                                fontSize = 14.sp,
                                color = MixinAppTheme.colors.textAssist
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tip),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable {
                                        val activity = context as? FragmentActivity ?: return@clickable
                                        PerpetualGuideFragment.newInstance()
                                            .show(activity.supportFragmentManager, PerpetualGuideFragment.TAG)
                                    },
                                tint = MixinAppTheme.colors.textAssist
                            )
                        }
                        Text(
                            text = "${calculateOrderValue(usdtAmount, leverage, market?.markPrice ?: "0")} ${market?.tokenSymbol}",
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.textAssist
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row (verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.Liquidation_Price),
                                fontSize = 14.sp,
                                color = MixinAppTheme.colors.textAssist
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tip),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable {
                                        val activity = context as? FragmentActivity ?: return@clickable
                                        PerpetualGuideFragment.newInstance()
                                            .show(activity.supportFragmentManager, PerpetualGuideFragment.TAG)
                                    },
                                tint = MixinAppTheme.colors.textAssist
                            )
                        }
                        Text(
                            text = calculateLiquidationPrice(
                                market?.markPrice ?: "0",
                                leverage,
                                isLong,
                                fiatRate,
                                fiatSymbol,
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
                        val token = currentToken ?: return@Button
                        val amount = usdtAmount.toBigDecimalOrNull() ?: return@Button

                        if (amount <= BigDecimal.ZERO) return@Button
                        if (amount > (token.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO)) return@Button

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
                    enabled = canReview,
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (canReview) {
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
                        text = if (insufficientBalance) {
                            "${currentToken?.symbol ?: ""} ${stringResource(R.string.insufficient_balance)}"
                        } else {
                            stringResource(R.string.Review)
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canReview) {
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

private fun generateLeverageOptions(maxLeverage: Int): List<Int> {
    val options = mutableListOf<Int>()
    val baseOptions = listOf(1, 2, 5, 10, 20)

    baseOptions.forEach { option ->
        if (option < maxLeverage) {
            options.add(option)
        }
    }

    if (options.size < 5) {
        options.add(maxLeverage)
    }

    options.add(-1)

    return options.take(7)
}

@Composable
private fun calculateProfitInfo(
    amount: String,
    leverage: Float,
    isLong: Boolean,
    priceChangePercent: Double,
    fiatRate: BigDecimal,
    fiatSymbol: String,
): String {
    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    if (amountValue == BigDecimal.ZERO) {
        return if (isLong) {
            stringResource(R.string.Price_Up_Profit, "1", "0.0", "${fiatSymbol}0.00")
        } else {
            stringResource(R.string.Price_Down_Profit, "1", "0.0", "${fiatSymbol}0.00")
        }
    }

    val profitPercent = priceChangePercent * leverage
    val profitAmount = amountValue
        .multiply(BigDecimal(profitPercent / 100))
        .multiply(fiatRate)

    return if (isLong) {
        stringResource(
            R.string.Price_Up_Profit,
            String.format("%.0f", abs(priceChangePercent)),
            String.format("%.1f", profitPercent),
            "${fiatSymbol}${profitAmount.priceFormat()}"
        )
    } else {
        stringResource(
            R.string.Price_Down_Profit,
            String.format("%.0f", abs(priceChangePercent)),
            String.format("%.1f", profitPercent),
            "${fiatSymbol}${profitAmount.priceFormat()}"
        )
    }
}

private fun calculateOrderValue(amount: String, leverage: Float, price: String): String {
    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val priceValue = price.toBigDecimalOrNull() ?: BigDecimal.ZERO


    if (priceValue == BigDecimal.ZERO) {
        return "0"
    }

    val orderValue = (amountValue * BigDecimal(leverage.toDouble())).divide(priceValue, 8, RoundingMode.HALF_UP)
    val result = orderValue.stripTrailingZeros().toPlainString()

    return result
}

private fun calculateLiquidationPrice(
    currentPrice: String,
    leverage: Float,
    isLong: Boolean,
    fiatRate: BigDecimal,
    fiatSymbol: String,
): String {
    val price = currentPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO


    if (price == BigDecimal.ZERO) {
        return "${fiatSymbol}0"
    }

    val liquidationPercent = BigDecimal(100.0 / leverage)
    val liquidationRatio = liquidationPercent.divide(BigDecimal(100), 8, RoundingMode.HALF_UP)
    val liquidationPrice = if (isLong) {
        price * (BigDecimal.ONE - liquidationRatio)
    } else {
        price * (BigDecimal.ONE + liquidationRatio)
    }
    val fiatLiquidationPrice = liquidationPrice.multiply(fiatRate)
    return "${fiatSymbol}${fiatLiquidationPrice.priceFormat()}"
}

private fun formatFiatPrice(
    rawPrice: String,
    fiatRate: BigDecimal,
    fiatSymbol: String,
): String {
    val price = rawPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
    return "${fiatSymbol}${price.multiply(fiatRate).priceFormat()}"
}
