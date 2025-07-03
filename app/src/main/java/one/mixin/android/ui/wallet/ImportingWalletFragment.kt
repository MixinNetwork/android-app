package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.components.ImportingContent
import one.mixin.android.ui.wallet.components.FetchWalletState
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel
import one.mixin.android.util.viewBinding

class ImportingWalletFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG = "importing"
        fun newInstance() = ImportingWalletFragment()
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)
    private val viewModel by activityViewModels<FetchWalletViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { requireActivity().finish() }
        binding.compose.setContent {
            ImportingContent()
        }
    }
}
