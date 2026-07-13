package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.crypto.getPendingImportMnemonic
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.components.VerifyPinBeforeImportWalletPage
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel
import one.mixin.android.util.analytics.AnalyticsTracker
import javax.inject.Inject

@AndroidEntryPoint
class VerifyPinBeforeImportWalletFragment : BaseFragment(R.layout.fragment_compose) {

    @Inject
    lateinit var tip: Tip

    private val viewModel by activityViewModels<FetchWalletViewModel>()

    private var mode: WalletSecurityActivity.Mode =
        WalletSecurityActivity.Mode.IMPORT_MNEMONIC // Default mode
    private var chainId: String? = null
    private var walletId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val modeOrdinal = it.getInt(ARG_MODE, WalletSecurityActivity.Mode.IMPORT_MNEMONIC.ordinal)
            mode = WalletSecurityActivity.Mode.entries.toTypedArray()[modeOrdinal]
            chainId = it.getString(ARG_CHAIN_ID)
            walletId = it.getString(ARG_WALLET_ID)
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
                    mode = mode,
                    pop = if (shouldHideWalletSecurityClose(mode)) null else ({ activity?.finish() }),
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
                                    WalletSecurityActivity.Mode.IMPORT_MNEMONIC -> {
                                        replaceAsRoot(
                                            AddWalletFragment.newInstance(),
                                            AddWalletFragment.TAG
                                        )
                                    }

                                    WalletSecurityActivity.Mode.VIEW_MNEMONIC -> {
                                        replaceAsRoot(
                                            DisplayWalletSecurityFragment.newInstance(mode, walletId = walletId),
                                            DisplayWalletSecurityFragment.TAG
                                        )
                                    }

                                    WalletSecurityActivity.Mode.VIEW_PRIVATE_KEY -> {
                                        replaceAsRoot(
                                            DisplayWalletSecurityFragment.newInstance(mode, chainId = chainId, walletId = walletId),
                                            DisplayWalletSecurityFragment.TAG
                                        )
                                    }

                                    WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY, WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS -> {
                                        replaceAsRoot(
                                            ImportWalletDetailFragment.newInstance(mode),
                                            ImportWalletDetailFragment.TAG
                                        )
                                    }
                                    WalletSecurityActivity.Mode.RE_IMPORT_MNEMONIC -> {
                                        replaceAsRoot(
                                            ReImportMnemonicFragment.newInstance(walletId),
                                            ReImportMnemonicFragment.TAG
                                        )
                                    }
                                    WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY -> {
                                        replaceAsRoot(
                                            ReImportPrivateKeyFragment.newInstance(walletId, chainId),
                                            ReImportPrivateKeyFragment.TAG
                                        )
                                    }
                                    WalletSecurityActivity.Mode.CREATE_WALLET -> {
                                        replaceAsRoot(
                                            ImportingWalletFragment.newInstance(WalletSecurityActivity.Mode.CREATE_WALLET),
                                            ImportingWalletFragment.TAG
                                        )
                                        viewModel.createClassicWallet()
                                    }
                                    WalletSecurityActivity.Mode.VIEW_ADDRESS -> {
                                       requireActivity().finish()
                                    }
                                    WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC -> {
                                        val mnemonic = getPendingImportMnemonic(requireContext())
                                        if (mnemonic.isNullOrBlank()) {
                                            requireActivity().finish()
                                        } else {
                                            replaceAsRoot(
                                                FetchingWalletFragment.newInstance(
                                                    mnemonic = mnemonic,
                                                    importCategory = importWalletCategoryForMode(mode),
                                                    fetchCustomerServiceSource = AnalyticsTracker.CustomerServiceSource.LOGIN_WALLET_FETCHING,
                                                    importCustomerServiceSource = AnalyticsTracker.CustomerServiceSource.LOGIN_WALLET_IMPORT,
                                                    hideCloseButton = true,
                                                ),
                                                FetchingWalletFragment.TAG
                                            )
                                        }
                                    }
                                    WalletSecurityActivity.Mode.REGISTER_IMPORT_MNEMONIC -> {
                                        requireActivity().finish()
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

    private fun replaceAsRoot(fragment: Fragment, tag: String) {
        val hostActivity = activity ?: return
        if (hostActivity.isFinishing || hostActivity.isDestroyed) return

        val fragmentManager = hostActivity.supportFragmentManager
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        fragmentManager.beginTransaction()
            .replace(R.id.container, fragment, tag)
            .commit()
    }

    companion object {
        const val TAG = "CheckPinFragment"
        private const val ARG_MODE = "arg_mode"
        private const val ARG_CHAIN_ID = "arg_chain_id"
        private const val ARG_WALLET_ID = "arg_wallet_id"

        fun newInstance(mode: WalletSecurityActivity.Mode = WalletSecurityActivity.Mode.IMPORT_MNEMONIC, chainId: String? = null, walletId: String? = null): VerifyPinBeforeImportWalletFragment {
            val fragment = VerifyPinBeforeImportWalletFragment()
            val args = Bundle()
            args.putInt(ARG_MODE, mode.ordinal)
            chainId?.let { args.putString(ARG_CHAIN_ID, it) }
            walletId?.let { args.putString(ARG_WALLET_ID, it) }
            fragment.arguments = args
            return fragment
        }
    }
}
