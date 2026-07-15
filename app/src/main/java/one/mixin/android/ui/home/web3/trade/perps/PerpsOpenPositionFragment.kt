package one.mixin.android.ui.home.web3.trade.perps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.TokenListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.safe.TokenItem
import javax.inject.Inject

@AndroidEntryPoint
class PerpsOpenPositionFragment : BaseFragment() {
    companion object {
        const val TAG = "PerpsOpenPositionFragment"
        private const val ARGS_MARKET_ID = "args_market_id"
        private const val ARGS_MARKET_SYMBOL = "args_market_symbol"
        private const val ARGS_DISPLAY_SYMBOL = "args_display_symbol"
        private const val ARGS_TOKEN_SYMBOL = "args_token_symbol"
        private const val ARGS_IS_LONG = "args_is_long"
        private const val ARGS_SOURCE = "args_source"

        fun newInstance(
            marketId: String,
            marketSymbol: String,
            displaySymbol: String,
            tokenSymbol: String,
            isLong: Boolean,
            source: String,
        ): PerpsOpenPositionFragment {
            return PerpsOpenPositionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARGS_MARKET_ID, marketId)
                    putString(ARGS_MARKET_SYMBOL, marketSymbol)
                    putString(ARGS_DISPLAY_SYMBOL, displaySymbol)
                    putString(ARGS_TOKEN_SYMBOL, tokenSymbol)
                    putBoolean(ARGS_IS_LONG, isLong)
                    putString(ARGS_SOURCE, source)
                }
            }
        }
    }

    @Inject
    lateinit var perpsMarketDao: PerpsMarketDao

    private var selectedToken by mutableStateOf<TokenItem?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isTopFragment()) {
                        close()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val marketId = requireArguments().getString(ARGS_MARKET_ID).orEmpty()
        val marketSymbol = requireArguments().getString(ARGS_MARKET_SYMBOL).orEmpty()
        val displaySymbol = requireArguments().getString(ARGS_DISPLAY_SYMBOL).orEmpty()
        val tokenSymbol = requireArguments().getString(ARGS_TOKEN_SYMBOL).orEmpty()
        val isLong = requireArguments().getBoolean(ARGS_IS_LONG, true)
        val source = requireArguments().getString(ARGS_SOURCE) ?: AnalyticsTracker.PerpsSource.MORE_EXPLORE

        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(
                    darkTheme = context.isNightMode(),
                ) {
                    var market by remember(marketId) { mutableStateOf<PerpsMarket?>(null) }
                    LaunchedEffect(marketId) {
                        market = withContext(Dispatchers.IO) {
                            perpsMarketDao.getMarket(marketId)
                        }
                        if (market == null) {
                            toast(R.string.Alert_Not_Support)
                            close()
                        } else {
                            AnalyticsTracker.trackPerpsOpenPositionStart(
                                direction = if (isLong) AnalyticsTracker.PerpsDirection.LONG else AnalyticsTracker.PerpsDirection.SHORT,
                                source = source,
                            )
                        }
                    }
                    market?.let { currentMarket ->
                        OpenPositionPage(
                            market = currentMarket,
                            isLong = isLong,
                            source = source,
                            onBack = { close() },
                            onOpenSuccess = { openedMarketId ->
                                PerpsRouteNavigator.showMarketDetail(
                                    fragmentManager = parentFragmentManager,
                                    marketId = openedMarketId,
                                    marketSymbol = currentMarket.displaySymbol.ifBlank { marketSymbol },
                                    displaySymbol = currentMarket.displaySymbol.ifBlank { displaySymbol },
                                    tokenSymbol = currentMarket.tokenSymbol.ifBlank { tokenSymbol },
                                    source = AnalyticsTracker.PerpsSource.PERPS_MARKET_DETAIL,
                                )
                            },
                            selectedToken = selectedToken,
                            onTokenSelect = { showTokenSelection() },
                            onCurrentTokenChange = { token ->
                                selectedToken = token
                            },
                        )
                    }
                }
            }
        }
    }

    private fun close() {
        PerpsRouteNavigator.closeTopRoute(parentFragmentManager, this)
    }

    private fun isTopFragment(): Boolean {
        return parentFragmentManager.fragments.lastOrNull { it.isVisible } == this
    }

    private fun showTokenSelection() {
        TokenListBottomSheetDialogFragment.newInstance(
            fromType = TokenListBottomSheetDialogFragment.TYPE_FROM_PERP,
            currentAssetId = selectedToken?.assetId,
        ).setOnAssetClick { token ->
            selectedToken = token
        }.setOnDepositClick {
            showDepositAssetSelection()
        }.show(parentFragmentManager, TokenListBottomSheetDialogFragment.TAG)
    }

    private fun showDepositAssetSelection() {
        val token = selectedToken
        if (token == null) {
            toast(R.string.Not_found)
            return
        }
        activity?.let {
            WalletActivity.showDeposit(it, token)
        }
    }
}
