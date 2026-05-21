package one.mixin.android.ui.home.web3.trade.perps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import one.mixin.android.Constants
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
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
        private const val ARGS_POSITION_HISTORY = "args_position_history"
        private const val ARGS_SOURCE = "args_source"
        private const val POSITION_REFRESH_INTERVAL_MS = 10_000L

        fun newInstance(position: PerpsPositionItem): PositionDetailFragment {
            return PositionDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_POSITION, position)
                }
            }
        }

        fun newInstance(position: PerpsPositionHistoryItem, source: String): PositionDetailFragment {
            return PositionDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_POSITION_HISTORY, position)
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
        val positionHistory = arguments?.getParcelableCompat(ARGS_POSITION_HISTORY, PerpsPositionHistoryItem::class.java)
        if (positionHistory != null) {
            val source = arguments?.getString(ARGS_SOURCE) ?: AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_LIST
            AnalyticsTracker.trackPerpsActivityDetail(source)
        }

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
                    } else if (positionHistory != null) {
                        PositionDetailPage(
                            positionHistory = positionHistory,
                            quoteColorReversed = quoteColorReversed,
                            pop = {
                                activity?.onBackPressedDispatcher?.onBackPressed()
                            },
                            onTradeAgain = {
                                openTradeAgain(positionHistory)
                            },
                            onShare = {
                                sharePosition(positionHistory)
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
            }
            .showNow(parentFragmentManager, PerpsCloseBottomSheetDialogFragment.TAG)
    }

    private fun openTradeAgain(positionHistory: PerpsPositionHistoryItem) {
        PerpsActivity.showDetail(
            context = requireContext(),
            marketId = positionHistory.marketId,
            marketSymbol = positionHistory.displaySymbol.orEmpty(),
            marketDisplaySymbol = positionHistory.displaySymbol.orEmpty(),
            marketTokenSymbol = positionHistory.tokenSymbol.orEmpty(),
            source = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
        )
    }

    private fun sharePosition(position: PerpsPositionItem) {
        PerpsPositionShareActivity.show(requireContext(), position)
    }

    private fun sharePosition(positionHistory: PerpsPositionHistoryItem) {
        PerpsPositionShareActivity.show(requireContext(), positionHistory)
    }
}
