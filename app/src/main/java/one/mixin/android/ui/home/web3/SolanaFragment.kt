package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentChainBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.dp
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.toast
import one.mixin.android.tip.wc.WCUnlockEvent
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.tip.wc.WalletUnlockBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.WalletUnlockBottomSheetDialogFragment.Companion.TYPE_ETH
import one.mixin.android.ui.tip.wc.WalletUnlockBottomSheetDialogFragment.Companion.TYPE_SOLANA
import one.mixin.android.widget.SpacesItemDecoration

@AndroidEntryPoint
class SolanaFragment : BaseFragment() {
    companion object {
        const val TAG = "SolanaFragment"
    }

    private var _binding: FragmentChainBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val connectionsViewModel by viewModels<ConnectionsViewModel>()
    private val adapter by lazy {
        WalletAdapter()
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChainBinding.inflate(inflater, container, false)
        binding.apply {
            walletRv.adapter = adapter
            walletRv.addItemDecoration(SpacesItemDecoration(4.dp, true))
        }
        RxBus.listen(WCUnlockEvent::class.java)
            .autoDispose(destroyScope)
            .subscribe { e ->
                updateUI()
            }
        updateUI()
        loadData()
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadData() {
        lifecycleScope.launch {
            val dapps = connectionsViewModel.dapps(Chain.Solana.chainId)
            adapter.connections = dapps
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateUI() {
        lifecycleScope.launch {
            val address = PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
            if (address.isBlank()) {
                adapter.setContent(getString(R.string.web3_account_network, getString(R.string.Solana)), getString(R.string.access_dapps_defi_projects), R.drawable.ic_solana, {
                    WalletUnlockBottomSheetDialogFragment.getInstance(TYPE_SOLANA).showIfNotShowing(parentFragmentManager, WalletUnlockBottomSheetDialogFragment.TAG)
                })
            } else {
                adapter.setContent(getString(R.string.web3_account_network, getString(R.string.Solana)), address.formatPublicKey(), R.drawable.ic_solana, {
                    requireContext().getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, address))
                    toast(R.string.copied_to_clipboard)
                }, R.string.Copy)
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