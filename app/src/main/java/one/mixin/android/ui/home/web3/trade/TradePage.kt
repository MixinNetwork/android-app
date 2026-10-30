@file:OptIn(FlowPreview::class)

package one.mixin.android.ui.home.web3.trade

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.CreateLimitOrderResponse
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.session.Session
import one.mixin.android.ui.components.TabItem
import one.mixin.android.ui.home.web3.components.OutlinedTab
import one.mixin.android.ui.home.web3.components.PageScaffold
import one.mixin.android.ui.home.web3.trade.perps.PerpetualContent
import one.mixin.android.ui.home.web3.widget.MarketSort
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.WalletCategory
import java.math.BigDecimal

@OptIn(ExperimentalFoundationApi::class)
@FlowPreview
@Composable
fun TradePage(
    walletId: String?,
    swapFrom: SwapToken?,
    swapTo: SwapToken?,
    limitFrom: SwapToken?,
    limitTo: SwapToken?,
    inMixin: Boolean,
    isLimitOrderTabBadgeDismissed: Boolean,
    isPerpetualTabBadgeDismissed: Boolean,
    initialAmount: String?,
    lastOrderTime: Long?,
    reviewing: Boolean,
    autoFocusAmount: Boolean,
    initialTabIndex: Int,
    source: String,
    entrySource: String,
    stockMarkets: List<MarketItem>,
    trendingMarkets: List<MarketItem>,
    topGainerMarkets: List<MarketItem>,
    topLoserMarkets: List<MarketItem>,
    scrollToTopSignal: Long,
    onSelectToken: (Boolean, SelectTokenType, Boolean) -> Unit,
    onReview: (QuoteResult, SwapToken, SwapToken, String) -> Unit,
    onLimitReview: (SwapToken, SwapToken, CreateLimitOrderResponse) -> Unit,
    onDeposit: (SwapToken) -> Unit,
    onOrderList: (String, Boolean, String) -> Unit,
    onDismissLimitOrderTabBadge: () -> Unit,
    onDismissPerpetualTabBadge: () -> Unit,
    onTabChanged: (Int) -> Unit,
    onSwitchToLimitOrder: (String, SwapToken, SwapToken) -> Unit,
    pop: () -> Unit,
    onLimitOrderClick: (String) -> Unit,
    onShowTradingGuideIfNeeded: (Int) -> Unit,
    onShowTradingGuide: (Int) -> Unit,
    onShowMore: (String?) -> Unit,
    onRecommendedMarketClick: (MarketItem) -> Unit,
    onRecommendedMarketViewAllClick: (SwapRecommendedMarketType, Boolean) -> Unit,
    onShowMarketList: (Boolean) -> Unit,
    onShowAllMarkets: (String?, MarketSort?) -> Unit,
    onShowAllOpenPositions: (String) -> Unit,
    onShowAllClosedPositions: (String) -> Unit,
    onOpenPositionClick: (PerpsPositionItem) -> Unit,
    onMarketItemClick: (PerpsMarket) -> Unit,
    onClosedPositionClick: (PerpsOrderItem) -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val viewModel = hiltViewModel<SwapViewModel>()
    var walletDisplayName by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val currentWalletId = walletId ?: Session.getAccountId() ?: ""
    val tradeWallet = if (walletId == null) {
        AnalyticsTracker.TradeWallet.MAIN
    } else {
        AnalyticsTracker.TradeWallet.WEB3
    }
    LaunchedEffect(walletId) {
        if (walletId != null) {
            viewModel.findWeb3WalletById(walletId)?.let {
                if (it.category == WalletCategory.CLASSIC.value ||
                    it.category == WalletCategory.IMPORTED_MNEMONIC.value ||
                    it.category == WalletCategory.IMPORTED_PRIVATE_KEY.value ||
                    it.category == WalletCategory.WATCH_ADDRESS.value) {
                    walletDisplayName = it.name
                }
            }
        }
    }

    // Build tabs dynamically: Perpetual tab should only exist when walletId == null
    val switchToLimitRequested = remember { mutableStateOf(false) }

    val tabs = mutableListOf<TabItem>()

    tabs += TabItem(stringResource(id = R.string.Trade_Simple)) {
        SwapContent(
            from = swapFrom,
            to = swapTo,
            inMixin = inMixin,
            initialAmount = initialAmount,
            lastOrderTime = lastOrderTime,
            reviewing = reviewing,
            autoFocus = autoFocusAmount && initialTabIndex == TradeFragment.TAB_SIMPLE,
            source = source,
            stockMarkets = stockMarkets,
            trendingMarkets = trendingMarkets,
            topGainerMarkets = topGainerMarkets,
            topLoserMarkets = topLoserMarkets,
            scrollToTopSignal = scrollToTopSignal,
            onSelectToken = { isReverse, type -> onSelectToken(isReverse, type, false) },
            onReview = onReview,
            onDeposit = onDeposit,
            onRecommendedMarketClick = onRecommendedMarketClick,
            onRecommendedMarketViewAllClick = onRecommendedMarketViewAllClick,
            onSwitchToLimitOrder = { inputText, fromToken, toToken ->
                // Notify parent and request navigation to Limit tab locally
                onSwitchToLimitOrder(inputText, fromToken, toToken)
                switchToLimitRequested.value = true
                onDismissLimitOrderTabBadge()
                onTabChanged(1)
            },
        )
    }

    tabs += TabItem(stringResource(id = R.string.Trade_Advanced)) {
        LimitOrderContent(
            limitFrom,
            limitTo,
            inMixin,
            initialAmount,
            lastOrderTime,
            reviewing,
            autoFocusAmount && initialTabIndex == TradeFragment.TAB_ADVANCED,
            { isReverse, type -> onSelectToken(isReverse, type, true) },
            onLimitReview,
            onDeposit,
            onLimitOrderClick,
            onOrderList,
        )
    }

    var perpetualTabIndex: Int? = null
    if (walletId == null) {
        perpetualTabIndex = tabs.size
        tabs += TabItem(title = stringResource(R.string.Perpetual)) {
            PerpetualContent(
                onShowTradingGuide = { onShowTradingGuide(perpetualTabIndex) },
                onShowMarketList = onShowMarketList,
                onShowAllMarkets = onShowAllMarkets,
                onShowAllOpenPositions = onShowAllOpenPositions,
                onShowAllClosedPositions = onShowAllClosedPositions,
                onOpenPositionClick = onOpenPositionClick,
                onMarketItemClick = onMarketItemClick,
                onClosedPositionClick = onClosedPositionClick,
            )
        }
    }

    val tabCount = tabs.size

    val pagerState = rememberPagerState(
        initialPage = initialTabIndex.coerceIn(0, (tabCount - 1).coerceAtLeast(0)),
        pageCount = { tabCount },
    )

    fun spotTypeForPage(page: Int): String {
        return if (page == 1) AnalyticsTracker.SpotTradeType.ADVANCED else AnalyticsTracker.SpotTradeType.SIMPLE
    }

    fun tradeTypeForPage(page: Int): String {
        return if (perpetualTabIndex != null && page == perpetualTabIndex) {
            AnalyticsTracker.SpotTradeType.PERPETUAL
        } else {
            spotTypeForPage(page)
        }
    }

    TrackSpotStartEvent(
        initialTabIndex = initialTabIndex,
        perpetualTabIndex = perpetualTabIndex,
        tradeWallet = tradeWallet,
        spotType = spotTypeForPage(initialTabIndex),
        entrySource = entrySource,
    )

    LaunchedEffect(Unit) {
        onShowTradingGuideIfNeeded(pagerState.currentPage)
    }

    // When SwapContent requests switching to Limit tab, animate to it
    LaunchedEffect(switchToLimitRequested.value) {
        if (switchToLimitRequested.value) {
            coroutineScope.launch {
                val target = (1).coerceAtMost((tabCount - 1).coerceAtLeast(0))
                pagerState.animateScrollToPage(target)
            }
            switchToLimitRequested.value = false
        }
    }

    PageScaffold(
        title = stringResource(id = R.string.Trade),
        titleContent = { TradeTitle() },
        subtitle = { TradeWalletSubtitle(walletId, walletDisplayName) },
        verticalScrollable = false,
        pop = pop,
        actions = {
            val proBadgeKey = "${Constants.Account.PREF_TRADE_PRO_BADGE_DISMISSED}_$currentWalletId"
            var isProBadgeDismissed by remember(currentWalletId) {
                mutableStateOf(context.defaultSharedPreferences.getBoolean(proBadgeKey, false))
            }
            Box {
                IconButton(onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (!isProBadgeDismissed) {
                        isProBadgeDismissed = true
                        context.defaultSharedPreferences.putBoolean(proBadgeKey, true)
                    }
                    AnalyticsTracker.trackTradeMoreMenu(
                        when (pagerState.currentPage) {
                            perpetualTabIndex -> "perps"
                            1 -> "limit"
                            else -> "swap"
                        }
                    )
                    onShowMore(walletDisplayName)
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_horiz_black_24dp),
                        contentDescription = stringResource(R.string.More),
                        tint = MixinAppTheme.colors.icon,
                    )
                }
                if (!isProBadgeDismissed) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-12).dp, y = 12.dp)
                            .size(8.dp)
                            .background(MixinAppTheme.colors.badgeRed, CircleShape),
                    )
                }
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.Start,
        ) {
            tabs.forEachIndexed { index, tab ->
                val isAdvancedTab: Boolean = index == 1
                val showAdvancedBadge: Boolean = isAdvancedTab && !isLimitOrderTabBadgeDismissed
                val isPerpetualTab: Boolean = perpetualTabIndex != null && index == perpetualTabIndex
                val showPerpetualBadge: Boolean = isPerpetualTab && !isPerpetualTabBadgeDismissed
                OutlinedTab(
                    text = tab.title,
                    selected = pagerState.currentPage == index,
                    showBadge = showAdvancedBadge || showPerpetualBadge,
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                        if (isAdvancedTab) {
                            onDismissLimitOrderTabBadge()
                        }
                        if (isPerpetualTab && !isPerpetualTabBadgeDismissed) {
                            onDismissPerpetualTabBadge()
                        }
                        AnalyticsTracker.trackTradeTypeSelect(tradeTypeForPage(index))
                        onShowTradingGuideIfNeeded(index)
                        onTabChanged(index)
                    },
                )
                if (index < tabs.size - 1) {
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
        ) { page ->
            tabs[page].screen()
        }
    }
}

@Composable
private fun TrackSpotStartEvent(
    initialTabIndex: Int,
    perpetualTabIndex: Int?,
    tradeWallet: String,
    spotType: String,
    entrySource: String,
) {
    SideEffect(
        key1 = initialTabIndex,
        key2 = tradeWallet,
        key3 = entrySource,
    ) {
        if (perpetualTabIndex == null || initialTabIndex != perpetualTabIndex) {
            AnalyticsTracker.trackSpotStart(
                wallet = tradeWallet,
                type = spotType,
                source = entrySource,
            )
        }
    }
}

/**
 * @return True if the input was successful, false if the balance is insufficient, or null if the input is invalid.
 */
fun checkBalance(
    inputText: String,
    balance: String?,
): Boolean? {
    if (balance.isNullOrEmpty()) return false
    val inputValue =
        try {
            BigDecimal(inputText)
        } catch (_: Exception) {
            null
        } ?: return null
    if (inputValue <= BigDecimal.ZERO) return null
    val balanceValue =
        try {
            BigDecimal(balance)
        } catch (_: Exception) {
            null
        } ?: return null
    return inputValue <= balanceValue
}
