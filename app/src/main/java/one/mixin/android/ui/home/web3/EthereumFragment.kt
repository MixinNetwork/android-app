package one.mixin.android.ui.home.web3

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentEthereumBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.dp
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.WalletCreateBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.WalletCreateBottomSheetDialogFragment.Companion.TYPE_ETH
import one.mixin.android.widget.SpacesItemDecoration

class EthereumFragment : BaseFragment() {
    companion object {
        const val TAG = "EthereumFragment"
    }

    private var _binding: FragmentEthereumBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEthereumBinding.inflate(inflater, container, false)
        binding.apply {
            walletRv.adapter = WalletAdapter()
            walletRv.addItemDecoration(SpacesItemDecoration(4.dp, true))
        }
        updateUI()
        return binding.root
    }

    private fun updateUI() {
        lifecycleScope.launch {
            val address = PropertyHelper.findValueByKey(Constants.Account.PREF_WALLET_CONNECT_ADDRESS, "")
            if (address.isBlank()) {
                binding.chainCard.setContent("Ethereum Account", "Access dapps and DeFi projects.", R.drawable.ic_ethereum)
                binding.chainCard.setOnCreateListener {
                    WalletCreateBottomSheetDialogFragment.newInstance(TYPE_ETH).showNow(parentFragmentManager, WalletConnectBottomSheetDialogFragment.TAG)
                }
            } else {
                binding.chainCard.setContent("Ethereum Account", address.formatPublicKey(), R.string.Copy, R.drawable.ic_ethereum)
                binding.chainCard.setOnCreateListener {
                    requireContext().getClipboardManager()
                        .setPrimaryClip(ClipData.newPlainText(null, address))
                    toast(R.string.copied_to_clipboard)
                }
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