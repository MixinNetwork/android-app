package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import one.mixin.android.R
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.QrBottomSheetDialogFragment
import one.mixin.android.ui.wallet.components.DisplayWalletSecurityContent
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel

class DisplayWalletSecurityFragment : BaseFragment(R.layout.fragment_compose) {

    private val viewModel by activityViewModels<FetchWalletViewModel>()
    private var mode: WalletSecurityActivity.Mode = WalletSecurityActivity.Mode.VIEW_MNEMONIC
    private var chainId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val modeOrdinal = it.getInt(ARG_MODE)
            mode = WalletSecurityActivity.Mode.entries.toTypedArray()[modeOrdinal]
            chainId = it.getString(ARG_CHAIN_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                var securityContent by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(mode, chainId) {
                    securityContent = when (mode) {
                        WalletSecurityActivity.Mode.VIEW_MNEMONIC -> {
                            viewModel.getWeb3Mnemonic(requireContext()) ?: throw IllegalArgumentException("Mnemonic not found")
                        }
                        WalletSecurityActivity.Mode.VIEW_PRIVATE_KEY -> {
                            viewModel.getWeb3Priva(requireContext(), chainId) ?: throw IllegalArgumentException("Private key not found")
                        }
                        else -> throw IllegalArgumentException("Unsupported mode: $mode")
                    }
                }

                DisplayWalletSecurityContent(
                    mode = mode,
                    securityContent = securityContent,
                    pop = {
                        activity?.finish()
                    }, onQrCode = { mnemonic ->
                        QrBottomSheetDialogFragment.newInstance(
                            mnemonic.joinToString(" "),
                            QrBottomSheetDialogFragment.TYPE_MNEMONIC_QR,
                        ).show(childFragmentManager, QrBottomSheetDialogFragment.TAG)
                    }
                )
            }
        }
    }

    companion object {
        private const val ARG_MODE = "arg_mode"
        private const val ARG_CHAIN_ID = "arg_chain_id"

        fun newInstance(mode: WalletSecurityActivity.Mode, chainId: String? = null): DisplayWalletSecurityFragment {
            return DisplayWalletSecurityFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_MODE, mode.ordinal)
                    chainId?.let { putString(ARG_CHAIN_ID, it) }
                }
            }
        }
    }
}
