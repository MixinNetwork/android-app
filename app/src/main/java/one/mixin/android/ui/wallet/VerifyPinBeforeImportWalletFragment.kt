package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.navTo
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.components.VerifyPinBeforeImportWalletPage
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel
import javax.inject.Inject

@AndroidEntryPoint
class VerifyPinBeforeImportWalletFragment : BaseFragment(R.layout.fragment_compose) {

    @Inject
    lateinit var tip: Tip

    private val viewModel by activityViewModels<FetchWalletViewModel>()

    private var mode: WalletSecurityActivity.Mode =
        WalletSecurityActivity.Mode.IMPORT // Default mode
    private var chainId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val modeOrdinal = it.getInt(ARG_MODE, WalletSecurityActivity.Mode.IMPORT.ordinal)
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
                VerifyPinBeforeImportWalletPage(
                    tip = tip,
                    pop = {
                        activity?.finish()
                    },
                    next = { pin ->
                        lifecycleScope.launch {
                            val result = tip.getOrRecoverTipPriv(requireContext(), pin)
                            if (result.isSuccess) {
                                val tipPriv = result.getOrThrow()
                                val spendKey = tip.getSpendPrivFromEncryptedSalt(
                                    tip.getMnemonicFromEncryptedPreferences(requireContext()),
                                    tip.getEncryptedSalt(requireContext()),
                                    pin,
                                    tipPriv
                                )
                                viewModel.setSpendKey(spendKey)
                                when (mode) {
                                    WalletSecurityActivity.Mode.IMPORT -> {
                                        navTo(
                                            AddWalletFragment.newInstance(),
                                            AddWalletFragment.TAG
                                        )
                                        requireActivity().supportFragmentManager
                                            .beginTransaction()
                                            .remove(this@VerifyPinBeforeImportWalletFragment)
                                            .commit()
                                    }

                                    WalletSecurityActivity.Mode.VIEW_MNEMONIC -> {
                                        navTo(
                                            DisplayWalletSecurityFragment.newInstance(
                                                WalletSecurityActivity.Mode.VIEW_MNEMONIC,
                                            ), "DisplayWalletSecurityFragment"
                                        )
                                        requireActivity().supportFragmentManager
                                            .beginTransaction()
                                            .remove(this@VerifyPinBeforeImportWalletFragment)
                                            .commit()
                                    }

                                    WalletSecurityActivity.Mode.VIEW_PRIVATE_KEY -> {
                                        navTo(
                                            DisplayWalletSecurityFragment.newInstance(
                                                WalletSecurityActivity.Mode.VIEW_PRIVATE_KEY, chainId
                                            ), "DisplayWalletSecurityFragment"
                                        )
                                        requireActivity().supportFragmentManager
                                            .beginTransaction()
                                            .remove(this@VerifyPinBeforeImportWalletFragment)
                                            .commit()
                                    }
                                }
                            } else {
                                requireActivity().finish()
                            }
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val TAG = "CheckPinFragment"
        private const val ARG_MODE = "arg_mode"
        private const val ARG_CHAIN_ID = "arg_chain_id"

        fun newInstance(mode: WalletSecurityActivity.Mode = WalletSecurityActivity.Mode.IMPORT, chainId: String? = null): VerifyPinBeforeImportWalletFragment {
            val fragment = VerifyPinBeforeImportWalletFragment()
            val args = Bundle()
            args.putInt(ARG_MODE, mode.ordinal)
            chainId?.let { args.putString(ARG_CHAIN_ID, it) }
            fragment.arguments = args
            return fragment
        }
    }
}