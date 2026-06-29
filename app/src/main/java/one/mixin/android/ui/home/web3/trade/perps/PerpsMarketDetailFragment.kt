package one.mixin.android.ui.home.web3.trade.perps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.isNightMode
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.analytics.AnalyticsTracker

@AndroidEntryPoint
class PerpsMarketDetailFragment : BaseFragment() {
    companion object {
        const val TAG = "PerpsMarketDetailFragment"
        private const val ARGS_MARKET_ID = "args_market_id"
        private const val ARGS_MARKET_SYMBOL = "args_market_symbol"
        private const val ARGS_DISPLAY_SYMBOL = "args_display_symbol"
        private const val ARGS_TOKEN_SYMBOL = "args_token_symbol"
        private const val ARGS_SOURCE = "args_source"

        fun newInstance(
            marketId: String,
            marketSymbol: String,
            displaySymbol: String,
            tokenSymbol: String,
            source: String = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
        ): PerpsMarketDetailFragment {
            return PerpsMarketDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARGS_MARKET_ID, marketId)
                    putString(ARGS_MARKET_SYMBOL, marketSymbol)
                    putString(ARGS_DISPLAY_SYMBOL, displaySymbol)
                    putString(ARGS_TOKEN_SYMBOL, tokenSymbol)
                    putString(ARGS_SOURCE, source)
                }
            }
        }
    }

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
        val source = requireArguments().getString(ARGS_SOURCE) ?: AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL

        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(
                    darkTheme = context.isNightMode(),
                ) {
                    PerpsMarketDetailPage(
                        marketId = marketId,
                        marketSymbol = marketSymbol,
                        displaySymbol = displaySymbol,
                        tokenSymbol = tokenSymbol,
                        onBack = { close() },
                        onSharePosition = ::sharePosition,
                        onViewAllClosedPositions = {
                            PerpsRouteNavigator.showPositionList(
                                fragmentManager = parentFragmentManager,
                                showOpenPositions = false,
                                source = AnalyticsTracker.PerpsSource.PERPS_MARKET_DETAIL,
                            )
                        },
                        onPositionClick = { position ->
                            PerpsRouteNavigator.showPositionDetail(
                                fragmentManager = parentFragmentManager,
                                order = position,
                                source = AnalyticsTracker.PerpsSource.PERPS_MARKET_DETAIL,
                            )
                        },
                        onOpenPosition = { market, isLong ->
                            PerpsRouteNavigator.showOpenPosition(
                                fragmentManager = parentFragmentManager,
                                marketId = market.marketId,
                                marketSymbol = market.displaySymbol,
                                displaySymbol = market.displaySymbol,
                                tokenSymbol = market.tokenSymbol,
                                isLong = isLong,
                                source = AnalyticsTracker.PerpsSource.PERPS_MARKET_DETAIL,
                            )
                        },
                        source = source,
                    )
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

    private fun sharePosition(position: PerpsPositionItem) {
        PerpsPositionShareBottomFragment.newInstance(position)
            .show(parentFragmentManager, PerpsPositionShareBottomFragment.TAG)
    }
}
