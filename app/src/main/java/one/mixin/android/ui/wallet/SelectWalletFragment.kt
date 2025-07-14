package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.components.FetchWalletState
import one.mixin.android.ui.wallet.components.SelectContent
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel
import one.mixin.android.util.viewBinding

class SelectWalletFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG = "select"
        fun newInstance() = SelectWalletFragment()
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)
    private val viewModel by activityViewModels<FetchWalletViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { requireActivity().finish() }
        binding.compose.setContent {
            val wallets by viewModel.wallets.collectAsState()
            val selectedWalletInfos by viewModel.selectedWalletInfos.collectAsState()
            val state by viewModel.state.collectAsState()
            SelectContent(
                wallets = wallets,
                selectedWalletInfos = selectedWalletInfos,
                onWalletToggle = viewModel::toggleWalletSelection,
                onContinue = viewModel::startImporting,
                onBackPressed = { requireActivity().finish() },
                onSelectAll = viewModel::selectAll,
                onFindMore = viewModel::findMoreWallets,
                isLoadingMore = state == FetchWalletState.FETCHING && wallets.isNotEmpty(),
            )
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (state == FetchWalletState.IMPORTING) {
                    navTo(
                        ImportingWalletFragment.newInstance(),
                        ImportingWalletFragment.TAG
                    )
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .remove(this@SelectWalletFragment)
                        .commit()
                }
            }
        }
    }
}
