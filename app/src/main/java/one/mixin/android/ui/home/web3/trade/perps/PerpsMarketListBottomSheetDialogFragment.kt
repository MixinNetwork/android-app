package one.mixin.android.ui.home.web3.trade.perps

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.perps.PerpsPositionDao
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.widget.MarketSort
import one.mixin.android.util.analytics.AnalyticsTracker
import javax.inject.Inject

private const val MARKET_REFRESH_INTERVAL_MS = 3_000L

@AndroidEntryPoint
class PerpsMarketListBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PerpsMarketListBottomSheetDialogFragment"
        private const val ARGS_IS_LONG = "args_is_long"
        private const val ARGS_INITIAL_CATEGORY = "args_initial_category"
        private const val ARGS_INITIAL_SORT = "args_initial_sort"
        const val CATEGORY_STOCKS = "stocks"
        const val CATEGORY_COMMODITIES = "commodities"

        fun newInstance(isLong: Boolean) = PerpsMarketListBottomSheetDialogFragment().withArgs {
            putBoolean(ARGS_IS_LONG, isLong)
        }

        fun newInstance(
            initialCategory: String? = null,
            initialSort: MarketSort? = null,
        ) = PerpsMarketListBottomSheetDialogFragment().withArgs {
            initialCategory?.let { putString(ARGS_INITIAL_CATEGORY, it) }
            initialSort?.let { putInt(ARGS_INITIAL_SORT, it.value) }
        }
    }

    private val viewModel by viewModels<PerpetualViewModel>()

    @Inject
    lateinit var perpsPositionDao: PerpsPositionDao

    private val isLong by lazy {
        arguments?.takeIf { it.containsKey(ARGS_IS_LONG) }?.getBoolean(ARGS_IS_LONG)
    }
    private val initialCategory by lazy {
        arguments?.getString(ARGS_INITIAL_CATEGORY)
    }
    private val initialSort by lazy {
        arguments
            ?.takeIf { it.containsKey(ARGS_INITIAL_SORT) }
            ?.getInt(ARGS_INITIAL_SORT)
            ?.let(MarketSort::fromValueOrNull)
    }
    private val mutableUiState by lazy {
        MutableStateFlow(
            PerpsMarketListUiState.initial(
                initialCategory = initialCategory,
                initialSort = initialSort,
                quoteColorReversed =
                    requireContext()
                        .defaultSharedPreferences
                        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false),
            ),
        )
    }
    private val uiState: StateFlow<PerpsMarketListUiState> by lazy {
        mutableUiState.asStateFlow()
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        observeMarkets()
    }

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            val state by uiState.collectAsStateWithLifecycle()
            PerpsMarketListBottomSheetPage(
                state = state,
                onQueryChanged = { query ->
                    mutableUiState.update { it.updateQuery(query) }
                },
                onCancel = ::dismiss,
                onCategorySelected = { category ->
                    mutableUiState.update { it.selectCategory(category) }
                },
                onSort = { column ->
                    mutableUiState.update { it.selectSort(column) }
                },
                onFavorite = ::updateFavorite,
                onMarketClick = ::onMarketClick,
                onRecommendationSelected = { market ->
                    mutableUiState.update { it.toggleRecommendation(market.marketId) }
                },
                onAddRecommendations = ::addRecommendations,
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() -
            view.getSafeAreaInsetsTop() -
            requireContext().appCompatActionBarHeight()
    }

    override fun showError(error: String) = Unit

    private fun observeMarkets() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    viewModel.observeMarkets().collect { markets ->
                        mutableUiState.update { it.updateMarkets(markets) }
                    }
                }
                launch {
                    viewModel.favoriteMarketIds.collect { marketIds ->
                        mutableUiState.update { it.updateFavoriteMarketIds(marketIds) }
                    }
                }
                launch {
                    viewModel.observeFeaturedMarkets().collect { markets ->
                        mutableUiState.update { it.updateFeaturedMarkets(markets) }
                    }
                }
                launch {
                    while (isActive) {
                        viewModel.refreshMarkets()
                        viewModel.refreshFavoriteMarkets()
                        viewModel.refreshFeaturedMarkets()
                        delay(MARKET_REFRESH_INTERVAL_MS)
                    }
                }
            }
        }
    }

    private fun updateFavorite(
        market: PerpsMarket,
        isFavored: Boolean,
        onResult: (Boolean) -> Unit,
    ) {
        if (uiState.value.isFavoriteUpdatePending(market.marketId)) {
            onResult(false)
            return
        }
        mutableUiState.update { it.startFavoriteUpdate(market.marketId) }
        viewModel.updateMarketFavorite(market.marketId, isFavored) { success ->
            mutableUiState.update { it.finishFavoriteUpdate(market.marketId, success) }
            onResult(success)
            if (success) {
                AnalyticsTracker.trackMarketWatchlist(
                    adding = !isFavored,
                    type = AnalyticsTracker.MarketType.PERPS,
                    source = AnalyticsTracker.MarketWatchlistSource.PERPS_MARKETS_DIALOG,
                )
                toast(
                    getString(
                        if (isFavored) {
                            R.string.watchlist_remove_desc
                        } else {
                            R.string.watchlist_add_desc
                        },
                        market.tokenSymbol,
                    ),
                )
            }
        }
    }

    private fun addRecommendations() {
        val selectedMarkets = uiState.value.selectedRecommendations
        val selectedIds = selectedMarkets.mapTo(mutableSetOf(), PerpsMarket::marketId)
        if (selectedIds.isEmpty() || uiState.value.isAddingRecommendations) return
        mutableUiState.update { it.startRecommendationSubmission() }
        viewModel.addFavoriteMarkets(selectedIds) { addedIds ->
            if (addedIds.isNotEmpty()) {
                AnalyticsTracker.trackMarketWatchlist(
                    adding = true,
                    type = AnalyticsTracker.MarketType.PERPS,
                    source = AnalyticsTracker.MarketWatchlistSource.PERPS_MARKETS_DIALOG,
                )
                val symbols =
                    selectedMarkets
                        .filter { it.marketId in addedIds }
                        .joinToString(", ") { it.tokenSymbol }
                toast(getString(R.string.watchlist_add_desc, symbols))
            }
            mutableUiState.update { it.completeRecommendationSubmission(addedIds) }
        }
    }

    private fun onMarketClick(market: PerpsMarket) {
        lifecycleScope.launch {
            val walletId = Session.getAccountId()
            val hasOpenPosition =
                if (walletId.isNullOrEmpty()) {
                    false
                } else {
                    withContext(Dispatchers.IO) {
                        perpsPositionDao.getOpenPositions(walletId).any { it.marketId == market.marketId }
                    }
                }

            if (hasOpenPosition || isLong == null) {
                PerpsRouteNavigator.showMarketDetail(
                    fragmentManager = parentFragmentManager,
                    marketId = market.marketId,
                    marketSymbol = market.displaySymbol,
                    displaySymbol = market.displaySymbol,
                    tokenSymbol = market.tokenSymbol,
                    source = AnalyticsTracker.PerpsSource.MORE_EXPLORE,
                )
            } else {
                PerpsRouteNavigator.showOpenPosition(
                    fragmentManager = parentFragmentManager,
                    marketId = market.marketId,
                    marketSymbol = market.displaySymbol,
                    displaySymbol = market.displaySymbol,
                    tokenSymbol = market.tokenSymbol,
                    isLong = requireNotNull(isLong),
                    source = AnalyticsTracker.PerpsSource.MORE_EXPLORE,
                )
            }
            dismiss()
        }
    }
}
