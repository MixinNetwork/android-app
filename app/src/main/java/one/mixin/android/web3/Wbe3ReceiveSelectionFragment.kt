
package one.mixin.android.web3

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWeb3ReceuceSelectionBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.navTo
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel

@AndroidEntryPoint
class Wbe3ReceiveSelectionFragment : BaseFragment() {
    companion object {
        const val TAG = "Wbe3ReceiveSelectionFragment"
    }

    private var _binding: FragmentWeb3ReceuceSelectionBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val web3ViewModel by viewModels<Web3ViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWeb3ReceuceSelectionBinding.inflate(inflater, container, false).apply { this.root.setOnClickListener { } }
        binding.root.setOnClickListener {  }
        binding.title.setOnClickListener {  }
        binding.title.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        binding.walletTv.text = getString(R.string.contact_mixin_id, Session.getAccount()?.identityNumber)
        binding.walletRl.setOnClickListener {
            lifecycleScope.launch {
                val address = PropertyHelper.findValueByKey(Constants.Account.ChainAddress.EVM_ADDRESS, "")
                if (address.isEmpty()) {
                    return@launch
                }
                val list = web3ViewModel.web3TokenItems()
                TokenListBottomSheetDialogFragment.newInstance(ArrayList(list)).apply {
                    setOnClickListener { token ->
                        navTo(InputFragment.newInstance(token, address), InputFragment.TAG)
                        dismissNow()
                    }
                }.show(parentFragmentManager, TokenListBottomSheetDialogFragment.TAG)
            }
        }
        binding.addressRl.setOnClickListener {
            navTo(Wbe3ReceiveFragment(), Wbe3ReceiveFragment.TAG)
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