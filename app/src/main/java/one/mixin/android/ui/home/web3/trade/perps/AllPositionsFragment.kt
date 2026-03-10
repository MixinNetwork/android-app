package one.mixin.android.ui.home.web3.trade.perps

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagedList
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.databinding.FragmentAllClosedPositionsBinding
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openUrl
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.trade.ClosedPositionAdapter
import one.mixin.android.ui.home.web3.trade.TotalPositionValueAdapter
import one.mixin.android.util.viewBinding
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class AllPositionsFragment : BaseFragment(R.layout.fragment_all_closed_positions) {

    companion object {
        const val TAG = "AllPositionsFragment"
        private const val ARGS_INITIAL_TAB = "args_initial_tab"
        private const val TAB_OPEN = "tab_open"
        private const val TAB_CLOSED = "tab_closed"
        private const val POSITION_REFRESH_INTERVAL_MS = 10_000L
        private const val CLOSED_POSITION_REFRESH_LIMIT = 100

        fun newInstance(initialOpenTab: Boolean = false) = AllPositionsFragment().apply {
            arguments = Bundle().apply {
                putString(ARGS_INITIAL_TAB, if (initialOpenTab) TAB_OPEN else TAB_CLOSED)
            }
        }

        fun newOpenInstance() = newInstance(initialOpenTab = true)

        fun newClosedInstance() = newInstance(initialOpenTab = false)
    }

    @Inject
    lateinit var perpsMarketDao: PerpsMarketDao

    private val binding by viewBinding(FragmentAllClosedPositionsBinding::bind)
    private val viewModel by viewModels<PerpetualViewModel>()
    private val totalValueAdapter by lazy { TotalPositionValueAdapter() }
    private val isQuoteColorReversed by lazy {
        requireContext().defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    }

    private val openPositionAdapter by lazy {
        OpenPositionAdapter(isQuoteColorReversed) { position ->
            lifecycleScope.launch {
                val market = withContext(Dispatchers.IO) {
                    perpsMarketDao.getMarket(position.productId)
                }
                activity?.let { ctx ->
                    PerpsActivity.showDetail(
                        context = ctx,
                        marketId = position.productId,
                        marketSymbol = market?.symbol ?: "",
                        marketDisplaySymbol = market?.displaySymbol ?: "",
                        marketTokenSymbol = market?.tokenSymbol ?: ""
                    )
                }
            }
        }
    }

    private val closedPositionAdapter by lazy {
        ClosedPositionAdapter(isQuoteColorReversed) { position ->
            activity?.supportFragmentManager?.let { fm ->
                fm.beginTransaction()
                    .add(
                        android.R.id.content,
                        PositionDetailFragment.Companion.newInstance(position),
                        PositionDetailFragment.Companion.TAG
                    )
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private enum class PositionTab {
        OPEN,
        CLOSED
    }

    private var currentTab: PositionTab = PositionTab.CLOSED
    private var openPositionsLiveData: LiveData<PagedList<PerpsPositionItem>>? = null
    private var closedPositionsLiveData: LiveData<PagedList<PerpsPositionHistoryItem>>? = null
    private var totalValueJob: Job? = null
    
    private var lastOpenTotalValue: Double = 0.0
    private var lastOpenTotalPnl: Double = 0.0
    private var lastClosedTotalPnl: Double = 0.0
    private var lastClosedTotalEntryValue: Double = 0.0

    private val openPositionsObserver = Observer<PagedList<PerpsPositionItem>> { pagedList ->
        binding.progressBar.isVisible = false
        openPositionAdapter.submitList(pagedList)
        val isEmpty = pagedList.isEmpty()
        binding.emptyView.walletTransactionsEmpty.text = getString(R.string.No_Positions)
        binding.emptyView.root.isVisible = isEmpty
    }

    private val closedPositionsObserver = Observer<PagedList<PerpsPositionHistoryItem>> { pagedList ->
        binding.progressBar.isVisible = false
        closedPositionAdapter.submitList(pagedList)
        val isEmpty = pagedList.isEmpty()
        binding.emptyView.walletTransactionsEmpty.text = getString(R.string.No_Closed_Positions)
        binding.emptyView.root.isVisible = isEmpty
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.rightIb.setImageResource(R.drawable.ic_support)
            titleView.rightAnimator.visibility = View.VISIBLE
            titleView.rightAnimator.displayedChild = 0
            titleView.rightAnimator.setOnClickListener {
                context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
            }
            titleView.setSubTitle(getString(R.string.Closed_Positions), "")

            positionsRv.layoutManager = LinearLayoutManager(requireContext())

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                currentTab = if (checkedId == R.id.radio_open) {
                    PositionTab.OPEN
                } else {
                    PositionTab.CLOSED
                }
                loadPositions()
            }

            val initialTab = arguments?.getString(ARGS_INITIAL_TAB, TAB_CLOSED)
            currentTab = if (initialTab == TAB_OPEN) {
                radioOpen.isChecked = true
                PositionTab.OPEN
            } else {
                radioClosed.isChecked = true
                PositionTab.CLOSED
            }
        }

        loadPositions()
        observePeriodicRefresh()
    }

    private fun loadPositions() {
        openPositionsLiveData?.removeObservers(viewLifecycleOwner)
        closedPositionsLiveData?.removeObservers(viewLifecycleOwner)
        totalValueJob?.cancel()
        
        lastOpenTotalValue = 0.0
        lastOpenTotalPnl = 0.0
        lastClosedTotalPnl = 0.0
        lastClosedTotalEntryValue = 0.0

        if (currentTab == PositionTab.OPEN) {
            binding.titleView.setSubTitle(getString(R.string.Open_Positions_Simple), "")
            totalValueAdapter.submitTitle(R.string.Total_Position_Value)
            binding.positionsRv.adapter = ConcatAdapter(totalValueAdapter, openPositionAdapter)
            loadOpenPositions()
        } else {
            binding.titleView.setSubTitle(getString(R.string.Closed_Positions), "")
            totalValueAdapter.submitTitle(R.string.Realized_PnL)
            binding.positionsRv.adapter = ConcatAdapter(totalValueAdapter, closedPositionAdapter)
            loadClosedPositions()
        }
    }

    private fun loadOpenPositions() {
        val walletId = Session.getAccountId() ?: run {
            binding.progressBar.isVisible = false
            return
        }

        binding.progressBar.isVisible = true
        binding.emptyView.root.isVisible = false
        totalValueAdapter.submitTotal(BigDecimal.ZERO)
        totalValueAdapter.submitSubtitle(BigDecimal.ZERO, BigDecimal.ZERO)

        openPositionsLiveData = viewModel.getOpenPositionsPaged(walletId)
        openPositionsLiveData?.observe(viewLifecycleOwner, openPositionsObserver)
        observeOpenTotals(walletId)
    }

    private fun loadClosedPositions() {
        val walletId = Session.getAccountId() ?: run {
            binding.progressBar.isVisible = false
            return
        }

        binding.progressBar.isVisible = true
        binding.emptyView.root.isVisible = false
        totalValueAdapter.submitTotal(BigDecimal.ZERO)
        totalValueAdapter.submitSubtitle(BigDecimal.ZERO, BigDecimal.ZERO)

        closedPositionsLiveData = viewModel.getClosedPositionsPaged(walletId)
        closedPositionsLiveData?.observe(viewLifecycleOwner, closedPositionsObserver)
        observeClosedTotals(walletId)
    }

    private fun observeOpenTotals(walletId: String) {
        totalValueJob?.cancel()
        totalValueJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.observeTotalOpenPositionValue(walletId),
                    viewModel.observeTotalUnrealizedPnl(walletId)
                ) { totalPositionValue, totalPnl ->
                    totalPositionValue to totalPnl
                }.collect { (totalPositionValue, totalPnl) ->
                    if (lastOpenTotalValue != totalPositionValue || lastOpenTotalPnl != totalPnl) {
                        lastOpenTotalValue = totalPositionValue
                        lastOpenTotalPnl = totalPnl
                        val percent = calculatePercent(totalPnl, totalPositionValue)
                        totalValueAdapter.submitTotal(BigDecimal.valueOf(totalPositionValue))
                        totalValueAdapter.submitSubtitle(BigDecimal.valueOf(totalPnl), BigDecimal.valueOf(percent))
                    }
                }
            }
        }
    }

    private fun observeClosedTotals(walletId: String) {
        totalValueJob?.cancel()
        totalValueJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.observeTotalRealizedPnl(walletId),
                    viewModel.observeTotalClosedEntryValue(walletId)
                ) { totalPnl, totalEntryValue ->
                    totalPnl to totalEntryValue
                }.collect { (totalPnl, totalEntryValue) ->
                    if (lastClosedTotalPnl != totalPnl || lastClosedTotalEntryValue != totalEntryValue) {
                        lastClosedTotalPnl = totalPnl
                        lastClosedTotalEntryValue = totalEntryValue
                        val percent = calculatePercent(totalPnl, totalEntryValue)
                        totalValueAdapter.submitTotal(BigDecimal.valueOf(totalPnl))
                        totalValueAdapter.submitSubtitle(BigDecimal.valueOf(totalPnl), BigDecimal.valueOf(percent))
                    }
                }
            }
        }
    }

    private fun observePeriodicRefresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val walletId = Session.getAccountId() ?: return@repeatOnLifecycle
                while (isActive) {
                    viewModel.refreshPositions(walletId)
                    viewModel.refreshPositionHistory(
                        walletId = walletId,
                        limit = CLOSED_POSITION_REFRESH_LIMIT
                    )
                    delay(POSITION_REFRESH_INTERVAL_MS)
                }
            }
        }
    }

    private fun calculatePercent(value: Double, base: Double): Double {
        if (base == 0.0) {
            return 0.0
        }
        return value / base * 100
    }
}
