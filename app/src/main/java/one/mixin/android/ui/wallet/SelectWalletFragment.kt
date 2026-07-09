package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.activityViewModels
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.components.FetchWalletState
import one.mixin.android.ui.wallet.components.SelectContent
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel
import one.mixin.android.util.viewBinding

class SelectWalletFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG = "select"
        private const val ARGS_CUSTOMER_SERVICE_SOURCE = "args_customer_service_source"
        fun newInstance(customerServiceSource: String? = null) = SelectWalletFragment().apply {
            arguments = Bundle().apply {
                customerServiceSource?.let { putString(ARGS_CUSTOMER_SERVICE_SOURCE, it) }
            }
        }
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)
    private val viewModel by activityViewModels<FetchWalletViewModel>()
    private val customerServiceSource: String? by lazy { arguments?.getString(ARGS_CUSTOMER_SERVICE_SOURCE) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { requireActivity().finish() }
        binding.titleView.setSubTitle(getString(R.string.import_wallet_title), "")
        binding.compose.setContent {
            val wallets by viewModel.wallets.collectAsState()
            val selectedWalletInfos by viewModel.selectedWalletInfos.collectAsState()
            val state by viewModel.state.collectAsState()
            SelectContent(
                wallets = wallets,
                selectedWalletInfos = selectedWalletInfos,
                onWalletToggle = viewModel::toggleWalletSelection,
                onContinue = {
                    parentFragmentManager.beginTransaction()
                        .replace(
                            R.id.container,
                            ImportingWalletFragment.newInstance(customerServiceSource),
                            ImportingWalletFragment.TAG
                        )
                        .commit()
                },
                onSelectAll = viewModel::selectAll,
                onFindMore = viewModel::findMoreWallets,
                isLoadingMore = state == FetchWalletState.FETCHING && wallets.isNotEmpty(),
            )
        }
    }
}
