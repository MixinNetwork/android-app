package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.openUrl
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.components.FetchErrorContent
import one.mixin.android.ui.wallet.components.FetchWalletState
import one.mixin.android.ui.wallet.components.FetchingContent
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel
import one.mixin.android.util.viewBinding
import javax.inject.Inject

@AndroidEntryPoint
class FetchingWalletFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG = "fetching"
        private const val ARGS_MNEMONIC = "args_mnemonic"
        private const val ARGS_PIN = "args_pin"
        private const val ARGS_IMPORT_CATEGORY = "args_import_category"
        fun newInstance(mnemonic: String?, pin: String? = null, importCategory: String? = null) = FetchingWalletFragment().apply {
            arguments = Bundle().apply {
                putString(ARGS_MNEMONIC, mnemonic)
                pin?.let { putString(ARGS_PIN, it) }
                importCategory?.let { putString(ARGS_IMPORT_CATEGORY, it) }
            }
        }
    }

    @Inject
    lateinit var tip: Tip

    private val binding by viewBinding(FragmentComposeBinding::bind)
    private val viewModel by activityViewModels<FetchWalletViewModel>()
    private val mnemonic: String? by lazy { arguments?.getString(ARGS_MNEMONIC) }
    private val pin: String? by lazy { arguments?.getString(ARGS_PIN) }
    private val importCategory: String? by lazy { arguments?.getString(ARGS_IMPORT_CATEGORY) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { requireActivity().finish() }
        binding.titleView.rightIb.setImageResource(R.drawable.ic_support)
        binding.titleView.rightAnimator.visibility = View.VISIBLE
        binding.titleView.rightAnimator.displayedChild = 0
        binding.titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE) }
        binding.compose.setContent {
            val state by viewModel.state.collectAsState()
            val errorMessage by viewModel.errorMessage.collectAsState()
            when (state) {
                FetchWalletState.FETCH_ERROR -> {
                    FetchErrorContent(
                        errorMessage = errorMessage,
                        onRetry = viewModel::retryFetching,
                    )
                }
                else -> {
                    FetchingContent()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            importCategory?.let { viewModel.setImportCategory(it) }
            pin?.let { pin ->
                val tipPriv = tip.getOrRecoverTipPriv(requireContext(), pin).getOrThrow()
                val spendKey = tip.getSpendPrivFromEncryptedSalt(
                    tip.getMnemonicFromEncryptedPreferences(requireContext()),
                    tip.getEncryptedSalt(requireContext()),
                    pin,
                    tipPriv,
                )
                viewModel.setSpendKey(spendKey)
            }
            viewModel.setMnemonic(mnemonic.orEmpty())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (state == FetchWalletState.SELECT) {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.container, SelectWalletFragment.newInstance(), SelectWalletFragment.TAG)
                        .commit()
                }
            }
        }
    }
}
