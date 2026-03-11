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
        private const val ARGS_POSITION_TYPE = "args_position_type"
        private const val TYPE_OPEN = "type_open"
        private const val TYPE_CLOSED = "type_closed"
        private const val POSITION_REFRESH_INTERVAL_MS = 10_000L
        private const val CLOSED_POSITION_REFRESH_LIMIT = 100

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
                    perpsMarketDao.getMarket(position.marketId)
                }
                activity?.let { ctx ->
                    PerpsActivity.showDetail(
                        context = ctx,
                        marketId = position.marketId,
                        marketSymbol = market?.displaySymbol ?: "",
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

    private enum class PositionType {
        OPEN,
        CLOSED
    }

    private var positionType: PositionType = PositionType.CLOSED
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
            positionType = when (arguments?.getString(ARGS_POSITION_TYPE, TYPE_CLOSED)) {
                TYPE_OPEN -> PositionType.OPEN
                else -> PositionType.CLOSED
            }
            titleView.setSubTitle(
                getString(if (positionType == PositionType.OPEN) R.string.perps_positions else R.string.perps_activity),
                ""
            )

            positionsRv.layoutManager = LinearLayoutManager(requireContext())
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

        if (positionType == PositionType.OPEN) {
            binding.titleView.setSubTitle(getString(R.string.perps_positions), "")
            totalValueAdapter.submitTitle(R.string.Total_Position_Value)
            binding.positionsRv.adapter = ConcatAdapter(totalValueAdapter, openPositionAdapter)
            loadOpenPositions()
        } else {
            binding.titleView.setSubTitle(getString(R.string.perps_activity), "")
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
                        val nowValue = totalEntryValue + totalPnl
                        val percent = calculateClosedPercent(nowValue, totalEntryValue)
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

    private fun calculateClosedPercent(nowValue: Double, entryValue: Double): Double {
        if (entryValue == 0.0) {
            return 0.0
        }
        return (nowValue / entryValue - 1.0) * 100
    }
}
