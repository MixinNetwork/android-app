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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.CreateLimitOrderResponse
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.components.TabItem
import one.mixin.android.ui.home.web3.components.OutlinedTab
import one.mixin.android.vo.WalletCategory
import java.math.BigDecimal

@OptIn(ExperimentalFoundationApi::class)
@FlowPreview
@Composable
fun TradePage(
    walletId: String?,
    from: SwapToken?,
    to: SwapToken?,
    inMixin: Boolean,
    orderBadge: Boolean,
    initialAmount: String?,
    lastOrderTime: Long?,
    reviewing: Boolean,
    source: String,
    onSelectToken: (Boolean, SelectTokenType) -> Unit,
    onReview: (QuoteResult, SwapToken, SwapToken, String) -> Unit,
    onLimitReview: (CreateLimitOrderResponse) -> Unit,
    onDeposit: (SwapToken) -> Unit,
    onOrderList: () -> Unit,
    pop: () -> Unit,
    onLimitOrderClick: (String) -> Unit,
) {
    val context = LocalContext.current

    val viewModel = hiltViewModel<SwapViewModel>()
    var walletDisplayName by remember { mutableStateOf<String?>(null) }

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

    val tabs = listOf(
        TabItem(stringResource(id = R.string.Trade_Simple)) {
            SwapContent(
                from = from,
                to = to,
                inMixin = inMixin,
                initialAmount = initialAmount,
                lastOrderTime = lastOrderTime,
                reviewing = reviewing,
                source = source,
                onSelectToken = onSelectToken,
                onReview = onReview,
                onDeposit = onDeposit,
            )
        },
        TabItem(stringResource(id = R.string.Trade_Advanced)) {
            LimitOrderContent(
                from = from,
                to = to,
                inMixin = inMixin,
                onSelectToken = onSelectToken,
                onLimitReview = onLimitReview,
                onDeposit = onDeposit,
                onLimitOrderClick = onLimitOrderClick,
            )
        }
    )
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    PageScaffold(
        title = stringResource(id = R.string.Swap),
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
            if (source != "web3") {
                Box {
                    IconButton(onClick = {
                        onOrderList()
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_order),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.icon,
                        )
                    }
                    if (orderBadge) {
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
            }
            IconButton(onClick = {
                context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_support),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
            }
        },
    ) {
        if (walletId.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                tabs.forEachIndexed { index, tab ->
                    OutlinedTab(
                        text = tab.title,
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
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
            ) { page ->
                tabs[page].screen()
            }
        } else {
            SwapContent(
                from = from,
                to = to,
                inMixin = inMixin,
                initialAmount = initialAmount,
                lastOrderTime = lastOrderTime,
                reviewing = reviewing,
                source = source,
                onSelectToken = onSelectToken,
                onReview = onReview,
                onDeposit = onDeposit,
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
