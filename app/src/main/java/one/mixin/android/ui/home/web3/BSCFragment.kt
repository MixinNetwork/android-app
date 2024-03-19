package one.mixin.android.ui.home.web3

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentBscBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.dp
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.WalletUnlockBottomSheetDialogFragment
import one.mixin.android.widget.SpacesItemDecoration

class BSCFragment : BaseFragment() {
    companion object {
        const val TAG = "BSCFragment"
    }

    private var _binding: FragmentBscBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val connectionsViewModel by viewModels<ConnectionsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBscBinding.inflate(inflater, container, false)
        binding.apply {
            walletRv.adapter = WalletAdapter().apply {
                connections = connectionsViewModel.getLatestActiveSignSessions()
            }

            walletRv.addItemDecoration(SpacesItemDecoration(4.dp, true))
        }
        updateUI()
        return binding.root
    }

    private fun updateUI() {
        lifecycleScope.launch {
            val address = PropertyHelper.findValueByKey(Constants.Account.PREF_WALLET_CONNECT_ADDRESS, "")
            if (address.isBlank()) {
                binding.chainCard.setContent(getString(R.string.web3_account_network, getString(R.string.BSC)), getString(R.string.access_dapps_defi_projects), R.drawable.ic_bsc)
                binding.chainCard.setOnCreateListener {
                    WalletUnlockBottomSheetDialogFragment.newInstance(WalletUnlockBottomSheetDialogFragment.TYPE_BSC).showNow(parentFragmentManager, WalletConnectBottomSheetDialogFragment.TAG)
                }
            } else {
                binding.chainCard.setContent(getString(R.string.web3_account_network, getString(R.string.BSC)), address.formatPublicKey(), R.string.Copy, R.drawable.ic_bsc)
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