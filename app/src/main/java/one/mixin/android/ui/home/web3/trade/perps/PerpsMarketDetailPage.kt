package one.mixin.android.ui.home.web3.trade.perps

import android.text.Layout
import android.util.TypedValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsOrder
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.toPosition
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.home.web3.components.PageScaffold
import one.mixin.android.ui.home.web3.trade.CandleChart
import one.mixin.android.ui.wallet.MarketDescriptionTextView
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.selectLocalizedMarketDescription
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.widget.components.MixinButton
import java.math.BigDecimal
import java.util.Locale

private const val CLOSED_POSITION_PREVIEW_LIMIT = 100
private const val MARKET_REFRESH_INTERVAL_MS = 10_000L
private const val PREF_MARKET_DETAIL_TIME_FRAME = "perps_market_detail_time_frame"

@Composable
fun PerpsMarketDetailPage(
    marketId: String,
    marketSymbol: String,
    displaySymbol: String,
    tokenSymbol: String,
    initialMarket: PerpsMarket? = null,
    onBack: () -> Unit,
    onSharePosition: (PerpsPositionItem) -> Unit,
    source: String,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val preferences = remember(context) { context.defaultSharedPreferences }
    val lifecycleOwner = LocalLifecycleOwner.current
    var market by remember(marketId, initialMarket) { mutableStateOf(initialMarket) }
    var isLoading by remember(marketId, initialMarket) { mutableStateOf(initialMarket == null) }
    val timeFramePreferenceKey = PREF_MARKET_DETAIL_TIME_FRAME
    val walletId = Session.getAccountId().orEmpty()
    val openPositions by remember(walletId) {
        if (walletId.isNotEmpty()) {
            viewModel.observeOpenPositions(walletId).map { it as List<PerpsPositionItem>? }
        } else {
            flowOf(emptyList<PerpsPositionItem>())
        }
    }.collectAsStateWithLifecycle(initialValue = null)
    val allClosedPositions by remember(walletId) {
        if (walletId.isNotEmpty()) {
            viewModel.observeOrders(walletId, CLOSED_POSITION_PREVIEW_LIMIT)
        } else {
            flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    var previousOpenPositionsCount by remember(walletId) { mutableStateOf<Int?>(null) }
    var isAddingProcessing by remember { mutableStateOf(false) }
    val currentPosition = openPositions?.firstOrNull { it.marketId == marketId }
    val hasLoadedOpenPositions = openPositions != null
    val closedPositions = allClosedPositions.filter { it.marketId == marketId }
    val timeFrameValues = listOf("1m", "5m", "15m", "1h", "4h", "1d", "1w")
    val timeFrameLabels = listOf(
        stringResource(R.string.minutes_count_short, 1),
        stringResource(R.string.minutes_count_short, 5),
        stringResource(R.string.minutes_count_short, 15),
        stringResource(R.string.hours_count_short, 1),
        stringResource(R.string.hours_count_short, 4),
        stringResource(R.string.days_count_short, 1),
        stringResource(R.string.weeks_count_short, 1),
    )
    var selectedTimeFrame by remember(timeFramePreferenceKey, timeFrameValues.size) {
        val savedIndex = preferences.getInt(timeFramePreferenceKey, 0)
        mutableIntStateOf(savedIndex.takeIf { it in timeFrameValues.indices } ?: 0)
    }
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed

    LaunchedEffect(marketId, walletId, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (walletId.isNotEmpty()) {
                viewModel.startRefreshOrders(walletId, intervalMs = MARKET_REFRESH_INTERVAL_MS)
            }
            try {
                while (isActive) {
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
                    delay(MARKET_REFRESH_INTERVAL_MS)
                }
            } finally {
                viewModel.stopRefreshOrders()
            }
        }
    }

    LaunchedEffect(walletId, openPositions?.size) {
        if (walletId.isEmpty()) return@LaunchedEffect
        val lastCount = previousOpenPositionsCount
        val currentCount = openPositions?.size ?: return@LaunchedEffect
        if (lastCount != null && currentCount < lastCount) {
            viewModel.refreshOrders(walletId, limit = CLOSED_POSITION_PREVIEW_LIMIT)
        }
        previousOpenPositionsCount = currentCount
    }

    PageScaffold(
        title = displaySymbol,
        subtitleText = stringResource(R.string.Perpetual),
        verticalScrollable = false,
        pop = onBack,
        actions = {
            IconButton(onClick = {
                context.openUrl(
                    Constants.HelpLink.CUSTOMER_SERVICE,
                    source = AnalyticsTracker.CustomerServiceSource.PERPS_MARKET_DETAIL,
                    wallet = AnalyticsTracker.TradeWallet.WEB3,
                )
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_support),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 80.dp)
            ) {

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
                            marketId = marketId,
                            displaySymbol = displaySymbol,
                            tokenSymbol = tokenSymbol,
                            selectedTimeFrame = selectedTimeFrame,
                            timeFrameValues = timeFrameValues,
                            timeFrameLabels = timeFrameLabels,
                            onTimeFrameChange = { index ->
                                selectedTimeFrame = index
                                preferences.putInt(timeFramePreferenceKey, index)
                            }
                        )
                    } else if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp),
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

                if (currentPosition != null && market != null) {
                    val pnl = currentPosition.unrealizedPnl?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val hasTakeProfit = !currentPosition.takeProfitPrice.isNullOrBlank()
                    val hasStopLoss = !currentPosition.stopLossPrice.isNullOrBlank()
                    var hideTakeProfitGuideUntil by remember(preferences) {
                        mutableStateOf(preferences.getTpSlGuideHideUntil(TpSlGuideType.TAKE_PROFIT))
                    }
                    var hideStopLossGuideUntil by remember(preferences) {
                        mutableStateOf(preferences.getTpSlGuideHideUntil(TpSlGuideType.STOP_LOSS))
                    }
                    var guideType by remember(currentPosition.positionId) {
                        mutableStateOf(
                            resolveTpSlGuideType(
                                pnl = pnl,
                                hasTakeProfit = hasTakeProfit,
                                hasStopLoss = hasStopLoss,
                                hideTakeProfitGuideUntil = hideTakeProfitGuideUntil,
                                hideStopLossGuideUntil = hideStopLossGuideUntil,
                                now = System.currentTimeMillis(),
                            )
                        )
                    }

                    LaunchedEffect(hasTakeProfit, hasStopLoss, guideType) {
                        if ((guideType == TpSlGuideType.TAKE_PROFIT && hasTakeProfit) ||
                            (guideType == TpSlGuideType.STOP_LOSS && hasStopLoss)
                        ) {
                            guideType = null
                        }
                    }

                    fun showTpSlBottomSheetFromGuide(mode: PerpsTpSlBottomSheetDialogFragment.Mode) {
                        val activity = context as? FragmentActivity ?: return
                        val existingPrice = when (mode) {
                            PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT -> currentPosition.takeProfitPrice.orEmpty()
                            PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS -> currentPosition.stopLossPrice.orEmpty()
                        }
                        val currentPrice = currentPosition.markPrice.orEmpty().ifBlank { currentPosition.entryPrice }
                        val requestMarginAmount = currentPosition.margin ?: currentPosition.openPayAmount.orEmpty()
                        PerpsTpSlBottomSheetDialogFragment.newInstance(
                            mode = mode,
                            price = existingPrice,
                            currentPrice = currentPrice,
                            isLong = currentPosition.side.equals("long", ignoreCase = true),
                            marketIconUrl = currentPosition.iconUrl.orEmpty(),
                            marketSymbol = currentPosition.tokenSymbol ?: "",
                            marginAmount = requestMarginAmount,
                            leverage = currentPosition.leverage,
                            entryPrice = currentPosition.entryPrice,
                            marketId = currentPosition.marketId,
                            priceScale = currentPosition.priceScale,
                        ).setOnApply { value ->
                            val normalizedValue = value?.trim().orEmpty()
                            if (normalizedValue == existingPrice) return@setOnApply
                            if (normalizedValue.isEmpty() && existingPrice.isEmpty()) return@setOnApply
                            val requestedValue = normalizedValue.ifEmpty { "" }
                            // TP/SL update API treats null as "keep existing value" and empty string as "clear this side".
                            viewModel.setPositionTpSl(
                                positionId = currentPosition.positionId,
                                takeProfitPrice = when (mode) {
                                    PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT -> requestedValue
                                    PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS -> null
                                },
                                stopLossPrice = when (mode) {
                                    PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT -> null
                                    PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS -> requestedValue
                                },
                                onSuccess = {
                                    toast(context.getString(R.string.Successful))
                                },
                                onError = { errorCode, errorMessage ->
                                    toast(context.getMixinErrorStringByCode(errorCode, errorMessage))
                                },
                            )
                        }.show(activity.supportFragmentManager, PerpsTpSlBottomSheetDialogFragment.TAG)
                    }

                    guideType?.let { currentGuideType ->
                        PerpsTpSlGuideCard(
                            guideType = currentGuideType,
                            onClose = {
                                val until = preferences.hideTpSlGuide(currentGuideType)
                                if (currentGuideType == TpSlGuideType.TAKE_PROFIT) {
                                    hideTakeProfitGuideUntil = until
                                } else {
                                    hideStopLossGuideUntil = until
                                }
                                guideType = null
                            },
                            actionText = stringResource(
                                if (currentGuideType == TpSlGuideType.TAKE_PROFIT) {
                                    R.string.Take_Profit
                                } else {
                                    R.string.Stop_Loss
                                }
                            ),
                            onActionClick = {
                                showTpSlBottomSheetFromGuide(
                                    if (currentGuideType == TpSlGuideType.TAKE_PROFIT) {
                                        PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT
                                    } else {
                                        PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS
                                    }
                                )
                            },
                            layout = PerpsTpSlGuideCardLayout.DETAIL,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    OpenPositionCard(
                        position = currentPosition,
                        viewModel = viewModel,
                        onShare = {
                            onSharePosition(currentPosition)
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (market != null) {
                    if (currentPosition == null) {
                        HowPerpsWorksCard(
                            onLearnClick = {
                                AnalyticsTracker.trackPerpsGuide(AnalyticsTracker.PerpsSource.PERPS_DETAIL_CARD)
                                val activity = context as? FragmentActivity ?: return@HowPerpsWorksCard
                                PerpetualGuideBottomSheetDialogFragment.newInstance(
                                    PerpetualGuideBottomSheetDialogFragment.TAB_OVERVIEW
                                ).show(activity.supportFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    MarketInfoCard(
                        market = market!!,
                        onFundingRateTipClick = {
                            val activity = context as? FragmentActivity ?: return@MarketInfoCard
                            PerpetualGuideBottomSheetDialogFragment.newInstance(
                                PerpetualGuideBottomSheetDialogFragment.TAB_FUNDING_RATE
                            ).show(activity.supportFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                        }
                    )
                }

                if (closedPositions.isNotEmpty() && market != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ClosedPositionsSection(
                        positions = closedPositions,
                        onViewAll = {
                            val activity = context as? FragmentActivity ?: return@ClosedPositionsSection
                            activity.supportFragmentManager.navigateToPerpsRoute(
                                AllPositionsFragment.newClosedInstance(AnalyticsTracker.PerpsSource.PERPS_MARKET_DETAIL),
                                AllPositionsFragment.TAG,
                                android.R.id.content,
                                animate = false,
                            )
                        },
                        onPositionClick = { position ->
                            val activity = context as? FragmentActivity ?: return@ClosedPositionsSection
                            activity.supportFragmentManager.navigateToPerpsRoute(
                                PositionDetailFragment.newInstance(position, AnalyticsTracker.PerpsSource.PERPS_MARKET_DETAIL),
                                PositionDetailFragment.TAG,
                                android.R.id.content,
                            )
                        }
                    )
                }

                if (currentPosition != null && market != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HowPerpsWorksCard(
                        onLearnClick = {
                            AnalyticsTracker.trackPerpsGuide(AnalyticsTracker.PerpsSource.PERPS_DETAIL_CARD)
                            val activity = context as? FragmentActivity ?: return@HowPerpsWorksCard
                            PerpetualGuideBottomSheetDialogFragment.newInstance(
                                PerpetualGuideBottomSheetDialogFragment.TAB_OVERVIEW
                            ).show(activity.supportFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                        }
                    )
                }

                val description = market?.descriptions?.let { descriptions ->
                    selectLocalizedMarketDescription(descriptions, Locale.getDefault().language)
                }
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MarketAboutSection(description)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (market != null && hasLoadedOpenPositions) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MixinAppTheme.colors.background)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 20.dp, top = 20.dp)
                ) {
                    if (currentPosition != null) {
                        val isOpen = currentPosition.state == PerpsPosition.STATE_OPEN
                        val isAdding = currentPosition.state == PerpsPosition.STATE_ADDING
                        val isPending = currentPosition.state == PerpsPosition.STATE_OPENING || isAdding
                        if (isPending) {
                            MixinButton(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .fillMaxWidth()
                                    .height(48.dp),
                                enabled = false,
                                onClick = {},
                                backgroundColor = MixinAppTheme.colors.backgroundWindow,
                                contentColor = MixinAppTheme.colors.textAssist,
                                shape = RoundedCornerShape(32.dp),
                            ) {
                                Text(
                                    fontSize = 16.sp,
                                    text = stringResource(if (isAdding) R.string.adding_position else R.string.Pending),
                                )
                            }
                        } else if (isOpen) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                MixinButton(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    enabled = !isAddingProcessing,
                                    onClick = {
                                        if (isAddingProcessing) return@MixinButton
                                        isAddingProcessing = true
                                        val activity = context as? FragmentActivity ?: run { isAddingProcessing = false; return@MixinButton }
                                        val positionForAdd = currentPosition
                                        AnalyticsTracker.trackPerpsAddStart(AnalyticsTracker.PerpsAddType.ADD_POSITION)
                                        PerpsAddBottomSheetDialogFragment.newInstance(positionForAdd)
                                            .setOnDestroy {
                                                isAddingProcessing = false
                                            }
                                            .setOnAdd { token, amount, liquidationPrice ->
                                                isAddingProcessing = false
                                                val referencePrice = market?.last
                                                    ?: positionForAdd.markPrice
                                                    ?: positionForAdd.entryPrice
                                                viewModel.increasePerpsPosition(
                                                    positionId = positionForAdd.positionId,
                                                    assetId = token.assetId,
                                                    amount = amount,
                                                    position = positionForAdd,
                                                    price = referencePrice.takeIf { it.isNotBlank() },
                                                    onSuccess = { response ->
                                                        val isLong = positionForAdd.side.equals("long", ignoreCase = true)
                                                        val symbol = positionForAdd.displaySymbol
                                                            ?: market?.displaySymbol
                                                            ?: positionForAdd.tokenSymbol.orEmpty()
                                                        val iconUrl = positionForAdd.iconUrl
                                                            ?: market?.iconUrl.orEmpty()
                                                        val confirmEntryPrice = referencePrice.ifBlank { positionForAdd.entryPrice }
                                                        PerpsConfirmBottomSheetDialogFragment.newInstance(
                                                            marketSymbol = symbol,
                                                            marketIcon = iconUrl,
                                                            isLong = isLong,
                                                            amount = response.payAmount,
                                                            leverage = positionForAdd.leverage,
                                                            entryPrice = confirmEntryPrice,
                                                            marginAssetPrice = token.priceUsd,
                                                            tokenSymbol = token.symbol,
                                                            takeProfitPrice = null,
                                                            stopLossPrice = null,
                                                            liquidationPrice = liquidationPrice,
                                                            priceScale = market?.priceScale ?: positionForAdd.priceScale,
                                                            payUrl = response.paymentUrl,
                                                            isAddPosition = true,
                                                        ).show(activity.supportFragmentManager, PerpsConfirmBottomSheetDialogFragment.TAG)
                                                    },
                                                    onError = { errorCode, errorMessage ->
                                                        isAddingProcessing = false
                                                        val message = if (errorCode > 0) {
                                                            context.getMixinErrorStringByCode(errorCode, errorMessage)
                                                        } else {
                                                            errorMessage
                                                        }
                                                        toast(message)
                                                    },
                                                )
                                            }
                                            .show(activity.supportFragmentManager, PerpsAddBottomSheetDialogFragment.TAG)
                                    },
                                    backgroundColor = MixinAppTheme.colors.walletGreen,
                                    contentColor = Color.White,
                                    shape = RoundedCornerShape(32.dp),
                                ) {
                                    Text(
                                        fontSize = 16.sp,
                                        text = stringResource(R.string.add_position),
                                    )
                                }

                                MixinButton(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    enabled = true,
                                    onClick = {
                                        val activity = context as? FragmentActivity ?: return@MixinButton
                                        val position = currentPosition.toPosition()
                                        AnalyticsTracker.trackPerpsCloseStart(AnalyticsTracker.PerpsCloseType.SINGLE)
                                        PerpsCloseBottomSheetDialogFragment.newInstance(
                                            position = position,
                                        ).show(activity.supportFragmentManager, PerpsCloseBottomSheetDialogFragment.TAG)
                                    },
                                    backgroundColor = MixinAppTheme.colors.accent,
                                    contentColor = Color.White,
                                    shape = RoundedCornerShape(32.dp),
                                ) {
                                    Text(
                                        fontSize = 16.sp,
                                        text = stringResource(R.string.Close_Position),
                                    )
                                }
                            }
                        } else {
                            MixinButton(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .wrapContentWidth()
                                    .height(48.dp),
                                enabled = false,
                                onClick = {},
                                backgroundColor = MixinAppTheme.colors.backgroundWindow,
                                contentColor = MixinAppTheme.colors.textAssist,
                                shape = RoundedCornerShape(32.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    fontSize = 16.sp,
                                    text = stringResource(R.string.Opening),
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MixinButton(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                onClick = {
                                    PerpsActivity.showOpenPosition(
                                        context = context,
                                        marketId = marketId,
                                        marketSymbol = marketSymbol,
                                        marketDisplaySymbol = market?.displaySymbol ?: marketSymbol,
                                        marketTokenSymbol = market?.tokenSymbol ?: "",
                                        isLong = true,
                                        source = AnalyticsTracker.PerpsSource.PERPS_MARKET_DETAIL,
                                        returnToDetail = true,
                                    )
                                },
                                backgroundColor = risingColor,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(32.dp),
                            ) {
                                Text(
                                    fontSize = 16.sp,
                                    text = stringResource(R.string.Long),
                                )
                            }

                            MixinButton(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                onClick = {
                                    PerpsActivity.showOpenPosition(
                                        context = context,
                                        marketId = marketId,
                                        marketSymbol = marketSymbol,
                                        marketDisplaySymbol = market?.displaySymbol ?: marketSymbol,
                                        marketTokenSymbol = market?.tokenSymbol ?: "",
                                        isLong = false,
                                        source = AnalyticsTracker.PerpsSource.PERPS_MARKET_DETAIL,
                                        returnToDetail = true,
                                    )
                                },
                                backgroundColor = fallingColor,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(32.dp),
                            ) {
                                Text(
                                    fontSize = 16.sp,
                                    text = stringResource(R.string.Short),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketAboutSection(description: String) {
    val textAssist = MixinAppTheme.colors.textAssist.toArgb()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.About),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textMinor,
        )
        Spacer(modifier = Modifier.height(10.dp))
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                MarketDescriptionTextView(context).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTextColor(textAssist)
                    breakStrategy = Layout.BREAK_STRATEGY_HIGH_QUALITY
                    hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
                }
            },
            update = { view ->
                view.setTextColor(textAssist)
                view.setMarketDescription(description)
            },
        )
    }
}

@Composable
private fun HowPerpsWorksCard(
    onLearnClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .clickable { onLearnClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painter = painterResource(id = R.drawable.ic_perps_help), contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.how_perps_works),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MixinAppTheme.colors.textPrimary
                )
                Text(
                    text = stringResource(R.string.learn_how_to_trade_perps),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            }
        }
    }
}

@Composable
private fun MarketInfoCard(
    market: PerpsMarket,
    onFundingRateTipClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.Volume_24H).uppercase(),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatVolume(market.volume),
            fontSize = 16.sp,
            color = MixinAppTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.Funding_Rate).uppercase(),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_tip),
                contentDescription = null,
                modifier = Modifier
                    .size(12.dp)
                    .clickable(onClick = onFundingRateTipClick),
                tint = MixinAppTheme.colors.textAssist
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatFundingRate(market.fundingRate),
            fontSize = 16.sp,
            color = MixinAppTheme.colors.textPrimary
        )
    }
}

@Composable
private fun formatVolume(
    volume: String,
): String {
    return try {
        val vol = BigDecimal(volume)
        "$PERPS_USD_SYMBOL${vol.numberFormatCompact()}"
    } catch (e: NumberFormatException) {
        stringResource(R.string.N_A)
    }
}

private fun formatFundingRate(fundingRate: String): String {
    return runCatching {
        BigDecimal(fundingRate).multiply(BigDecimal(100)).stripTrailingZeros().toPlainString() + "%"
    }.getOrElse { fundingRate }
}

@Composable
private fun MarketDetailCard(
    market: PerpsMarket,
    marketId: String,
    displaySymbol: String,
    tokenSymbol: String,
    selectedTimeFrame: Int,
    timeFrameValues: List<String>,
    timeFrameLabels: List<String>,
    onTimeFrameChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed

    val changePercent = market.changePercent()
    val isPositive = changePercent >= BigDecimal.ZERO
    val changeColor = if (isPositive) risingColor else fallingColor
    val changeText = formatPerpsSignedPercent(changePercent)
    val displayTokenSymbol = tokenSymbol
        .takeIf { it.isNotBlank() }
        ?: market.tokenSymbol.takeIf { it.isNotBlank() }
        ?: displaySymbol

    val displayPrice = market.last

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTokenSymbol,
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = "$PERPS_USD_SYMBOL$displayPrice",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.W500,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clipToBounds()
        ) {
            CandleChart(
                marketId = marketId,
                timeFrame = timeFrameValues[selectedTimeFrame],
                marketPrice = market.last
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            timeFrameLabels.forEachIndexed { index, timeFrameLabel ->
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .then(
                            if (selectedTimeFrame == index) {
                                Modifier.background(MixinAppTheme.colors.backgroundWindow)
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onTimeFrameChange(index) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timeFrameLabel,
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

@Composable
private fun OpenPositionCard(
    position: PerpsPositionItem,
    viewModel: PerpetualViewModel,
    onShare: () -> Unit,
) {
    val context = LocalContext.current
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val pnl = position.unrealizedPnl?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val roe = (position.roe?.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(BigDecimal(100))
    val isProfit = pnl >= BigDecimal.ZERO
    val pnlColor = if (isProfit) risingColor else fallingColor

    val isLong = position.side.equals("long", ignoreCase = true)
    val directionColor = if (isLong) risingColor else fallingColor

    val quantity = position.quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val marginAmount = position.margin?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val amountValue = marginAmount
    val currentPrice = position.markPrice.orEmpty()
        .ifBlank { position.entryPrice }
        .toBigDecimalOrNull() ?: BigDecimal.ZERO
    val positionValue = quantity.abs().multiply(currentPrice)
    var tpSlLoadingMode by remember(position.positionId) {
        mutableStateOf<PerpsTpSlBottomSheetDialogFragment.Mode?>(null)
    }

    val entryPrice = position.entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val liquidationPriceText = position.liquidationPrice
        ?.takeIf { it.isNotBlank() }
        ?.let { formatPerpsPrice(it, position.priceScale) }
        ?: "--"
    val compactTextStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))

    fun showTpSlGuide() {
        val activity = context as? FragmentActivity ?: return
        PerpetualGuideBottomSheetDialogFragment.newInstance(
            PerpetualGuideBottomSheetDialogFragment.TAB_TP_SL
        ).show(activity.supportFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
    }

    fun showTpSlBottomSheet(mode: PerpsTpSlBottomSheetDialogFragment.Mode) {
        val activity = context as? FragmentActivity ?: return
        val existingPrice = when (mode) {
            PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT -> position.takeProfitPrice.orEmpty()
            PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS -> position.stopLossPrice.orEmpty()
        }
        val currentPrice = position.markPrice.orEmpty().ifBlank { position.entryPrice }
        val requestMarginAmount = position.margin ?: position.openPayAmount.orEmpty()

        PerpsTpSlBottomSheetDialogFragment.newInstance(
            mode = mode,
            price = existingPrice,
            currentPrice = currentPrice,
            isLong = isLong,
            marketIconUrl = position.iconUrl.orEmpty(),
            marketSymbol = position.tokenSymbol ?: "",
            marginAmount = requestMarginAmount,
            leverage = position.leverage,
            entryPrice = position.entryPrice,
            marketId = position.marketId,
            priceScale = position.priceScale,
            liquidationPrice = position.liquidationPrice,
        ).setOnApply { value ->
            val normalizedValue = value?.trim().orEmpty()
            if (normalizedValue == existingPrice) {
                return@setOnApply
            }
            if (normalizedValue.isEmpty() && existingPrice.isEmpty()) {
                return@setOnApply
            }

            tpSlLoadingMode = mode
            val requestedValue = normalizedValue.ifEmpty { "" }
            // TP/SL update API treats null as "keep existing value" and empty string as "clear this side".
            viewModel.setPositionTpSl(
                positionId = position.positionId,
                takeProfitPrice = when (mode) {
                    PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT -> requestedValue
                    PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS -> null
                },
                stopLossPrice = when (mode) {
                    PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT -> null
                    PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS -> requestedValue
                },
                onSuccess = {
                    tpSlLoadingMode = null
                    toast(context.getString(R.string.Successful))
                },
                onError = { errorCode, errorMessage ->
                    tpSlLoadingMode = null
                    toast(context.getMixinErrorStringByCode(errorCode, errorMessage))
                },
            )
        }.show(activity.supportFragmentManager, PerpsTpSlBottomSheetDialogFragment.TAG)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.perps_position),
                fontSize = 14.sp,
                style = compactTextStyle,
                fontWeight = FontWeight.W400,
                color = MixinAppTheme.colors.textMinor
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.ic_share_arrow),
                contentDescription = null,
                tint = MixinAppTheme.colors.accent,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onShare),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.PnL).uppercase(),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textAssist
                )
                Text(
                    text = stringResource(R.string.Direction).uppercase(),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textAssist
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatPerpsSignedRawUsdDecimal(pnl)} (${formatPerpsSignedPercent(roe, withSign = false)})",
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    style = compactTextStyle,
                    color = pnlColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(directionColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isLong) stringResource(R.string.Long) else stringResource(R.string.Short),
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            style = compactTextStyle,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${position.leverage}x",
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        style = compactTextStyle,
                        color = MixinAppTheme.colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.position_size).uppercase(),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        style = compactTextStyle,
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
                                PerpetualGuideBottomSheetDialogFragment.newInstance(
                                    PerpetualGuideBottomSheetDialogFragment.TAB_POSITION
                                ).show(activity.supportFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                            },
                        tint = MixinAppTheme.colors.textAssist
                    )
                }
                Text(
                    text = stringResource(R.string.Margin).uppercase(),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textAssist
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatPerpsQuantity(quantity.abs())} ${position.tokenSymbol} (${formatPerpsUsdDecimal(positionValue)})",
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textPrimary
                )
                Text(
                    text = formatPerpsUsdDecimal(amountValue),
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.Entry_Price).uppercase(),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textAssist
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.Liquidation_Price).uppercase(),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        style = compactTextStyle,
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
                                PerpetualGuideBottomSheetDialogFragment.newInstance(
                                    PerpetualGuideBottomSheetDialogFragment.TAB_LIQUIDATION
                                ).show(activity.supportFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                            },
                        tint = MixinAppTheme.colors.textAssist
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPerpsPrice(entryPrice, position.priceScale),
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textPrimary
                )
                Text(
                    text = liquidationPriceText,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TpSlLabel(
                    text = stringResource(R.string.Take_Profit).uppercase(),
                    compactTextStyle = compactTextStyle,
                    onTipClick = ::showTpSlGuide,
                )
                TpSlLabel(
                    text = stringResource(R.string.Stop_Loss).uppercase(),
                    compactTextStyle = compactTextStyle,
                    onTipClick = ::showTpSlGuide,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TpSlActionCell(
                    modifier = Modifier.weight(1f),
                    value = formatMarketTpSlDisplayValue(position.takeProfitPrice, position.priceScale),
                    loading = tpSlLoadingMode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT,
                    compactTextStyle = compactTextStyle,
                    onClick = {
                        showTpSlBottomSheet(PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT)
                    },
                    onDelete = {
                        tpSlLoadingMode = PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT
                        viewModel.setPositionTpSl(
                            positionId = position.positionId,
                            takeProfitPrice = "",
                            stopLossPrice = null,
                            onSuccess = {
                                tpSlLoadingMode = null
                                toast(context.getString(R.string.Successful))
                            },
                            onError = { errorCode, errorMessage ->
                                tpSlLoadingMode = null
                                toast(context.getMixinErrorStringByCode(errorCode, errorMessage))
                            },
                        )
                    },
                )
                Spacer(modifier = Modifier.width(12.dp))
                TpSlActionCell(
                    modifier = Modifier.weight(1f),
                    value = formatMarketTpSlDisplayValue(position.stopLossPrice, position.priceScale),
                    loading = tpSlLoadingMode == PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS,
                    compactTextStyle = compactTextStyle,
                    alignment = Alignment.End,
                    onClick = {
                        showTpSlBottomSheet(PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS)
                    },
                    onDelete = {
                        tpSlLoadingMode = PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS
                        viewModel.setPositionTpSl(
                            positionId = position.positionId,
                            takeProfitPrice = null,
                            stopLossPrice = "",
                            onSuccess = {
                                tpSlLoadingMode = null
                                toast(context.getString(R.string.Successful))
                            },
                            onError = { errorCode, errorMessage ->
                                tpSlLoadingMode = null
                                toast(context.getMixinErrorStringByCode(errorCode, errorMessage))
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TpSlLabel(
    text: String,
    compactTextStyle: TextStyle,
    onTipClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            style = compactTextStyle,
            color = MixinAppTheme.colors.textAssist
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_tip),
            contentDescription = null,
            modifier = Modifier
                .size(12.dp)
                .clickable(onClick = onTipClick),
            tint = MixinAppTheme.colors.textAssist
        )
    }
}

@Composable
private fun TpSlActionCell(
    modifier: Modifier = Modifier,
    value: String?,
    loading: Boolean,
    compactTextStyle: TextStyle,
    alignment: Alignment.Horizontal = Alignment.Start,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val hasValue = value != null
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (alignment == Alignment.End) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hasValue && loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MixinAppTheme.colors.accent
                )
            } else {
                Text(
                    text = value ?: stringResource(R.string.Add),
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    style = compactTextStyle,
                    color = if (hasValue) MixinAppTheme.colors.textPrimary else MixinAppTheme.colors.accent,
                    modifier = if (!hasValue) Modifier.clickable(enabled = !loading, onClick = onClick) else Modifier,
                )
            }
            if (hasValue && loading) {
                Spacer(modifier = Modifier.width(4.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MixinAppTheme.colors.accent
                )
            } else if (hasValue) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_action_delete),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textBlue,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(enabled = !loading && onDelete != null) { onDelete?.invoke() }
                )
            } else if (!loading) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_gray_right),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp).offset(x = 4.dp)
                )
            }
        }
    }
}

private fun formatMarketTpSlDisplayValue(rawValue: String?, priceScale: Int): String? {
    val normalized = rawValue?.trim().orEmpty()
    if (normalized.isEmpty()) {
        return null
    }
    return normalized.toBigDecimalOrNull()?.let { formatPerpsPrice(it, priceScale) } ?: normalized
}

@Composable
private fun ClosedPositionsSection(
    positions: List<PerpsOrderItem>,
    onViewAll: () -> Unit,
    onPositionClick: (PerpsOrderItem) -> Unit,
) {
    val context = LocalContext.current
    val displayPositions = positions.take(3)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewAll)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.perps_activity),
                fontSize = 14.sp,
                fontWeight = FontWeight.W400,
                color = MixinAppTheme.colors.textMinor
            )

            Icon(
                painter = painterResource(R.drawable.ic_arrow_gray_right),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp).offset(x = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        var previousDate: String? = null
        displayPositions.forEach { order ->
            val date = order.createdAtDateLabel(context)
            if (date != previousDate) {
                PerpsActivityDateHeader(
                    date = date,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (order.orderType == PerpsOrder.TYPE_CLOSE) {
                ClosedActivityItem(
                    order = order,
                    onClick = { onPositionClick(order) },
                )
            } else {
                OpenedOrderItem(
                    order = order,
                    onClick = { onPositionClick(order) },
                )
            }
            if (order != displayPositions.last()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            previousDate = date
        }
    }
}
