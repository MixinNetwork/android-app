package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.components.WalletNoticePage
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel

@AndroidEntryPoint
class WalletNoticeFragment : BaseFragment(R.layout.fragment_compose) {

    private val viewModel by activityViewModels<FetchWalletViewModel>()
    private var mode: WalletSecurityActivity.Mode = WalletSecurityActivity.Mode.IMPORT_MNEMONIC
    private var walletId: String? = null
    private var chainId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val modeOrdinal = it.getInt(ARG_MODE, WalletSecurityActivity.Mode.IMPORT_MNEMONIC.ordinal)
            mode = WalletSecurityActivity.Mode.entries[modeOrdinal]
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
                WalletNoticePage(
                    mode = mode,
                    pop = {
                        requireActivity().finish()
                    },
                    next = {
                        when (mode) {
                            WalletSecurityActivity.Mode.CREATE_WALLET -> {
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.container, VerifyPinBeforeImportWalletFragment.newInstance(mode))
                                    .addToBackStack(null)
                                    .commit()

                            }

                            else -> {
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.container, VerifyPinBeforeImportWalletFragment.newInstance(mode, walletId, chainId))
                                    .addToBackStack(null)
                                    .commit()
                            }
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val TAG = "WalletNoticeFragment"
        private const val ARG_MODE = "arg_mode"
        private const val ARG_WALLET_ID = "arg_wallet_id"
        private const val ARG_CHAIN_ID = "arg_chain_id"

        fun newInstance(mode: WalletSecurityActivity.Mode, chainId: String? = null, walletId: String? = null): WalletNoticeFragment {
            val fragment = WalletNoticeFragment()
            val args = Bundle()
            args.putInt(ARG_MODE, mode.ordinal)
            walletId?.let { args.putString(ARG_WALLET_ID, it) }
            chainId?.let { args.putString(ARG_CHAIN_ID, it) }
            fragment.arguments = args
            return fragment
        }
    }
}
