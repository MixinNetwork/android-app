package one.mixin.android.ui.home.web3.trade

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.perps.PositionHistoryView
import one.mixin.android.databinding.FragmentAllClosedPositionsBinding
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class AllClosedPositionsFragment : BaseFragment(R.layout.fragment_all_closed_positions) {

    companion object {
        const val TAG = "AllClosedPositionsFragment"

        fun newInstance() = AllClosedPositionsFragment()
    }

    private val binding by viewBinding(FragmentAllClosedPositionsBinding::bind)
    private val viewModel by viewModels<PerpetualViewModel>()

    private val adapter by lazy {
        ClosedPositionAdapter { position ->
            activity?.supportFragmentManager?.let { fm ->
                fm.beginTransaction()
                    .add(android.R.id.content, PositionDetailFragment.newInstance(position), PositionDetailFragment.TAG)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.setSubTitle(getString(R.string.Closed_Positions), "")

            positionsRv.layoutManager = LinearLayoutManager(requireContext())
            positionsRv.adapter = adapter
        }

        loadClosedPositions()
    }

    private fun loadClosedPositions() {
        val walletId = Session.getAccountId() ?: return

        binding.progressBar.isVisible = true
        binding.emptyView.root.isVisible = false

        lifecycleScope.launch {
            viewModel.loadPositionHistory(
                walletId = walletId,
                limit = 100,
                onSuccess = { positions ->
                    binding.progressBar.isVisible = false
                    if (positions.isEmpty()) {
                        binding.emptyView.root.isVisible = true
                        binding.positionsRv.isVisible = false
                    } else {
                        binding.emptyView.root.isVisible = false
                        binding.positionsRv.isVisible = true
                        adapter.submitList(positions)
                    }
                },
                onError = {
                    binding.progressBar.isVisible = false
                    binding.emptyView.root.isVisible = true
                    binding.positionsRv.isVisible = false
                }
            )
        }
    }
}
