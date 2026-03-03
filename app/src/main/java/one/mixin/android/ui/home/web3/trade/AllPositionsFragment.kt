package one.mixin.android.ui.home.web3.trade

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.databinding.FragmentAllClosedPositionsBinding
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding
import java.math.BigDecimal

@AndroidEntryPoint
class AllPositionsFragment : BaseFragment(R.layout.fragment_all_closed_positions) {

    companion object {
        const val TAG = "AllPositionsFragment"

        fun newInstance() = AllPositionsFragment()
    }

    private val binding by viewBinding(FragmentAllClosedPositionsBinding::bind)
    private val viewModel by viewModels<PerpetualViewModel>()
    private val totalValueAdapter by lazy { TotalPositionValueAdapter() }

    private val openPositionAdapter by lazy {
        OpenPositionAdapter { position ->
            activity?.supportFragmentManager?.let { fm ->
                fm.beginTransaction()
                    .add(android.R.id.content, PositionDetailFragment.newInstance(position), PositionDetailFragment.TAG)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private val closedPositionAdapter by lazy {
        ClosedPositionAdapter { position ->
            activity?.supportFragmentManager?.let { fm ->
                fm.beginTransaction()
                    .add(android.R.id.content, PositionDetailFragment.newInstance(position), PositionDetailFragment.TAG)
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

    private val openPositionsObserver = Observer<PagedList<PerpsPositionItem>> { pagedList ->
        binding.progressBar.isVisible = false
        openPositionAdapter.submitList(pagedList)
        val isEmpty = pagedList.isEmpty()
        binding.emptyView.infoTv.text = getString(R.string.No_Positions)
        binding.emptyView.root.isVisible = isEmpty
    }

    private val closedPositionsObserver = Observer<PagedList<PerpsPositionHistoryItem>> { pagedList ->
        binding.progressBar.isVisible = false
        closedPositionAdapter.submitList(pagedList)
        val isEmpty = pagedList.isEmpty()
        binding.emptyView.infoTv.text = getString(R.string.No_Closed_Positions)
        binding.emptyView.root.isVisible = isEmpty
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
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

            radioClosed.isChecked = true
            loadPositions()
        }
    }

    private fun loadPositions() {
        openPositionsLiveData?.removeObservers(viewLifecycleOwner)
        closedPositionsLiveData?.removeObservers(viewLifecycleOwner)

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

        lifecycleScope.launch {
            val totalPositionValue = viewModel.getTotalOpenPositionValueFromDb(walletId)
            val totalPnl = viewModel.getTotalUnrealizedPnlFromDb(walletId)
            val percent = calculatePercent(totalPnl, totalPositionValue)
            totalValueAdapter.submitTotal(BigDecimal.valueOf(totalPositionValue))
            totalValueAdapter.submitSubtitle(BigDecimal.valueOf(totalPnl), BigDecimal.valueOf(percent))
        }
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

        lifecycleScope.launch {
            val totalPnl = viewModel.getTotalRealizedPnlFromDb(walletId)
            val totalEntryValue = viewModel.getTotalClosedEntryValueFromDb(walletId)
            val percent = calculatePercent(totalPnl, totalEntryValue)
            totalValueAdapter.submitTotal(BigDecimal.valueOf(totalPnl))
            totalValueAdapter.submitSubtitle(BigDecimal.valueOf(totalPnl), BigDecimal.valueOf(percent))
        }
    }

    private fun calculatePercent(value: Double, base: Double): Double {
        if (base == 0.0) {
            return 0.0
        }
        return value / base * 100
    }
}
