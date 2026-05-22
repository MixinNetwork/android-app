package one.mixin.android.ui.home.web3.trade.perps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
        private const val POSITION_REFRESH_INTERVAL_MS = 10_000L

        fun newInstance(position: PerpsPositionItem, source: String = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL): PositionDetailFragment {
            return PositionDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_POSITION, position)
                    putString(ARGS_SOURCE, source)
                }
            }
        }

        fun newInstance(order: PerpsOrderItem, source: String = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL): PositionDetailFragment {
            return PositionDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_CLOSE_ORDER, order)
                    putString(ARGS_SOURCE, source)
                }
            }
        }
    }

    private val viewModel by viewModels<PerpetualViewModel>()

    private val quoteColorReversed: Boolean by lazy {
        requireContext().defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val position = arguments?.getParcelableCompat(ARGS_POSITION, PerpsPositionItem::class.java)
        val closeOrder = arguments?.getParcelableCompat(ARGS_CLOSE_ORDER, PerpsOrderItem::class.java)
        val source = arguments?.getString(ARGS_SOURCE) ?: AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL
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
                            onClose = {
                                showCloseDialog(currentPosition)
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
                                onTradeAgain = {
                                    openTradeAgain(closeOrder)
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
                            OpenedOrderDetailPage(
                                openedOrder = closeOrder,
                                quoteColorReversed = quoteColorReversed,
                                pop = {
                                    activity?.onBackPressedDispatcher?.onBackPressed()
                                },
                                onViewMarket = {
                                    openViewMarket(closeOrder)
                                },
                                onShare = {
                                    val active = cachedPosition.value
                                    if (active != null) {
                                        sharePosition(active)
                                    } else {
                                        sharePosition(closeOrder, closeOrder.leverage)
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

    private fun showCloseDialog(position: PerpsPositionItem) {
        val perpsPosition = position.toPosition()
        AnalyticsTracker.trackPerpsClosePositionStart()
        PerpsCloseBottomSheetDialogFragment.newInstance(perpsPosition)
            .setOnDone {
                PerpsActivity.showDetail(
                    requireContext(),
                    position.marketId,
                    position.displaySymbol.orEmpty(),
                    position.displaySymbol.orEmpty(),
                    position.tokenSymbol.orEmpty(),
                    AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
                )
                activity?.supportFragmentManager?.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
            .showNow(parentFragmentManager, PerpsCloseBottomSheetDialogFragment.TAG)
    }

    private fun openTradeAgain(order: PerpsOrderItem) {
        PerpsActivity.showDetail(
            context = requireContext(),
            marketId = order.marketId,
            marketSymbol = order.displaySymbol.orEmpty(),
            marketDisplaySymbol = order.displaySymbol.orEmpty(),
            marketTokenSymbol = order.tokenSymbol.orEmpty(),
            source = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
        )
        activity?.supportFragmentManager?.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun openViewMarket(order: PerpsOrderItem) {
        PerpsActivity.showDetail(
            context = requireContext(),
            marketId = order.marketId,
            marketSymbol = order.displaySymbol.orEmpty(),
            marketDisplaySymbol = order.displaySymbol.orEmpty(),
            marketTokenSymbol = order.tokenSymbol.orEmpty(),
            source = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
        )
        activity?.supportFragmentManager?.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun sharePosition(position: PerpsPositionItem) {
        PerpsPositionShareBottomFragment.newInstance(position)
            .show(parentFragmentManager, PerpsPositionShareBottomFragment.TAG)
    }

    private fun sharePosition(order: PerpsOrderItem, leverage: Int) {
        PerpsPositionShareBottomFragment.newInstance(order, leverage)
            .show(parentFragmentManager, PerpsPositionShareBottomFragment.TAG)
    }
}
