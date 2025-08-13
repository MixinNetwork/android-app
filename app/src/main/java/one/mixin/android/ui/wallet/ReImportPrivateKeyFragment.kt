package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.WalletOperationType
import one.mixin.android.event.WalletRefreshedEvent
import one.mixin.android.extension.toast
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.components.ImportWalletDetailPage
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ReImportPrivateKeyFragment : BaseFragment(R.layout.fragment_compose) {

    @Inject
    lateinit var tip: Tip

    private val viewModel by activityViewModels<FetchWalletViewModel>()

    private var walletId: String? = null
    private var chainId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            walletId = it.getString(ARG_WALLET_ID)
            chainId = it.getString(ARG_CHAIN_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ImportWalletDetailPage(
                    mode = WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY,
                    chainId = chainId,
                    walletId = walletId,
                    pop = {
                        activity?.finish()
                    },
                    onConfirmClick = { _, text ->
                        viewModel.savePrivateKey(requireNotNull(walletId), text)
                        RxBus.publish(WalletRefreshedEvent(requireNotNull(walletId), WalletOperationType.CREATE))
                        toast(R.string.Success)
                        activity?.finish()
                    }
                )
            }
        }
    }

    companion object {
        const val TAG = "ReImportPrivateKeyFragment"
        private const val ARG_WALLET_ID = "arg_wallet_id"
        private const val ARG_CHAIN_ID = "arg_chain_id"

        fun newInstance(walletId: String?, chainId: String?): ReImportPrivateKeyFragment {
            val fragment = ReImportPrivateKeyFragment()
            val args = Bundle()
            walletId?.let { args.putString(ARG_WALLET_ID, it) }
            chainId?.let { args.putString(ARG_CHAIN_ID, it) }
            fragment.arguments = args
            return fragment
        }
    }
}
