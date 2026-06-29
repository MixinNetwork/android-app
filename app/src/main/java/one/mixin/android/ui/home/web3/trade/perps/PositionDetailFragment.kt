package one.mixin.android.ui.home.web3.trade.perps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.api.response.perps.PerpsOrder
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.toPosition
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.analytics.AnalyticsTracker

@AndroidEntryPoint
class PositionDetailFragment : BaseFragment() {
    companion object {
        const val TAG = "PositionDetailFragment"
        private const val ARGS_POSITION = "args_position"
        private const val ARGS_CLOSE_ORDER = "args_close_order"
        private const val ARGS_SOURCE = "args_source"
        private const val ARGS_USE_TRADE_FLOW_NAVIGATOR = "args_use_trade_flow_navigator"
        private const val POSITION_REFRESH_INTERVAL_MS = 10_000L

        fun newInstance(
            position: PerpsPositionItem,
            source: String = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
            useTradeFlowNavigator: Boolean = false,
        ): PositionDetailFragment {
            return PositionDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_POSITION, position)
                    putString(ARGS_SOURCE, source)
                    putBoolean(ARGS_USE_TRADE_FLOW_NAVIGATOR, useTradeFlowNavigator)
                }
            }
        }

        fun newInstance(
            order: PerpsOrderItem,
            source: String = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
            useTradeFlowNavigator: Boolean = false,
        ): PositionDetailFragment {
            return PositionDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_CLOSE_ORDER, order)
                    putString(ARGS_SOURCE, source)
                    putBoolean(ARGS_USE_TRADE_FLOW_NAVIGATOR, useTradeFlowNavigator)
                }
            }
        }
    }

    private val viewModel by viewModels<PerpetualViewModel>()

    private val quoteColorReversed: Boolean by lazy {
        requireContext().defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    }

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
        val position = arguments?.getParcelableCompat(ARGS_POSITION, PerpsPositionItem::class.java)
        val closeOrder = arguments?.getParcelableCompat(ARGS_CLOSE_ORDER, PerpsOrderItem::class.java)
        val source = arguments?.getString(ARGS_SOURCE) ?: AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL
        val useTradeFlowNavigator = arguments?.getBoolean(ARGS_USE_TRADE_FLOW_NAVIGATOR, false) == true
        AnalyticsTracker.trackPerpsActivityDetail(source)

        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(
                    darkTheme = context.isNightMode(),
                ) {
                    if (position != null) {
                        val lifecycleOwner = viewLifecycleOwner
                        val positionFlow = remember(position.positionId) {
                            viewModel.observePosition(position.positionId)
                        }
                        val positionState = positionFlow
                            .collectAsStateWithLifecycle(initialValue = position)

                        LaunchedEffect(position.positionId, lifecycleOwner) {
                            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                while (isActive) {
                                    viewModel.refreshSinglePosition(
                                        positionId = position.positionId,
                                        walletId = position.walletId,
                                    )
                                    delay(POSITION_REFRESH_INTERVAL_MS)
                                }
                            }
                        }

                        LaunchedEffect(positionState.value) {
                            if (positionState.value == null) {
                                activity?.onBackPressedDispatcher?.onBackPressed()
                            }
                        }

                        val currentPosition = positionState.value ?: position
                        PositionDetailPage(
                            position = currentPosition,
                            quoteColorReversed = quoteColorReversed,
                            pop = {
                                activity?.onBackPressedDispatcher?.onBackPressed()
                            },
                            onViewMarket = {
                                openViewMarket(currentPosition, useTradeFlowNavigator)
                            },
                            onClose = {
                                showCloseDialog(currentPosition, useTradeFlowNavigator)
                            },
                            onShare = {
                                sharePosition(currentPosition)
                            },
                            onSupport = {
                                context?.openUrl(
                                    Constants.HelpLink.CUSTOMER_SERVICE,
                                    source = AnalyticsTracker.CustomerServiceSource.PERPS_ACTIVITY_DETAIL,
                                    wallet = AnalyticsTracker.TradeWallet.WEB3,
                                )
                            }
                        )
                    } else if (closeOrder != null) {
                        if (closeOrder.orderType == PerpsOrder.TYPE_CLOSE) {
                            val cachedPositionFlow = remember(closeOrder.positionId) {
                                viewModel.observePosition(closeOrder.positionId)
                            }
                            val cachedPosition = cachedPositionFlow
                                .collectAsStateWithLifecycle(initialValue = null)
                            val leverage = cachedPosition.value?.leverage
                            PositionDetailPage(
                                closeOrder = closeOrder,
                                leverage = leverage,
                                quoteColorReversed = quoteColorReversed,
                                pop = {
                                    activity?.onBackPressedDispatcher?.onBackPressed()
                                },
                                onViewMarket = {
                                    openViewMarket(closeOrder, useTradeFlowNavigator)
                                },
                                onTradeAgain = {
                                    openTradeAgain(closeOrder, useTradeFlowNavigator)
                                },
                                onShare = {
                                    sharePosition(closeOrder, leverage ?: closeOrder.leverage)
                                },
                                onSupport = {
                                    context?.openUrl(
                                        Constants.HelpLink.CUSTOMER_SERVICE,
                                        source = AnalyticsTracker.CustomerServiceSource.PERPS_ACTIVITY_DETAIL,
                                        wallet = AnalyticsTracker.TradeWallet.WEB3,
                                    )
                                }
                            )
                        } else {
                            val cachedPositionFlow = remember(closeOrder.positionId) {
                                viewModel.observePosition(closeOrder.positionId)
                            }
                            val cachedPosition = cachedPositionFlow
                                .collectAsStateWithLifecycle(initialValue = null)

                            val lifecycleOwner = viewLifecycleOwner
                            LaunchedEffect(closeOrder.positionId, lifecycleOwner) {
                                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                    while (isActive) {
                                        viewModel.refreshSinglePosition(
                                            positionId = closeOrder.positionId,
                                        )
                                        delay(POSITION_REFRESH_INTERVAL_MS)
                                    }
                                }
                            }

                            OpenedOrderDetailPage(
                                openedOrder = closeOrder,
                                quoteColorReversed = quoteColorReversed,
                                pop = {
                                    activity?.onBackPressedDispatcher?.onBackPressed()
                                },
                                onViewMarket = {
                                    openViewMarket(closeOrder, useTradeFlowNavigator)
                                },
                                onShare = {
                                    lifecycleScope.launch {
                                        val closed = viewModel.getCloseOrderFromDb(closeOrder.positionId)
                                        if (closed != null && closed.orderType == PerpsOrder.TYPE_CLOSE) {
                                            sharePosition(closed, closed.leverage)
                                            return@launch
                                        }
                                        val active = cachedPosition.value ?: viewModel.getPositionFromDb(closeOrder.positionId)
                                        if (active != null) {
                                            sharePosition(active)
                                        }
                                    }
                                },
                                onSupport = {
                                    context?.openUrl(
                                        Constants.HelpLink.CUSTOMER_SERVICE,
                                        source = AnalyticsTracker.CustomerServiceSource.PERPS_ACTIVITY_DETAIL,
                                        wallet = AnalyticsTracker.TradeWallet.WEB3,
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showCloseDialog(position: PerpsPositionItem, useTradeFlowNavigator: Boolean) {
        val perpsPosition = position.toPosition()
        AnalyticsTracker.trackPerpsClosePositionStart()
        PerpsCloseBottomSheetDialogFragment.newInstance(perpsPosition)
            .setOnDone {
                showMarketDetail(
                    marketId = position.marketId,
                    marketSymbol = position.displaySymbol.orEmpty(),
                    displaySymbol = position.displaySymbol.orEmpty(),
                    tokenSymbol = position.tokenSymbol.orEmpty(),
                    source = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
                    useTradeFlowNavigator = useTradeFlowNavigator,
                )
            }
            .showNow(parentFragmentManager, PerpsCloseBottomSheetDialogFragment.TAG)
    }

    private fun openTradeAgain(order: PerpsOrderItem, useTradeFlowNavigator: Boolean) {
        val marketSymbol = order.displaySymbol.orEmpty()
        val tokenSymbol = order.tokenSymbol.orEmpty()
        val isLong = order.side.equals("long", ignoreCase = true)
        if (useTradeFlowNavigator) {
            PerpsRouteNavigator.showOpenPosition(
                fragmentManager = parentFragmentManager,
                marketId = order.marketId,
                marketSymbol = marketSymbol,
                displaySymbol = marketSymbol,
                tokenSymbol = tokenSymbol,
                isLong = isLong,
                source = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
            )
        } else {
            PerpsActivity.showOpenPosition(
                context = requireContext(),
                marketId = order.marketId,
                marketSymbol = marketSymbol,
                marketDisplaySymbol = marketSymbol,
                marketTokenSymbol = tokenSymbol,
                isLong = isLong,
                source = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
            )
        }
    }

    private fun openViewMarket(order: PerpsOrderItem, useTradeFlowNavigator: Boolean) {
        showMarketDetail(
            marketId = order.marketId,
            marketSymbol = order.displaySymbol.orEmpty(),
            displaySymbol = order.displaySymbol.orEmpty(),
            tokenSymbol = order.tokenSymbol.orEmpty(),
            source = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
            useTradeFlowNavigator = useTradeFlowNavigator,
        )
    }

    private fun openViewMarket(position: PerpsPositionItem, useTradeFlowNavigator: Boolean) {
        showMarketDetail(
            marketId = position.marketId,
            marketSymbol = position.displaySymbol.orEmpty(),
            displaySymbol = position.displaySymbol.orEmpty(),
            tokenSymbol = position.tokenSymbol.orEmpty(),
            source = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
            useTradeFlowNavigator = useTradeFlowNavigator,
        )
    }

    private fun showMarketDetail(
        marketId: String,
        marketSymbol: String,
        displaySymbol: String,
        tokenSymbol: String,
        source: String,
        useTradeFlowNavigator: Boolean,
    ) {
        if (useTradeFlowNavigator) {
            PerpsRouteNavigator.showMarketDetail(
                fragmentManager = parentFragmentManager,
                marketId = marketId,
                marketSymbol = marketSymbol,
                displaySymbol = displaySymbol,
                tokenSymbol = tokenSymbol,
                source = source,
            )
        } else {
            PerpsActivity.showDetail(
                requireContext(),
                marketId,
                marketSymbol,
                displaySymbol,
                tokenSymbol,
                source,
            )
            activity?.supportFragmentManager?.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    private fun sharePosition(position: PerpsPositionItem) {
        PerpsPositionShareBottomFragment.newInstance(position)
            .show(parentFragmentManager, PerpsPositionShareBottomFragment.TAG)
    }

    private fun sharePosition(order: PerpsOrderItem, leverage: Int) {
        PerpsPositionShareBottomFragment.newInstance(order, leverage)
            .show(parentFragmentManager, PerpsPositionShareBottomFragment.TAG)
    }

    private fun close() {
        PerpsRouteNavigator.closeTopRoute(parentFragmentManager, this)
    }

    private fun isTopFragment(): Boolean {
        return parentFragmentManager.fragments.lastOrNull { it.isVisible } == this
    }
}
