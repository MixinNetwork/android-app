package one.mixin.android.ui.home.web3.trade.perps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import one.mixin.android.Constants
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openUrl
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.api.response.perps.toPosition
import one.mixin.android.ui.common.BaseFragment

@AndroidEntryPoint
class PositionDetailFragment : BaseFragment() {
    companion object {
        const val TAG = "PositionDetailFragment"
        private const val ARGS_POSITION = "args_position"
        private const val ARGS_POSITION_HISTORY = "args_position_history"
        private const val POSITION_REFRESH_INTERVAL_MS = 10_000L

        fun newInstance(position: PerpsPositionItem): PositionDetailFragment {
            return PositionDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_POSITION, position)
                }
            }
        }

        fun newInstance(position: PerpsPositionHistoryItem): PositionDetailFragment {
            return PositionDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_POSITION_HISTORY, position)
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

        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(
                    darkTheme = context.isNightMode(),
                ) {
                    if (position != null) {
                        val lifecycleOwner = LocalLifecycleOwner.current
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
                                PerpsPositionShareActivity.show(requireContext(), currentPosition)
                            },
                            onSupport = {
                                context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
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
                                PerpsPositionShareActivity.show(requireContext(), positionHistory)
                            },
                            onSupport = {
                                context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun showCloseDialog(position: PerpsPositionItem) {
        val perpsPosition = position.toPosition()
        PerpsCloseBottomSheetDialogFragment.newInstance(perpsPosition)
            .setOnDone {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            .showNow(parentFragmentManager, PerpsCloseBottomSheetDialogFragment.TAG)
    }

    private fun openTradeAgain(positionHistory: PerpsPositionHistoryItem) {
        val isLong = positionHistory.side.equals("long", ignoreCase = true)
        PerpsActivity.showOpenPosition(
            context = requireContext(),
            marketId = positionHistory.productId,
            marketSymbol = positionHistory.marketSymbol ?: positionHistory.tokenSymbol ?: "",
            marketDisplaySymbol = positionHistory.displaySymbol ?: positionHistory.tokenSymbol ?: "",
            isLong = isLong
        )
    }
}
