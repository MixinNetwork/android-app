package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentChainBinding
import one.mixin.android.databinding.ViewWalletTransactionsBottomBinding
import one.mixin.android.databinding.ViewWalletTransactionsSendBottomBinding
import one.mixin.android.databinding.ViewWalletWeb3BottomBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.dp
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.navTo
import one.mixin.android.extension.toast
import one.mixin.android.tip.wc.WCUnlockEvent
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.tip.wc.WalletUnlockBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.WalletUnlockBottomSheetDialogFragment.Companion.TYPE_ETH
import one.mixin.android.web3.Wbe3DepositFragment
import one.mixin.android.web3.dapp.SearchDappFragment
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SpacesItemDecoration

@AndroidEntryPoint
class EthereumFragment : BaseFragment() {
    companion object {
        const val TAG = "EthereumFragment"
    }

    private var _binding: FragmentChainBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var _bottomBinding: ViewWalletWeb3BottomBinding? = null
    private val bottomBinding get() = requireNotNull(_bottomBinding) { "required _bottomBinding is null" }

    private val web3ViewModel by viewModels<Web3ViewModel>()
    private val adapter by lazy {
        Web3WalletAdapter { id ->
            when (id) {
                R.id.send -> {
                    toast(R.string.coming_soon)
                }

                R.id.receive -> {
                    navTo(Wbe3DepositFragment(), Wbe3DepositFragment.TAG)
                }

                R.id.browser -> {
                    navTo(SearchDappFragment(), SearchDappFragment.TAG)
                }

                R.id.more -> {
                    val builder = BottomSheet.Builder(requireActivity())
                    _bottomBinding = ViewWalletWeb3BottomBinding.bind(View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_wallet_web3_bottom, null))
                    builder.setCustomView(bottomBinding.root)
                    val bottomSheet = builder.create()
                    bottomBinding.apply {
                        addressTv.text = this@EthereumFragment.address?.formatPublicKey()
                        copy.setOnClickListener {
                            context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, address))
                            toast(R.string.copied_to_clipboard)
                            bottomSheet.dismiss()
                        }
                        cancel.setOnClickListener { bottomSheet.dismiss() }
                    }

                    bottomSheet.show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChainBinding.inflate(inflater, container, false)
        lifecycleScope.launch {
            val address = PropertyHelper.findValueByKey(EVM_ADDRESS, "")
            this@EthereumFragment.address = address
            binding.apply {
                adapter.address = address
                walletRv.adapter = adapter
                walletRv.addItemDecoration(SpacesItemDecoration(4.dp, true))
            }
        }
        RxBus.listen(WCUnlockEvent::class.java)
            .autoDispose(destroyScope)
            .subscribe { e ->
                updateUI()
            }
        updateUI()
        return binding.root
    }

    private var address: String? = null
    private fun updateUI() {
        lifecycleScope.launch {
            val address = PropertyHelper.findValueByKey(EVM_ADDRESS, "")
            this@EthereumFragment.address = address
            if (address.isBlank()) {
                adapter.setContent(getString(R.string.web3_account_network, getString(R.string.Ethereum)), getString(R.string.access_dapps_defi_projects), R.drawable.ic_ethereum) {
                    WalletUnlockBottomSheetDialogFragment.getInstance(TYPE_ETH).showIfNotShowing(parentFragmentManager, WalletUnlockBottomSheetDialogFragment.TAG)
                }
            } else {
                adapter.address = address
                refreshAccount(address)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun refreshAccount(address: String) {
        if (adapter.isEmpty()) {
            binding.progress.isVisible = true
        }
        val account = try {
            val response = web3ViewModel.web3Account(address)
            if (response.error != null) {
                handleError(address, response.errorDescription)
                binding.progress.isVisible = false
                return
            }
            response
        } catch (e: Exception) {
            handleError(address, e.message ?: getString(R.string.Unknown))
            binding.progress.isVisible = false
            return
        }
        account.data?.let { data ->
            adapter.account = data
        }
        handleEmpty()
        binding.empty.isVisible = false
        binding.progress.isVisible = false
    }

    private fun handleError(address: String, err: String) {
        binding.apply {
            if (adapter.account != null) return
            empty.isVisible = true
            titleTv.text = err
            receiveTv.setText(R.string.Retry)
            receiveTv.setOnClickListener {
                lifecycleScope.launch {
                    refreshAccount(address = address)
                }
            }
        }
    }

    private fun handleEmpty() {
        binding.apply {
            if (!adapter.isEmpty()) return
            empty.isVisible = true
            titleTv.setText(R.string.No_asset)
            receiveTv.setText(R.string.Receive)
            receiveTv.setOnClickListener {
                navTo(Wbe3DepositFragment(), Wbe3DepositFragment.TAG)
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateUI()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }
}
