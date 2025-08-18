package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.components.FetchWalletState
import one.mixin.android.ui.wallet.components.FetchingContent
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel
import one.mixin.android.util.viewBinding

class FetchingWalletFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG = "fetching"
        private const val ARGS_MNEMONIC = "args_mnemonic"
        fun newInstance(mnemonic: String?) = FetchingWalletFragment().apply {
            arguments = Bundle().apply {
                putString(ARGS_MNEMONIC, mnemonic)
            }
        }
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)
    private val viewModel by activityViewModels<FetchWalletViewModel>()
    private val mnemonic: String? by lazy { arguments?.getString(ARGS_MNEMONIC) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { requireActivity().finish() }
        binding.titleView.rightIb.setImageResource(R.drawable.ic_support)
        binding.titleView.rightAnimator.visibility = View.VISIBLE
        binding.titleView.rightAnimator.displayedChild = 0
        binding.titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE) }
        binding.compose.setContent {
            FetchingContent()
        }
        viewModel.setMnemonic(mnemonic.orEmpty())
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (state == FetchWalletState.SELECT) {
                    navTo(SelectWalletFragment.newInstance(), SelectWalletFragment.TAG)
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .remove(this@FetchingWalletFragment)
                        .commit()
                }
            }
        }
    }
}
