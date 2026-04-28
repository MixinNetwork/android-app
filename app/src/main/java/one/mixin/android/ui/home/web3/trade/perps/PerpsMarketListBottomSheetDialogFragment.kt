package one.mixin.android.ui.home.web3.trade.perps

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.databinding.FragmentMarketListBottomSheetBinding
import one.mixin.android.db.perps.PerpsPositionDao
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.scrollToCenterCheckedRadio
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.widget.MarketSort
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView
import javax.inject.Inject

private const val MARKET_REFRESH_INTERVAL_MS = 3_000L

@AndroidEntryPoint
class PerpsMarketListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    private enum class MarketCategory(val apiValues: Set<String>) {
        ALL(emptySet()),
        CRYPTO(setOf("crypto")),
        STOCKS(setOf("stock", "stocks")),
        INDICES(setOf("index", "indices")),
        COMMODITIES(setOf("commodity", "commodities")),
        FOREX(setOf("forex", "fx")),
    }

    companion object {
        const val TAG = "PerpsMarketListBottomSheetDialogFragment"
        private const val ARGS_IS_LONG = "args_is_long"
        private const val ARGS_INITIAL_CATEGORY = "args_initial_category"
        const val CATEGORY_STOCKS = "stocks"
        const val CATEGORY_COMMODITIES = "commodities"

        fun newInstance() = PerpsMarketListBottomSheetDialogFragment()

        fun newInstance(isLong: Boolean) = PerpsMarketListBottomSheetDialogFragment().withArgs {
            putBoolean(ARGS_IS_LONG, isLong)
        }

        fun newInstance(initialCategory: String? = null) = PerpsMarketListBottomSheetDialogFragment().withArgs {
            initialCategory?.let { putString(ARGS_INITIAL_CATEGORY, it) }
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
    private val initialCategory by lazy {
        arguments?.getString(ARGS_INITIAL_CATEGORY)
    }
    private var allMarkets = listOf<PerpsMarket>()
    private var currentQuery = ""
    private var currentCategory = MarketCategory.ALL
    private var currentSort: MarketSort? = null

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
            priceTitle.text = getString(R.string.change_percent_period_hour, 24)
            closeIb.setOnClickListener {
                dismiss()
            }

            marketRv.layoutManager = LinearLayoutManager(requireContext())
            marketRv.adapter = adapter
            (marketRv.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
            applyInitialCategory()
            categoryScroll.scrollToCenterCheckedRadio(categoryGroup)

            searchEt.listener = object : SearchView.OnSearchViewListener {
                override fun afterTextChanged(s: Editable?) {
                    currentQuery = s?.toString().orEmpty()
                    filterAndSortMarkets()
                }

                override fun onSearch() {}
            }

            categoryGroup.setOnCheckedChangeListener { group, checkedId ->
                currentCategory = when (checkedId) {
                    R.id.radio_crypto -> MarketCategory.CRYPTO
                    R.id.radio_stocks -> MarketCategory.STOCKS
                    R.id.radio_indices -> MarketCategory.INDICES
                    R.id.radio_commodities -> MarketCategory.COMMODITIES
                    R.id.radio_forex -> MarketCategory.FOREX
                    else -> MarketCategory.ALL
                }
                currentSort = null
                renderSortState()
                categoryScroll.scrollToCenterCheckedRadio(group)
                filterAndSortMarkets()
            }

            volumeSort.setOnClickListener {
                updateSort(nextSort(currentSort, MarketSort.RANK_ASCENDING, MarketSort.RANK_DESCENDING))
            }
            changeSort.setOnClickListener {
                updateSort(nextSort(currentSort, MarketSort.PRICE_ASCENDING, MarketSort.PRICE_DESCENDING))
            }
            priceSort.setOnClickListener {
                updateSort(
                    nextSort(
                        currentSort,
                        MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING,
                        MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_DESCENDING,
                    )
                )
            }
        }

        renderSortState()
        observeMarkets()
    }

    private fun applyInitialCategory() {
        when (initialCategory) {
            CATEGORY_STOCKS -> {
                currentCategory = MarketCategory.STOCKS
                binding.radioStocks.isChecked = true
            }
            CATEGORY_COMMODITIES -> {
                currentCategory = MarketCategory.COMMODITIES
                binding.radioCommodities.isChecked = true
            }
        }
    }

    private fun observeMarkets() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    viewModel.observeMarkets().collect { markets ->
                        allMarkets = markets
                        filterAndSortMarkets()
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

    private fun nextSort(current: MarketSort?, ascending: MarketSort, descending: MarketSort): MarketSort? {
        return when (current) {
            ascending -> descending
            descending -> null
            else -> ascending
        }
    }

    private fun updateSort(sort: MarketSort?) {
        currentSort = sort
        renderSortState()
        filterAndSortMarkets(scrollToTop = true)
    }

    private fun renderSortState() {
        binding.apply {
            resetSortIcon(volumeIcon)
            resetSortIcon(priceIcon)
            resetSortIcon(changeIcon)

            when (currentSort) {
                MarketSort.RANK_ASCENDING, MarketSort.RANK_DESCENDING -> {
                    volumeIcon.setImageResource(
                        if (currentSort == MarketSort.RANK_ASCENDING) R.drawable.ic_perps_sort_asc else R.drawable.ic_perps_sort_desc
                    )
                }

                MarketSort.PRICE_ASCENDING, MarketSort.PRICE_DESCENDING -> {
                    changeIcon.setImageResource(
                        if (currentSort == MarketSort.PRICE_ASCENDING) R.drawable.ic_perps_sort_asc else R.drawable.ic_perps_sort_desc
                    )
                }

                MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING, MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_DESCENDING -> {
                    priceIcon.setImageResource(
                        if (currentSort == MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING) R.drawable.ic_perps_sort_asc else R.drawable.ic_perps_sort_desc
                    )
                }

                else -> Unit
            }
        }
    }

    private fun resetSortIcon(icon: ImageView) {
        icon.setImageResource(R.drawable.ic_perps_sort_default)
        icon.isVisible = true
    }

    private fun filterAndSortMarkets(scrollToTop: Boolean = false) {
        val query = currentQuery.trim()
        val sourceOrder = allMarkets.withIndex().associate { it.value.marketId to it.index }
        val filtered = allMarkets
            .asSequence()
            .filter { market ->
                currentCategory.apiValues.isEmpty() ||
                    currentCategory.apiValues.any { category -> market.category.equals(category, ignoreCase = true) }
            }
            .filter { market ->
                query.isEmpty() || market.matchesSearchQuery(query)
            }
            .toList()
            .let { markets ->
                currentComparator()?.let { comparator ->
                    markets.sortedWith(comparator)
                } ?: markets.sortedBy { market ->
                    sourceOrder[market.marketId] ?: Int.MAX_VALUE
                }
            }

        updateList(filtered, scrollToTop)
    }

    private fun updateList(markets: List<PerpsMarket>, scrollToTop: Boolean = false) {
        binding.rvVa.displayedChild = if (markets.isEmpty()) 1 else 0
        adapter.submitList(markets) {
            if (scrollToTop && markets.isNotEmpty()) {
                binding.marketRv.scrollToPosition(0)
            }
        }
    }

    private fun currentComparator(): Comparator<PerpsMarket>? {
        return when (currentSort) {
            MarketSort.RANK_ASCENDING -> compareByDescending { it.volumeDecimal() }
            MarketSort.RANK_DESCENDING -> compareBy { it.volumeDecimal() }
            MarketSort.PRICE_ASCENDING -> compareBy { it.lastDecimal() }
            MarketSort.PRICE_DESCENDING -> compareByDescending { it.lastDecimal() }
            MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING -> compareBy { it.changePercent() }
            MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_DESCENDING -> compareByDescending<PerpsMarket> { it.changePercent() }
            else -> null
        }?.thenBy { it.tokenSymbol }
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

    private fun PerpsMarket.matchesSearchQuery(query: String): Boolean {
        return tokenSymbol.contains(query, ignoreCase = true) ||
            tags.orEmpty().any { it.contains(query, ignoreCase = true) }
    }

    private fun PerpsMarket.volumeDecimal() = volume.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO

    private fun PerpsMarket.lastDecimal() = last.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
}
