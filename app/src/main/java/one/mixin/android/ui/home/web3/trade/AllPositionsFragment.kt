package one.mixin.android.ui.home.web3.trade

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
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
        if (currentTab == PositionTab.OPEN) {
            binding.titleView.setSubTitle(getString(R.string.Open_Positions_Simple), "")
            binding.positionsRv.adapter = ConcatAdapter(totalValueAdapter, openPositionAdapter)
            loadOpenPositions()
        } else {
            binding.titleView.setSubTitle(getString(R.string.Closed_Positions), "")
            binding.positionsRv.adapter = ConcatAdapter(totalValueAdapter, closedPositionAdapter)
            loadClosedPositions()
        }
    }

    private fun loadOpenPositions() {
        val walletId = Session.getAccountId() ?: return

        binding.progressBar.isVisible = true
        binding.emptyView.root.isVisible = false

        lifecycleScope.launch {
            val positions = viewModel.getOpenPositionsFromDb(walletId)
            binding.progressBar.isVisible = false
            if (positions.isEmpty()) {
                openPositionAdapter.submitList(emptyList())
                binding.emptyView.infoTv.text = getString(R.string.No_Positions)
                binding.emptyView.root.isVisible = true
                totalValueAdapter.submitTotal(BigDecimal.ZERO)
            } else {
                binding.emptyView.root.isVisible = false
                openPositionAdapter.submitList(positions)
                totalValueAdapter.submitTotal(sumOpenPnl(positions))
            }
        }
    }

    private fun loadClosedPositions() {
        val walletId = Session.getAccountId() ?: return

        binding.progressBar.isVisible = true
        binding.emptyView.root.isVisible = false

        lifecycleScope.launch {
            val positions = viewModel.getClosedPositionsFromDb(walletId, 100)
            binding.progressBar.isVisible = false
            if (positions.isEmpty()) {
                closedPositionAdapter.submitList(emptyList())
                binding.emptyView.infoTv.text = getString(R.string.No_Closed_Positions)
                binding.emptyView.root.isVisible = true
                totalValueAdapter.submitTotal(BigDecimal.ZERO)
            } else {
                binding.emptyView.root.isVisible = false
                closedPositionAdapter.submitList(positions)
                totalValueAdapter.submitTotal(sumClosedPnl(positions))
            }
        }
    }

    private fun sumOpenPnl(positions: List<PerpsPositionItem>): BigDecimal {
        return positions.fold(BigDecimal.ZERO) { total, position ->
            val pnl = (position.unrealizedPnl ?: "0").toBigDecimalOrNull() ?: BigDecimal.ZERO
            total + pnl
        }
    }

    private fun sumClosedPnl(positions: List<PerpsPositionHistoryItem>): BigDecimal {
        return positions.fold(BigDecimal.ZERO) { total, position ->
            val pnl = position.realizedPnl.toBigDecimalOrNull() ?: BigDecimal.ZERO
            total + pnl
        }
    }
}
