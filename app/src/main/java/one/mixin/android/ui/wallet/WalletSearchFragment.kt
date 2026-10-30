package one.mixin.android.ui.wallet

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.inscription.InscriptionActivity
import one.mixin.android.ui.home.web3.market.MarketSearchFragment
import one.mixin.android.ui.home.web3.trade.perps.PerpsActivity
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.safe.TokenItem

@AndroidEntryPoint
class WalletSearchFragment : BaseFragment() {
    private val viewModel by viewModels<WalletSearchViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MixinAppTheme(darkTheme = context.isNightMode()) {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
                    WalletSearchPage(
                        state = state,
                        recentSearches = recentSearches,
                        onQueryChanged = viewModel::updateQuery,
                        onCancel = {
                            view?.hideKeyboard()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        },
                        onSelectTab = viewModel::selectTab,
                        onClearRecent = viewModel::clearRecentSearches,
                        onResultClick = ::openResult,
                        onSearchMarkets = {
                            view?.hideKeyboard()
                            findNavController().navigate(
                                R.id.action_wallet_search_to_market_search,
                                bundleOf(
                                    MarketSearchFragment.ARGS_QUERY to state.query.trim(),
                                    MarketSearchFragment.ARGS_MARKET_SOURCE to AnalyticsTracker.MarketSource.WALLET_SEARCH,
                                    MarketSearchFragment.ARGS_PERPS_SOURCE to AnalyticsTracker.PerpsSource.WALLET_SEARCH,
                                ),
                                NavOptions.Builder()
                                    .setPopUpTo(R.id.wallet_search_fragment, true)
                                    .setEnterAnim(R.anim.slide_in_right)
                                    .setPopExitAnim(R.anim.slide_out_right)
                                    .build(),
                            )
                        },
                    )
                }
            }
        }

    override fun onStart() {
        super.onStart()
        viewModel.loadRecentSearches()
    }

    private fun openResult(result: WalletSearchResult) {
        view?.hideKeyboard()
        when (result) {
            is WalletSearchResult.Asset -> openAsset(result.token.assetId, result.token)
            is WalletSearchResult.Position -> {
                viewModel.saveRecentSearch(result)
                viewLifecycleOwner.lifecycleScope.launch {
                    val position = result.position
                    val market = viewModel.findPerpetualMarket(position.marketId)
                    PerpsActivity.showDetail(
                        requireContext(),
                        position.marketId,
                        market?.displaySymbol ?: position.displaySymbol.orEmpty(),
                        market?.displaySymbol ?: position.displaySymbol.orEmpty(),
                        market?.tokenSymbol ?: position.tokenSymbol.orEmpty(),
                        AnalyticsTracker.PerpsSource.WALLET_SEARCH,
                    )
                }
            }
            is WalletSearchResult.Earn -> {
                val earn = Uri.parse("${Constants.Scheme.APPS}/${Constants.MIXIN_EARN_USER_ID}")
                    .buildUpon()
                    .appendQueryParameter("action", "open")
                result.account.productionId?.takeIf { it.isNotBlank() }?.let { earn.appendQueryParameter("production", it) }
                earn.build().toString().openAsUrlOrWeb(requireActivity(), null, parentFragmentManager, viewLifecycleOwner.lifecycleScope)
            }
            is WalletSearchResult.Collectible -> InscriptionActivity.show(requireContext(), result.collectible.inscriptionHash)
        }
    }

    private fun openAsset(assetId: String, token: TokenItem?) {
        AnalyticsTracker.trackSearchItemSelect(AnalyticsTracker.SearchItemType.ASSET, AnalyticsTracker.SearchSource.WALLET)
        viewLifecycleOwner.lifecycleScope.launch {
            val asset = token ?: run {
                val dialog = indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                    setCancelable(false)
                    show()
                }
                try {
                    viewModel.findOrSyncAsset(assetId)
                } finally {
                    dialog.dismiss()
                }
            } ?: return@launch
            viewModel.saveRecentSearch(WalletSearchResult.Asset(asset))
            findNavController().navigate(
                R.id.action_wallet_search_to_transactions,
                bundleOf(TransactionsFragment.ARGS_ASSET to asset),
            )
        }
    }

}
