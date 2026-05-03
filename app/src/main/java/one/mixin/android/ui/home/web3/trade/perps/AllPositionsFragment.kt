package one.mixin.android.ui.home.web3.trade.perps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.common.BaseFragment
import javax.inject.Inject

@AndroidEntryPoint
class AllPositionsFragment : BaseFragment() {

    companion object {
        const val TAG = "AllPositionsFragment"
        private const val ARGS_POSITION_TYPE = "args_position_type"
        private const val TYPE_OPEN = "type_open"
        private const val TYPE_CLOSED = "type_closed"

        fun newInstance(showOpenPositions: Boolean = false) = AllPositionsFragment().apply {
            arguments = Bundle().apply {
                putString(ARGS_POSITION_TYPE, if (showOpenPositions) TYPE_OPEN else TYPE_CLOSED)
            }
        }

        fun newOpenInstance() = newInstance(showOpenPositions = true)

        fun newClosedInstance() = newInstance(showOpenPositions = false)
    }

    @Inject
    lateinit var perpsMarketDao: PerpsMarketDao

    private val viewModel by viewModels<PerpetualViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val positionType = when (arguments?.getString(ARGS_POSITION_TYPE, TYPE_CLOSED)) {
            TYPE_OPEN -> AllPositionsType.OPEN
            else -> AllPositionsType.CLOSED
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
                            context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                        },
                        onShowTradingGuide = {
                            PerpetualGuideBottomSheetDialogFragment.newInstance(
                                initialTab = PerpetualGuideBottomSheetDialogFragment.TAB_OVERVIEW
                            ).show(parentFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                        },
                        onOpenPositionClick = { position ->
                            lifecycleScope.launch {
                                val market = withContext(Dispatchers.IO) {
                                    perpsMarketDao.getMarket(position.marketId)
                                }
                                activity?.let { ctx ->
                                    PerpsActivity.showDetail(
                                        context = ctx,
                                        marketId = position.marketId,
                                        marketSymbol = market?.displaySymbol ?: "",
                                        marketDisplaySymbol = market?.displaySymbol ?: "",
                                        marketTokenSymbol = market?.tokenSymbol ?: "",
                                    )
                                }
                            }
                        },
                        onClosedPositionClick = { position ->
                            activity?.supportFragmentManager?.let { fm ->
                                fm.beginTransaction()
                                    .setCustomAnimations(
                                        R.anim.slide_in_right,
                                        0,
                                        0,
                                        R.anim.slide_out_right,
                                    )
                                    .add(
                                        android.R.id.content,
                                        PositionDetailFragment.newInstance(position),
                                        PositionDetailFragment.TAG,
                                    )
                                    .addToBackStack(null)
                                    .commit()
                            }
                        },
                    )
                }
            }
        }
    }
}
