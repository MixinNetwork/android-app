package one.mixin.android.ui.home.web3

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.event.GlobalMarketEvent
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshGlobalWeb3MarketJob
import one.mixin.android.job.RefreshMarketsJob
import one.mixin.android.ui.common.Web3Fragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.home.web3.market.MarketListEntry
import one.mixin.android.ui.home.web3.market.MarketPage
import one.mixin.android.ui.home.web3.market.MarketPageViewModel
import one.mixin.android.ui.home.web3.trade.perps.PerpsActivity
import one.mixin.android.ui.search.SearchExploreFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.WalletActivity.Destination
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.rxpermission.RxPermissions
import javax.inject.Inject

@AndroidEntryPoint
class MarketFragment : Web3Fragment() {
    companion object {
        const val TAG = "MarketFragment"
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private val viewModel by viewModels<MarketPageViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(inflater.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MixinAppTheme(darkTheme = context.isNightMode()) {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    var showDisplaySettings by remember { mutableStateOf(false) }
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

                    DisposableEffect(lifecycleOwner) {
                        val observer =
                            LifecycleEventObserver { _, event ->
                                when (event) {
                                    Lifecycle.Event.ON_RESUME -> viewModel.startPerpetualRefresh()
                                    Lifecycle.Event.ON_PAUSE -> viewModel.stopPerpetualRefresh()
                                    else -> Unit
                                }
                            }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                            viewModel.stopPerpetualRefresh()
                        }
                    }

                    MarketPage(
                        state = state,
                        showDisplaySettings = showDisplaySettings,
                        onSearch = ::showSearch,
                        onScan = ::showScan,
                        onShowDisplaySettings = { showDisplaySettings = true },
                        onDismissDisplaySettings = { showDisplaySettings = false },
                        onApplyDisplaySettings = viewModel::applyDisplaySettings,
                        onSelectTopTab = viewModel::selectTopTab,
                        onSelectSubTab = viewModel::selectSubTab,
                        onSort = viewModel::updateSort,
                        onFavorite = viewModel::toggleFavorite,
                        onToggleRecommendation = viewModel::toggleRecommendation,
                        onAddRecommendations = viewModel::addSelectedRecommendations,
                        onKeepPriceAlerts = viewModel::keepPriceAlerts,
                        onDeletePriceAlerts = viewModel::deletePriceAlerts,
                        onEntryClick = ::openMarket,
                    )
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        RxBus.listen(GlobalMarketEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe { viewModel.loadIndicator() }
        updateUI()
    }

    override fun updateUI() {
        if (!::jobManager.isInitialized) return
        jobManager.addJobInBackground(RefreshMarketsJob())
        jobManager.addJobInBackground(RefreshMarketsJob("favorite"))
        jobManager.addJobInBackground(RefreshGlobalWeb3MarketJob())
        viewModel.refreshAll()
    }

    private fun showSearch() {
        activity?.addFragment(
            this,
            SearchExploreFragment.newInstance(true),
            SearchExploreFragment.TAG,
            id = R.id.internal_container,
        )
    }

    private fun showScan() {
        RxPermissions(requireActivity())
            .request(Manifest.permission.CAMERA)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    (requireActivity() as? MainActivity)?.showCapture(true)
                } else {
                    context?.openPermissionSetting()
                }
            }
    }

    private fun openMarket(entry: MarketListEntry) {
        when (entry) {
            is MarketListEntry.Spot ->
                WalletActivity.showWithMarket(
                    requireActivity(),
                    entry.market,
                    Destination.Market,
                    if (entry.isFavored) {
                        AnalyticsTracker.MarketSource.MORE_FAVORITES
                    } else {
                        AnalyticsTracker.MarketSource.MORE_MARKET_CAP
                    },
                )

            is MarketListEntry.Perpetual ->
                PerpsActivity.showDetail(
                    context = requireContext(),
                    marketId = entry.market.marketId,
                    marketSymbol = entry.market.displaySymbol,
                    marketDisplaySymbol = entry.market.displaySymbol,
                    marketTokenSymbol = entry.market.tokenSymbol,
                    source = AnalyticsTracker.PerpsSource.MORE_EXPLORE,
                )
        }
    }
}
