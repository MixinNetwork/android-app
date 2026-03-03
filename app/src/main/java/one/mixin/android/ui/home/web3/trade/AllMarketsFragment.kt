package one.mixin.android.ui.home.web3.trade

import PageScaffold
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.vo.market.MarketItem

@AndroidEntryPoint
class AllMarketsFragment : BaseFragment() {

    companion object {
        const val TAG = "AllMarketsFragment"

        fun newInstance() = AllMarketsFragment()
    }

    private val swapViewModel by viewModels<SwapViewModel>()

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?,
    ): android.view.View {
        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(darkTheme = context.isNightMode()) {
                    AllMarketsPage(
                        pop = { activity?.onBackPressedDispatcher?.onBackPressed() },
                        onMarketClick = { market ->
                            lifecycleScope.launch {
                                showMarketDetails(market)
                            }
                        }
                    )
                }
            }
        }
    }

    private suspend fun showMarketDetails(market: PerpsMarket) {
        val marketItem = findMarketItemByPerpsMarket(market)
        if (marketItem != null && activity != null) {
            WalletActivity.showWithMarket(requireActivity(), marketItem, WalletActivity.Destination.Market)
        } else {
            toast(R.string.Alert_Not_Support)
        }
    }

    private suspend fun findMarketItemByPerpsMarket(market: PerpsMarket): MarketItem? {
        val symbols = linkedSetOf(
            market.tokenSymbol,
            market.displaySymbol.substringBefore("/"),
            market.displaySymbol.substringBefore("-"),
            market.symbol.substringBefore("-"),
            market.symbol.substringBefore("_"),
        ).map { it.trim().uppercase() }.filter { it.isNotEmpty() }

        for (symbol in symbols) {
            val tokens = runCatching { swapViewModel.searchTokens(symbol, true) }
                .getOrNull()
                ?.data
                .orEmpty()
            val token = tokens.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) } ?: tokens.firstOrNull()
            val assetId = token?.assetId ?: continue
            val marketItem = runCatching { swapViewModel.checkMarketById(assetId, false) }.getOrNull()
            if (marketItem != null) {
                return marketItem
            }
        }
        return null
    }
}

@Composable
private fun AllMarketsPage(
    pop: () -> Unit,
    onMarketClick: (PerpsMarket) -> Unit,
) {
    val viewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel<PerpetualViewModel>()
    var markets by remember { mutableStateOf<List<PerpsMarket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
    }

    PageScaffold(
        title = androidx.compose.ui.res.stringResource(R.string.Markets),
        verticalScrollable = false,
        pop = pop
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MixinAppTheme.colors.accent)
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.red,
                    )
                }
            }

            markets.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.No_Markets),
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(markets, key = { it.marketId }) { market ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MarketItem(
                                market = market,
                                onClick = { onMarketClick(market) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}
