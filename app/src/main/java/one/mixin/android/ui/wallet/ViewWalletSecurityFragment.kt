package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import one.mixin.android.R
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.components.ViewWalletSecurityContent

class ViewWalletSecurityFragment : BaseFragment(R.layout.fragment_compose) {
    private var chainId: String? = null
    private var walletId: String? = null
    private var mode: WalletSecurityActivity.Mode = WalletSecurityActivity.Mode.VIEW_MNEMONIC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val modeOrdinal = it.getInt(ARG_MODE)
            mode = WalletSecurityActivity.Mode.entries[modeOrdinal]
            chainId = it.getString(ARG_CHAIN_ID)
            walletId = it.getString(ARG_WALLET_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ViewWalletSecurityContent(
                    mode = mode,
                    pop = {
                        requireActivity().finish()
                    },
                    next = {
                        navTo(
                            VerifyPinBeforeImportWalletFragment.newInstance(mode, walletId = walletId, chainId = chainId), "VerifyPinBeforeImportWalletFragment"
                        )
                        parentFragmentManager.beginTransaction()
                            .remove(this@ViewWalletSecurityFragment)
                            .commit()
                    }
                )
            }
        }
    }

    companion object {
        private const val ARG_MODE = "arg_mode"
        private const val ARG_CHAIN_ID = "arg_chain_id"
        private const val ARG_WALLET_ID = "arg_wallet_id"

        fun newInstance(mode: WalletSecurityActivity.Mode, chainId: String? = null, walletId: String?): ViewWalletSecurityFragment {
            val fragment = ViewWalletSecurityFragment()
            val args = Bundle()
            args.putInt(ARG_MODE, mode.ordinal)
            chainId?.let { args.putString(ARG_CHAIN_ID, it) }
            walletId?.let { args.putString(ARG_WALLET_ID, it) }
            fragment.arguments = args
            return fragment
        }
    }

    
}