package one.mixin.android.ui.home.web3.trade.perps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.analytics.AnalyticsTracker
import javax.inject.Inject

@AndroidEntryPoint
class AllPositionsFragment : BaseFragment() {

    companion object {
        const val TAG = "AllPositionsFragment"
        private const val ARGS_POSITION_TYPE = "args_position_type"
        private const val ARGS_SOURCE = "args_source"
        private const val ARGS_USE_TRADE_FLOW_NAVIGATOR = "args_use_trade_flow_navigator"
        private const val TYPE_OPEN = "type_open"
        private const val TYPE_CLOSED = "type_closed"

        fun newInstance(
            showOpenPositions: Boolean = false,
            source: String = AnalyticsTracker.PerpsSource.PERPS_ALL_POSITIONS,
            useTradeFlowNavigator: Boolean = false,
        ) = AllPositionsFragment().apply {
            arguments = Bundle().apply {
                putString(ARGS_POSITION_TYPE, if (showOpenPositions) TYPE_OPEN else TYPE_CLOSED)
                putString(ARGS_SOURCE, source)
                putBoolean(ARGS_USE_TRADE_FLOW_NAVIGATOR, useTradeFlowNavigator)
            }
        }

        fun newOpenInstance(
            source: String = AnalyticsTracker.PerpsSource.PERPS_ALL_POSITIONS,
            useTradeFlowNavigator: Boolean = false,
        ) = newInstance(
            showOpenPositions = true,
            source = source,
            useTradeFlowNavigator = useTradeFlowNavigator,
        )

        fun newClosedInstance(
            source: String = AnalyticsTracker.PerpsSource.PERPS_ALL_POSITIONS,
            useTradeFlowNavigator: Boolean = false,
        ) = newInstance(
            showOpenPositions = false,
            source = source,
            useTradeFlowNavigator = useTradeFlowNavigator,
        )
    }

    @Inject
    lateinit var perpsMarketDao: PerpsMarketDao

    private val viewModel by viewModels<PerpetualViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments?.getBoolean(ARGS_USE_TRADE_FLOW_NAVIGATOR, false) != true) return
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
        val positionType = when (arguments?.getString(ARGS_POSITION_TYPE, TYPE_CLOSED)) {
            TYPE_OPEN -> AllPositionsType.OPEN
            else -> AllPositionsType.CLOSED
        }
        val source = arguments?.getString(ARGS_SOURCE) ?: AnalyticsTracker.PerpsSource.PERPS_ALL_POSITIONS
        val useTradeFlowNavigator = arguments?.getBoolean(ARGS_USE_TRADE_FLOW_NAVIGATOR, false) == true
        if (positionType == AllPositionsType.OPEN) {
            AnalyticsTracker.trackPerpsAllPositions(source)
        }
        if (positionType == AllPositionsType.CLOSED) {
            AnalyticsTracker.trackPerpsActivity(source)
        }

        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(
                    darkTheme = context.isNightMode(),
                ) {
                    AllPositionsPage(
                        positionType = positionType,
                        viewModel = viewModel,
                        onBack = {
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        },
                        onSupport = {
                            context.openUrl(
                                Constants.HelpLink.CUSTOMER_SERVICE,
                                source = AnalyticsTracker.CustomerServiceSource.PERPS_ALL_POSITIONS,
                                wallet = AnalyticsTracker.TradeWallet.WEB3,
                            )
                        },
                        onShowTradingGuide = {
                            AnalyticsTracker.trackPerpsGuide(AnalyticsTracker.PerpsSource.PERPS_ALL_POSITIONS)
                            PerpetualGuideBottomSheetDialogFragment.newInstance(
                                initialTab = PerpetualGuideBottomSheetDialogFragment.TAB_OVERVIEW
                            ).show(parentFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                        },
                        onOpenPositionClick = { position ->
                            lifecycleScope.launch {
                                val market = withContext(Dispatchers.IO) {
                                    perpsMarketDao.getMarket(position.marketId)
                                }
                                if (useTradeFlowNavigator) {
                                    PerpsRouteNavigator.showMarketDetail(
                                        fragmentManager = parentFragmentManager,
                                        marketId = position.marketId,
                                        marketSymbol = market?.displaySymbol ?: position.displaySymbol.orEmpty(),
                                        displaySymbol = market?.displaySymbol ?: position.displaySymbol.orEmpty(),
                                        tokenSymbol = market?.tokenSymbol ?: position.tokenSymbol.orEmpty(),
                                        source = AnalyticsTracker.PerpsSource.PERPS_ALL_POSITIONS,
                                    )
                                } else {
                                    activity?.let { ctx ->
                                        PerpsActivity.showDetail(
                                            context = ctx,
                                            marketId = position.marketId,
                                            marketSymbol = market?.displaySymbol ?: position.displaySymbol.orEmpty(),
                                            marketDisplaySymbol = market?.displaySymbol ?: position.displaySymbol.orEmpty(),
                                            marketTokenSymbol = market?.tokenSymbol ?: position.tokenSymbol.orEmpty(),
                                            source = AnalyticsTracker.PerpsSource.PERPS_ALL_POSITIONS,
                                        )
                                    }
                                }
                            }
                        },
                        onClosedPositionClick = { position ->
                            if (useTradeFlowNavigator) {
                                PerpsRouteNavigator.showPositionDetail(
                                    fragmentManager = parentFragmentManager,
                                    order = position,
                                    source = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_LIST,
                                )
                            } else {
                                showPositionDetail(parentFragmentManager, position)
                            }
                        },
                    )
                }
            }
        }
    }

    private fun showPositionDetail(fragmentManager: FragmentManager, position: PerpsOrderItem) {
        fragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right)
            .add(
                android.R.id.content,
                PositionDetailFragment.newInstance(position, AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_LIST),
                PositionDetailFragment.TAG,
            )
            .addToBackStack(null)
            .commit()
    }

    private fun close() {
        PerpsRouteNavigator.closeTopRoute(parentFragmentManager, this)
    }

    private fun isTopFragment(): Boolean {
        return parentFragmentManager.fragments.lastOrNull { it.isVisible } == this
    }
}
