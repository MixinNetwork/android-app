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
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentChainBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.dp
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.toast
import one.mixin.android.tip.wc.WCUnlockEvent
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.tip.wc.WalletUnlockBottomSheetDialogFragment
import one.mixin.android.widget.SpacesItemDecoration


@AndroidEntryPoint
class PolygonFragment : BaseFragment() {
    companion object {
        const val TAG = "PolygonFragment"
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
            if (adapter.itemCount <= 0) {
                binding.va.displayedChild = 0
            }
            val dapps = connectionsViewModel.dapps().filter {
                it.chains.contains(Constants.ChainId.Polygon)
            }
            adapter.connections = dapps
            adapter.notifyDataSetChanged()
            binding.va.displayedChild = 1
        }
    }

    private fun updateUI() {
        lifecycleScope.launch {
            val address = PropertyHelper.findValueByKey(Constants.Account.PREF_WALLET_CONNECT_ADDRESS, "")
            if (address.isBlank()) {
                binding.chainCard.setContent(getString(R.string.web3_account_network, getString(R.string.Polygon)), getString(R.string.access_dapps_defi_projects), R.drawable.ic_polygon)
                binding.chainCard.setOnCreateListener {
                    WalletUnlockBottomSheetDialogFragment.getInstance(WalletUnlockBottomSheetDialogFragment.TYPE_POLYGON).showIfNotShowing(parentFragmentManager, WalletUnlockBottomSheetDialogFragment.TAG)
                }
            } else {
                binding.chainCard.setContent(getString(R.string.web3_account_network, getString(R.string.Polygon)), address.formatPublicKey(), R.string.Copy, R.drawable.ic_polygon)
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
