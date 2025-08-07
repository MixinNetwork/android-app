package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import one.mixin.android.R
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.components.ViewWalletSecurityContent

class ViewWalletSecurityFragment : BaseFragment(R.layout.fragment_compose) {

    private var mode: WalletSecurityActivity.Mode = WalletSecurityActivity.Mode.VIEW_MNEMONIC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val modeOrdinal = it.getInt(ARG_MODE)
            mode = WalletSecurityActivity.Mode.values()[modeOrdinal]
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val chainId = arguments?.getString(ARG_CHAIN_ID)
        val walletId = arguments?.getString(ARG_WALLET_ID)
        return ComposeView(requireContext()).apply {
            setContent {
                ViewWalletSecurityContent(
                    mode = mode,
                    pop = {
                        requireActivity()?.finish()
                    },
                    next = {
                        val verifyPinMode = when (mode) {
                            WalletSecurityActivity.Mode.VIEW_MNEMONIC -> WalletSecurityActivity.Mode.VIEW_MNEMONIC
                            WalletSecurityActivity.Mode.VIEW_PRIVATE_KEY -> WalletSecurityActivity.Mode.VIEW_PRIVATE_KEY
                            else -> throw IllegalArgumentException("Unsupported mode: $mode")
                        }
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.container, DisplayWalletSecurityFragment.newInstance(verifyPinMode, chainId, walletId))
                            .addToBackStack(null)
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