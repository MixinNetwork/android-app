package one.mixin.android.ui.home.web3.trade.perps

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.databinding.FragmentMarketListBottomSheetBinding
import one.mixin.android.db.perps.PerpsPositionDao
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView
import javax.inject.Inject

private const val MARKET_REFRESH_INTERVAL_MS = 3_000L

@AndroidEntryPoint
class PerpsMarketListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PerpsMarketListBottomSheetDialogFragment"
        private const val ARGS_IS_LONG = "args_is_long"

        fun newInstance() = PerpsMarketListBottomSheetDialogFragment()

        fun newInstance(isLong: Boolean) = PerpsMarketListBottomSheetDialogFragment().withArgs {
            putBoolean(ARGS_IS_LONG, isLong)
        }
    }

    private val binding by viewBinding(FragmentMarketListBottomSheetBinding::inflate)
    private val isQuoteColorReversed by lazy {
        requireContext().defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    }
    private val adapter by lazy {
        PerpsMarketListAdapter(isQuoteColorReversed) { market -> onMarketClick(market) }
    }
    private val viewModel by viewModels<PerpetualViewModel>()

    @Inject
    lateinit var perpsPositionDao: PerpsPositionDao

    private val isLong by lazy {
        arguments?.takeIf { it.containsKey(ARGS_IS_LONG) }?.getBoolean(ARGS_IS_LONG)
    }
    private var allMarkets = listOf<PerpsMarket>()
    private var currentQuery = ""

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root

        binding.ph.doOnPreDraw {
            binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
                height = binding.ph.getSafeAreaInsetsTop() + requireContext().appCompatActionBarHeight()
            }
        }

        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.apply {
            closeIb.setOnClickListener {
                dismiss()
            }

            marketRv.layoutManager = LinearLayoutManager(requireContext())
            marketRv.adapter = adapter
            (marketRv.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

            searchEt.listener = object : SearchView.OnSearchViewListener {
                override fun afterTextChanged(s: Editable?) {
                    currentQuery = s?.toString().orEmpty()
                    filterMarkets(currentQuery)
                }

                override fun onSearch() {}
            }
        }

        observeMarkets()
    }

    private fun observeMarkets() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    viewModel.observeMarkets().collect { markets ->
                        allMarkets = markets
                        filterMarkets(currentQuery)
                    }
                }
                launch {
                    while (isActive) {
                        viewModel.refreshMarkets()
                        delay(MARKET_REFRESH_INTERVAL_MS)
                    }
                }
            }
        }
    }

    private fun filterMarkets(query: String) {
        if (query.isEmpty()) {
            updateList(allMarkets)
        } else {
            val filtered = allMarkets.filter { market ->
                market.tokenSymbol.contains(query, ignoreCase = true)
            }
            updateList(filtered)
        }
    }

    private fun updateList(markets: List<PerpsMarket>) {
        binding.rvVa.displayedChild = if (markets.isEmpty()) 1 else 0
        adapter.submitList(markets)
    }

    private fun onMarketClick(market: PerpsMarket) {
        lifecycleScope.launch {
            val walletId = Session.getAccountId()
            val hasOpenPosition = if (walletId.isNullOrEmpty()) {
                false
            } else {
                withContext(Dispatchers.IO) {
                    perpsPositionDao.getOpenPositions(walletId).any { it.marketId == market.marketId }
                }
            }

            if (hasOpenPosition || isLong == null) {
                PerpsActivity.showDetail(
                    context = requireContext(),
                    marketId = market.marketId,
                    marketSymbol = market.displaySymbol,
                    marketDisplaySymbol = market.displaySymbol,
                    marketTokenSymbol = market.tokenSymbol
                )
            } else {
                PerpsActivity.showOpenPosition(
                    context = requireContext(),
                    marketId = market.marketId,
                    marketSymbol = market.displaySymbol,
                    marketDisplaySymbol = market.displaySymbol,
                    marketTokenSymbol = market.tokenSymbol,
                    isLong = requireNotNull(isLong)
                )
            }
            dismiss()
        }
    }
}
