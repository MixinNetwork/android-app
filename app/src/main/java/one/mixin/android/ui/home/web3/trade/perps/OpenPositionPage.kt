package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.home.web3.components.InputAction
import one.mixin.android.ui.home.web3.components.PageScaffold
import one.mixin.android.ui.home.web3.trade.InputContent
import one.mixin.android.ui.home.web3.trade.KeyboardAwareBox
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.home.web3.trade.TradeFragment
import one.mixin.android.ui.wallet.AddFeeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.components.MixinButton
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.roundToInt

private fun getLeveragePrefKey(marketId: String) = "pref_perps_leverage_$marketId"
private const val MARKET_REFRESH_INTERVAL_MS = 5_000L
private const val DEFAULT_LEVERAGE = 10

private fun readAcceptedPerpAssetIds(context: android.content.Context): List<String> {
    return context.defaultSharedPreferences
        .getString(Constants.Account.PREF_PERPS_ACCEPTED_ASSET_IDS_V2, null)
        .orEmpty()
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private fun TokenItem.hasPositiveBalance(): Boolean =
    (balance.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO

private fun resolveCurrentToken(
    selectedToken: TokenItem?,
    availableTokens: List<TokenItem>,
    preferredAssetIds: List<String>,
): TokenItem? {
    if (selectedToken == null) {
        return availableTokens.firstOrNull { it.hasPositiveBalance() }
            ?: preferredAssetIds.firstNotNullOfOrNull { assetId ->
                availableTokens.firstOrNull { it.assetId == assetId }
            }
            ?: availableTokens.firstOrNull()
    }

    val matchedToken = availableTokens.firstOrNull { it.assetId == selectedToken.assetId }
    return when {
        matchedToken != null -> matchedToken
        else -> selectedToken
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OpenPositionPage(
    market: PerpsMarket,
    isLong: Boolean,
    onBack: () -> Unit,
    onOpenSuccess: (String) -> Unit = { onBack() },
    selectedToken: TokenItem?,
    onTokenSelect: () -> Unit = {},
    onCurrentTokenChange: (TokenItem?) -> Unit = {},
) {
    val context = LocalContext.current
    val waitingOtherOrdersError = stringResource(R.string.error_waiting_other_orders)
    val dataError = stringResource(R.string.Data_error)
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val marketId = market.marketId
    val acceptedPerpAssetIdsOrdered = remember { readAcceptedPerpAssetIds(context) }
    val acceptedPerpAssetIds = remember(acceptedPerpAssetIdsOrdered) { acceptedPerpAssetIdsOrdered.toSet() }

    var currentMarket by remember(marketId) { mutableStateOf(market) }
    var currentToken by remember { mutableStateOf<TokenItem?>(selectedToken) }
    var availableTokens by remember { mutableStateOf<List<TokenItem>>(emptyList()) }
    var usdtAmount by remember { mutableStateOf("") }
    var takeProfitPrice by remember { mutableStateOf("") }
    var stopLossPrice by remember { mutableStateOf("") }
    var errorInfo by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val savedLeverage = remember(marketId) {
        context.defaultSharedPreferences
            .getInt(getLeveragePrefKey(marketId), DEFAULT_LEVERAGE)
            .coerceAtLeast(1)
    }
    var leverage by remember(marketId) { mutableFloatStateOf(savedLeverage.toFloat()) }

    LaunchedEffect(marketId) {
        while (true) {
            viewModel.loadMarketDetail(
                marketId = marketId,
                onSuccess = { data ->
                    currentMarket = data
                },
                onError = {}
            )
            delay(MARKET_REFRESH_INTERVAL_MS)
        }
    }

    LaunchedEffect(acceptedPerpAssetIds) {
        viewModel.loadUsdTokens { tokens ->
            val supportedTokens = if (acceptedPerpAssetIds.isEmpty()) {
                tokens
            } else {
                tokens.filter { it.assetId in acceptedPerpAssetIds }
            }
            val orderedSupportedTokens = if (acceptedPerpAssetIdsOrdered.isEmpty()) {
                supportedTokens
            } else {
                acceptedPerpAssetIdsOrdered.mapNotNull { assetId ->
                    supportedTokens.firstOrNull { it.assetId == assetId }
                }
            }
            availableTokens = orderedSupportedTokens
            currentToken = resolveCurrentToken(
                selectedToken = selectedToken,
                availableTokens = orderedSupportedTokens,
                preferredAssetIds = acceptedPerpAssetIdsOrdered,
            )
        }
    }

    LaunchedEffect(selectedToken?.assetId, availableTokens) {
        currentToken = resolveCurrentToken(
            selectedToken = selectedToken,
            availableTokens = availableTokens,
            preferredAssetIds = acceptedPerpAssetIdsOrdered,
        )
    }
    LaunchedEffect(currentToken) {
        onCurrentTokenChange(currentToken)
    }
    val maxLeverage = currentMarket.leverage.coerceAtLeast(1)
    LaunchedEffect(usdtAmount, leverage, currentToken?.assetId) {
        errorInfo = null
    }
    LaunchedEffect(maxLeverage, marketId) {
        val boundedLeverage = leverage.coerceIn(1f, maxLeverage.toFloat())
        if (boundedLeverage != leverage) {
            leverage = boundedLeverage
            context.defaultSharedPreferences.putInt(getLeveragePrefKey(marketId), boundedLeverage.toInt())
        }
    }

    val leverageOptions = generateLeverageOptions(maxLeverage)
    val inputAmount = usdtAmount.toBigDecimalOrNull()
    val tokenBalance = currentToken?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val minimumMargin = currentMarket.minAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val maximumMargin = currentMarket.maxAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val hasInputAmount = inputAmount != null && inputAmount > BigDecimal.ZERO
    val belowMinimumMargin = hasInputAmount && minimumMargin > BigDecimal.ZERO && inputAmount < minimumMargin
    val aboveMaximumMargin = hasInputAmount && maximumMargin > BigDecimal.ZERO && inputAmount > maximumMargin
    val insufficientBalance = hasInputAmount && inputAmount > tokenBalance
    val showAddAction = insufficientBalance || tokenBalance <= BigDecimal.ZERO
    val canReview = hasInputAmount && !belowMinimumMargin && !aboveMaximumMargin && !insufficientBalance
    val minimumMarginError = stringResource(
        R.string.perps_minimum_margin,
        minimumMargin.stripTrailingZeros().toPlainString()
    )
    val maximumMarginError = stringResource(
        R.string.perps_maximum_margin,
        maximumMargin.stripTrailingZeros().toPlainString()
    )
    val marginLimitError = when {
        belowMinimumMargin -> minimumMarginError
        aboveMaximumMargin -> maximumMarginError
        else -> null
    }
    val displayedErrorInfo = errorInfo?.takeIf { it.isNotBlank() } ?: marginLimitError
    val tokenNetworkName = currentToken?.chainName
        ?.takeIf { it.isNotBlank() }
        ?: currentToken?.chainSymbol
            ?.takeIf { it.isNotBlank() }
            ?: ""

    fun showPerpsGuide(tab: Int) {
        val activity = context as? FragmentActivity ?: return
        PerpetualGuideBottomSheetDialogFragment.newInstance(tab)
            .show(activity.supportFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
    }

    fun showTpSlBottomSheet(mode: PerpsTpSlBottomSheetDialogFragment.Mode) {
        val activity = context as? FragmentActivity ?: return
        PerpsTpSlBottomSheetDialogFragment.newInstance(
            mode = mode,
            price = if (mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT) {
                takeProfitPrice
            } else {
                stopLossPrice
            },
            currentPrice = currentMarket.last,
            isLong = isLong,
            marketIconUrl = currentMarket.iconUrl,
            marketSymbol = currentMarket.displaySymbol,
            marginAmount = usdtAmount,
            leverage = leverage.toInt(),
            entryPrice = null,
            marketId = currentMarket.marketId,
        ).setOnApply { value ->
            if (mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT) {
                takeProfitPrice = value.orEmpty()
            } else {
                stopLossPrice = value.orEmpty()
            }
        }.show(activity.supportFragmentManager, PerpsTpSlBottomSheetDialogFragment.TAG)
    }

    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.Open_Position),
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
            KeyboardAwareBox(
                modifier = Modifier.fillMaxHeight(),
                content = { _ ->
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
                    CoilImage(
                        model = currentMarket.iconUrl,
                        placeholder = R.drawable.ic_avatar_place_holder,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${if (isLong) stringResource(R.string.Long) else stringResource(R.string.Short)} ${currentMarket.tokenSymbol}",
                            fontSize = 16.sp,
                            color = MixinAppTheme.colors.textPrimary
                        )
                        Text(
                            text = stringResource(
                                R.string.Current_price,
                                formatPerpsPrice(currentMarket.last)
                            ),
                            fontSize = 13.sp,
                            color = MixinAppTheme.colors.textAssist
                        )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.Margin),
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (tokenNetworkName.isNotBlank()) {
                            Text(
                                text = tokenNetworkName,
                                fontSize = 12.sp,
                                color = MixinAppTheme.colors.textAssist,
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    InputContent(
                        token = currentToken?.toSwapToken(),
                        text = usdtAmount,
                        selectClick = {
                            onTokenSelect()
                        },
                        onInputChanged = { usdtAmount = it },
                        tokenIconSize = 25.dp,
                    )

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
                        if (showAddAction) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.Add),
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = MixinAppTheme.colors.accent,
                                ),
                                modifier = Modifier.clickable {
                                    val activity = context as? FragmentActivity ?: return@clickable
                                    val token = currentToken
                                    if (token == null) {
                                        onTokenSelect()
                                        return@clickable
                                    }
                                    AddFeeBottomSheetDialogFragment.newInstance(token)
                                        .apply {
                                            onAction = { type, addToken ->
                                                if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
                                                    val currentWalletId = Session.getAccountId() ?: ""
                                                    val preferenceKey = "${TradeFragment.PREF_TRADE_SELECTED_TAB_PREFIX}$currentWalletId"
                                                    context.defaultSharedPreferences.putInt(preferenceKey, 0)
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
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = currentToken?.name
                                ?.takeIf { it.isNotBlank() }
                                ?: "",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = MixinAppTheme.colors.textAssist,
                                textAlign = TextAlign.End,
                            ),
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
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MixinAppTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        leverageOptions.forEach { lev ->
                            val isSelected = when (lev) {
                                -1 -> {
                                    !leverageOptions.dropLast(1).contains(leverage.toInt())
                                }
                                maxLeverage -> {
                                    leverage.toInt() == maxLeverage
                                }
                                else -> {
                                    leverage.toInt() == lev
                                }
                            }

                            val displayText = when (lev) {
                                -1 -> stringResource(R.string.Custom)
                                maxLeverage.takeIf { it > 1 } -> stringResource(R.string.Max)
                                else -> "${lev}x"
                            }

                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MixinAppTheme.colors.accent else MixinAppTheme.colors.borderColor,
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
                    )

                    Text(
                        text = profitInfo,
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                }

                Spacer(modifier = Modifier.height(16.dp))


                Column(
                    modifier = Modifier
                        .fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    val quoteColorReversed = context.defaultSharedPreferences
                        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
                    val tpColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
                    val slColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
                    PerpsActionRow(
                        title = stringResource(R.string.Take_Profit),
                        value = takeProfitPrice.takeIf { it.isNotBlank() }?.let(::formatPerpsPrice),
                        valueColor = tpColor,
                        onClick = {
                            showTpSlBottomSheet(PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT)
                        },
                        onTipClick = {
                            showPerpsGuide(PerpetualGuideBottomSheetDialogFragment.TAB_TP_SL)
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PerpsActionRow(
                        title = stringResource(R.string.Stop_Loss),
                        value = stopLossPrice.takeIf { it.isNotBlank() }?.let(::formatPerpsPrice),
                        valueColor = slColor,
                        onClick = {
                            showTpSlBottomSheet(PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS)
                        },
                        onTipClick = {
                            showPerpsGuide(PerpetualGuideBottomSheetDialogFragment.TAB_TP_SL)
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PerpsInfoRow(
                        title = stringResource(R.string.position_size),
                        value = "${calculateOrderValue(usdtAmount, leverage, currentMarket.last)} ${currentMarket.tokenSymbol}",
                        onTipClick = {
                            showPerpsGuide(PerpetualGuideBottomSheetDialogFragment.TAB_POSITION)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PerpsInfoRow(
                        title = stringResource(R.string.Liquidation_Price),
                        value = calculateLiquidationPrice(
                            currentMarket.last,
                            leverage,
                            isLong,
                        ),
                        onTipClick = {
                            showPerpsGuide(PerpetualGuideBottomSheetDialogFragment.TAB_LIQUIDATION)
                        },
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(16.dp))

                if (displayedErrorInfo != null) {
                    Text(
                        text = displayedErrorInfo,
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.walletRed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                MixinButton(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        errorInfo = null
                        val token = currentToken ?: return@MixinButton
                        val amount = usdtAmount.toBigDecimalOrNull() ?: return@MixinButton

                        if (amount <= BigDecimal.ZERO) return@MixinButton
                        if (minimumMargin > BigDecimal.ZERO && amount < minimumMargin) {
                            errorInfo = minimumMarginError
                            return@MixinButton
                        }
                        if (maximumMargin > BigDecimal.ZERO && amount > maximumMargin) {
                            errorInfo = maximumMarginError
                            return@MixinButton
                        }
                        if (amount > (token.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO)) return@MixinButton

                        val m = currentMarket
                        val walletId = Session.getAccountId() ?: "" // Privacy Wallet
                        if (walletId.isEmpty()) return@MixinButton

                        val activity = context as? FragmentActivity ?: return@MixinButton

                        val price = m.last.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        if (price == BigDecimal.ZERO) return@MixinButton


                        scope.launch {
                            val hasOpeningPosition = viewModel.getOpenPositionsFromDb(walletId)
                                .any { it.marketId == m.marketId }
                            if (hasOpeningPosition) {
                                errorInfo = waitingOtherOrdersError
                                return@launch
                            }

                            viewModel.openPerpsOrder(
                                assetId = token.assetId,
                                marketId = m.marketId,
                                side = if (isLong) "long" else "short",
                                amount = amount.stripTrailingZeros().toPlainString(),
                                leverage = leverage.toInt(),
                                walletId = walletId,
                                takeProfitPrice = takeProfitPrice.takeIf { it.isNotBlank() },
                                stopLossPrice = stopLossPrice.takeIf { it.isNotBlank() },
                                entryPrice = m.last,
                                onSuccess = { response ->
                                    PerpsConfirmBottomSheetDialogFragment.newInstance(
                                        marketSymbol = m.displaySymbol,
                                        marketIcon = m.iconUrl,
                                        isLong = isLong,
                                        amount = response.payAmount,
                                        leverage = leverage.toInt(),
                                        entryPrice = m.last,
                                        tokenSymbol = token.symbol,
                                        takeProfitPrice = takeProfitPrice.takeIf { it.isNotBlank() },
                                        stopLossPrice = stopLossPrice.takeIf { it.isNotBlank() },
                                        payUrl = response.paymentUrl
                                    ).setOnDone {
                                            onOpenSuccess(m.marketId)
                                        }.show(activity.supportFragmentManager, PerpsConfirmBottomSheetDialogFragment.TAG)
                                    },
                                onError = { errorCode, errorMessage ->
                                    errorInfo = if (errorCode > 0) {
                                        context.getMixinErrorStringByCode(errorCode, errorMessage)
                                    } else {
                                        errorMessage.ifBlank { dataError }
                                    }
                                }
                            )
                        }
                    },
                    enabled = canReview,
                    backgroundColor = if (canReview) {
                        MixinAppTheme.colors.accent
                    } else {
                        MixinAppTheme.colors.backgroundGrayLight
                    },
                    contentColor = if (canReview) {
                        Color.White
                    } else {
                        MixinAppTheme.colors.textAssist
                    },
                    shape = RoundedCornerShape(32.dp),
                ) {
                    Text(
                        fontSize = 16.sp,
                        text = if (insufficientBalance) {
                            "${currentToken?.symbol ?: ""} ${stringResource(R.string.insufficient_balance)}"
                        } else {
                            stringResource(R.string.Review)
                        }
                    )
                }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                },
                floating = {
                    fun applyBalancePercent(percent: BigDecimal) {
                        if (tokenBalance > BigDecimal.ZERO) {
                            usdtAmount = tokenBalance
                                .multiply(percent)
                                .stripTrailingZeros()
                                .toPlainString()
                        } else {
                            usdtAmount = ""
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MixinAppTheme.colors.backgroundWindow)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InputAction("25%", showBorder = true) {
                            applyBalancePercent(BigDecimal("0.25"))
                        }
                        InputAction("50%", showBorder = true) {
                            applyBalancePercent(BigDecimal("0.5"))
                        }
                        InputAction("100%", showBorder = true) {
                            applyBalancePercent(BigDecimal.ONE)
                        }
                        InputAction(stringResource(R.string.Done), showBorder = false) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }
                }
            )
        }
    }

}

private fun generateLeverageOptions(maxLeverage: Int): List<Int> {
    val safeMaxLeverage = maxLeverage.coerceAtLeast(1)
    val baseOptions = listOf(5, 10, 20)
    val options = baseOptions
        .filter { it in 1 until safeMaxLeverage }
        .toMutableList()

    options.add(safeMaxLeverage)
    options.add(-1)

    return options.distinct()
}

@Composable
private fun calculateProfitInfo(
    amount: String,
    leverage: Float,
    isLong: Boolean,
    priceChangePercent: Double,
): String {
    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val leverageInt = leverage.roundToInt()
    if (amountValue == BigDecimal.ZERO) {
        return if (isLong) {
            stringResource(R.string.Price_Up_Profit, "1", leverageInt.toString(), formatPerpsRawUsdDecimal(BigDecimal.ZERO))
        } else {
            stringResource(R.string.Price_Down_Profit, "1", leverageInt.toString(), formatPerpsRawUsdDecimal(BigDecimal.ZERO))
        }
    }

    val profitPercent = leverageInt
    val profitAmount = amountValue
        .multiply(BigDecimal(profitPercent).divide(BigDecimal(100)))

    return if (isLong) {
        stringResource(
            R.string.Price_Up_Profit,
            String.format("%.0f", abs(priceChangePercent)),
            profitPercent.toString(),
            formatPerpsRawUsdDecimal(profitAmount)
        )
    } else {
        stringResource(
            R.string.Price_Down_Profit,
            String.format("%.0f", abs(priceChangePercent)),
            profitPercent.toString(),
            formatPerpsRawUsdDecimal(profitAmount)
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
): String {
    val price = currentPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO


    if (price == BigDecimal.ZERO) {
        return formatPerpsUsdDecimal(BigDecimal.ZERO)
    }

    val liquidationPercent = BigDecimal(100.0 / leverage)
    val liquidationRatio = liquidationPercent.divide(BigDecimal(100), 8, RoundingMode.HALF_UP)
    val liquidationPrice = if (isLong) {
        price * (BigDecimal.ONE - liquidationRatio)
    } else {
        price * (BigDecimal.ONE + liquidationRatio)
    }
    return formatPerpsUsdDecimal(liquidationPrice)
}

private fun formatPerpsPrice(
    rawPrice: String,
): String {
    val price = rawPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
    return "$PERPS_USD_SYMBOL${price.priceFormat()}"
}

@Composable
private fun PerpsActionRow(
    title: String,
    value: String?,
    valueColor: Color? = null,
    onClick: () -> Unit,
    onTipClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_tip),
                contentDescription = null,
                tint = MixinAppTheme.colors.textAssist,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onTipClick),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value ?: stringResource(R.string.Add),
                fontSize = 14.sp,
                color = if (value == null) MixinAppTheme.colors.accent else (valueColor ?: MixinAppTheme.colors.textPrimary),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MixinAppTheme.colors.textAssist,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun PerpsInfoRow(
    title: String,
    value: String,
    onTipClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
            )
            if (onTipClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_tip),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textAssist,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onTipClick),
                )
            }
        }
        Text(
            text = value,
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist,
            textAlign = TextAlign.End,
        )
    }
}
