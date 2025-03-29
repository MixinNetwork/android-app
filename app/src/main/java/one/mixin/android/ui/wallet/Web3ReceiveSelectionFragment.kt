package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Web3ChainIds
import one.mixin.android.databinding.FragmentWeb3ReceiveSelectionBinding
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.web3.send.TokenListBottomSheetDialogFragment

@AndroidEntryPoint
class Web3ReceiveSelectionFragment : BaseFragment() {
    companion object {
        const val TAG = "Web3ReceiveSelectionFragment"
        private const val ARGS_ADDRESS = "args_address"
        private const val ARGS_CHAIN_ID = "args_chain_id"

        fun newInstance(address: String, chainId: String) = Web3ReceiveSelectionFragment().apply {
            arguments = Bundle().apply {
                putString(ARGS_ADDRESS, address)
                putString(ARGS_CHAIN_ID, chainId)
            }
        }
    }

    private var _binding: FragmentWeb3ReceiveSelectionBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val web3ViewModel by viewModels<Web3ViewModel>()
    
    private val address by lazy { requireArguments().getString(ARGS_ADDRESS, "") }
    private val chainId by lazy { requireArguments().getString(ARGS_CHAIN_ID, "") }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWeb3ReceiveSelectionBinding.inflate(inflater, container, false).apply { this.root.setOnClickListener { } }
        binding.root.setOnClickListener { }
        binding.title.setOnClickListener { }
        binding.title.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        binding.walletRl.setOnClickListener {
            lifecycleScope.launch {
                if (address.isEmpty()) {
                    return@launch
                }
                val chainIds = if (chainId.isNotEmpty()) {
                    listOf(chainId)
                } else {
                    Web3ChainIds
                }
                val list = web3ViewModel.web3TokenItems(chainIds)
                TokenListBottomSheetDialogFragment.newInstance(ArrayList(list)).apply {
                    setOnClickListener { token ->
                        navTo(InputFragment.newInstance(token, address, null, true), InputFragment.TAG)
                        dismissNow()
                    }
                }.show(parentFragmentManager, TokenListBottomSheetDialogFragment.TAG)
            }
        }
        binding.addressRl.setOnClickListener {
            WalletActivity.showWithAddress(this.requireActivity(), address, WalletActivity.Destination.Address)
        }
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
