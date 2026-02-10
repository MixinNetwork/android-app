@file:OptIn(FlowPreview::class)

package one.mixin.android.ui.home.web3.trade

import PageScaffold
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.CreateLimitOrderResponse
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.session.Session
import one.mixin.android.ui.components.TabItem
import one.mixin.android.ui.home.web3.components.OutlinedTab
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
    orderBadge: Boolean,
    isLimitOrderTabBadgeDismissed: Boolean,
    initialAmount: String?,
    lastOrderTime: Long?,
    reviewing: Boolean,
    initialTabIndex: Int,
    source: String,
    onSelectToken: (Boolean, SelectTokenType, Boolean) -> Unit,
    onReview: (QuoteResult, SwapToken, SwapToken, String) -> Unit,
    onLimitReview: (SwapToken, SwapToken, CreateLimitOrderResponse) -> Unit,
    onDeposit: (SwapToken) -> Unit,
    onOrderList: (String, Boolean) -> Unit,
    onDismissLimitOrderTabBadge: () -> Unit,
    onTabChanged: (Int) -> Unit,
    onSwitchToLimitOrder: (String, SwapToken, SwapToken) -> Unit,
    pop: () -> Unit,
    onLimitOrderClick: (String) -> Unit,
    onShowTradingGuide: () -> Unit,
) {
    val context = LocalContext.current

    val viewModel = hiltViewModel<SwapViewModel>()
    var walletDisplayName by remember { mutableStateOf<String?>(null) }
    var pendingOrderCount by remember { mutableIntStateOf(0) }
    
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val coroutineScope = rememberCoroutineScope()

    val currentWalletId = walletId ?: Session.getAccountId() ?: ""
    val pendingCount by viewModel.getPendingOrderCountByWallet(currentWalletId).collectAsStateWithLifecycle(initialValue = 0)

    LaunchedEffect(pendingCount) {
        pendingOrderCount = pendingCount
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
            source = source,
            onSelectToken = { isReverse, type -> onSelectToken(isReverse, type, false) },
            onReview = onReview,
            onDeposit = onDeposit,
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
            { isReverse, type -> onSelectToken(isReverse, type, true) },
            onLimitReview,
            onDeposit,
            onLimitOrderClick,
            onOrderList,
        )
    }

    if (walletId == null) {
        tabs += TabItem(title = stringResource(R.string.Perpetual)) {
            PerpetualContent(
                onLongClick = { _ ->
                    // TODO: Handle long position
                },
                onShortClick = { _ ->
                    // TODO: Handle short position
                },
                onShowTradingGuide = onShowTradingGuide
            )
        }
    }

    val tabCount = tabs.size

    val pagerState = rememberPagerState(
        initialPage = initialTabIndex.coerceIn(0, (tabCount - 1).coerceAtLeast(0)),
        pageCount = { tabCount },
    )

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

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MixinAppTheme.colors.background,
        sheetContent = {
            HelpBottomSheetContent(
                onContactSupport = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                    }
                },
                onTradingGuide = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        onShowTradingGuide()
                    }
                },
                onDismiss = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                    }
                }
            )
        }
    ) {
    PageScaffold(
        title = stringResource(id = R.string.Trade),
        subtitle = {
            val text = if (walletId == null) {
                stringResource(id = R.string.Privacy_Wallet)
            } else {
                walletDisplayName ?: stringResource(id = R.string.Common_Wallet)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MixinAppTheme.colors.textAssist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (walletId == null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_wallet_privacy),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        },
        verticalScrollable = true,
        pop = pop,
        actions = {
            Box {
                IconButton(onClick = {
                    onOrderList(currentWalletId, false)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_order),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.icon,
                    )
                }
                if (pendingOrderCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-8).dp, y = (8).dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(color = Color(0xFF3D75E3))
                            .padding(vertical = 2.dp, horizontal = 6.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "${if (pendingOrderCount > 99) "99+" else pendingOrderCount}",
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            color = Color.White,
                        )
                    }
                } else if (orderBadge) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(x = (-12).dp, y = (12).dp)
                            .background(
                                color = MixinAppTheme.colors.badgeRed,
                                shape = CircleShape
                            )
                            .align(Alignment.TopEnd)
                    )
                }
            }
            IconButton(onClick = {
                coroutineScope.launch {
                    bottomSheetState.show()
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_support),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
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
                OutlinedTab(
                    text = tab.title,
                    selected = pagerState.currentPage == index,
                    showBadge = showAdvancedBadge,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                        if (isAdvancedTab) {
                            onDismissLimitOrderTabBadge()
                        }
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
