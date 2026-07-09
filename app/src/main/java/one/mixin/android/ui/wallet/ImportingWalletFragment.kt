package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.activityViewModels
import one.mixin.android.R
import one.mixin.android.crypto.clearPendingImportMnemonic
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.openCustomerService
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.setting.member.MixinMemberUpgradeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.components.FetchWalletState
import one.mixin.android.ui.wallet.components.ImportErrorContent
import one.mixin.android.ui.wallet.components.ImportingContent
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel
import one.mixin.android.util.viewBinding
import timber.log.Timber

class ImportingWalletFragment : BaseFragment(R.layout.fragment_compose) {
    private val binding by viewBinding(FragmentComposeBinding::bind)
    private val viewModel by activityViewModels<FetchWalletViewModel>()
    private val customerServiceSource: String? by lazy { arguments?.getString(ARG_CUSTOMER_SERVICE_SOURCE) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { requireActivity().finish() }
        binding.titleView.leftIb.setImageResource(R.drawable.ic_close_black)
        binding.titleView.rightIb.setImageResource(R.drawable.ic_support)
        binding.titleView.rightAnimator.visibility = View.VISIBLE
        binding.titleView.rightAnimator.displayedChild = 0
        binding.titleView.rightAnimator.setOnClickListener { openCustomerService(source = customerServiceSource) }
        binding.compose.setContent {
            val state by viewModel.state.collectAsState()
            val errorCode by viewModel.errorCode.collectAsState()
            val errorMessage by viewModel.errorMessage.collectAsState()
            val partialSuccess by viewModel.partialSuccess.collectAsState()
            val importedWalletDestination by viewModel.importedWalletDestination.collectAsState()

            when (state) {
                FetchWalletState.IMPORTING -> {
                    ImportingContent {
                        requireActivity().finish()
                    }
                }
                FetchWalletState.IMPORT_ERROR -> {
                    Timber.i("LoginFlow wallet_import_result success=false partial_success=$partialSuccess code=$errorCode")
                    binding.titleView.leftIb.setImageResource(R.drawable.ic_close_black)
                    ImportErrorContent(
                        partialSuccess =partialSuccess,
                        errorCode = errorCode,
                        errorMessage =errorMessage,
                        onUpgradePlan = {
                            val fragment = MixinMemberUpgradeBottomSheetDialogFragment.newInstance()
                            fragment.show(parentFragmentManager, MixinMemberUpgradeBottomSheetDialogFragment.TAG)
                        },
                        onNotNow = {
                            requireActivity().finish()
                        }
                    )
                }
                FetchWalletState.IMPORT_SUCCESS ->{
                    LaunchedEffect(importedWalletDestination) {
                        Timber.i("LoginFlow wallet_import_result success=true has_destination=${importedWalletDestination != null}")
                        clearPendingImportMnemonic(requireContext())
                        Timber.i("LoginFlow pending_import_cleared source=wallet_import")
                        MainActivity.showWallet(
                            requireContext(),
                            walletDestination = importedWalletDestination
                        )
                        requireActivity().finish()
                    }
                }
                else -> {
                    ImportingContent {
                        requireActivity().finish()
                    }
                }
            }
        }

        val fromDetail = arguments?.getBoolean(ARG_FROM_DETAIL, false) ?: false
        val modeOrdinal = arguments?.getInt(ARG_MODE) ?: WalletSecurityActivity.Mode.IMPORT_MNEMONIC.ordinal
        val mode = WalletSecurityActivity.Mode.values()[modeOrdinal]
        if (mode == WalletSecurityActivity.Mode.CREATE_WALLET){
            // do nothing, this is a create wallet mode
        } else if (fromDetail) {
            val key = arguments?.getString(ARG_KEY) ?: return
            val chainId = arguments?.getString(ARG_CHAIN_ID) ?: return
            val walletName = arguments?.getString(ARG_WALLET_NAME)
            val modeOrdinal =
                arguments?.getInt(ARG_MODE, WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY.ordinal)
                    ?: WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY.ordinal
            val mode = WalletSecurityActivity.Mode.entries[modeOrdinal]
            viewModel.importWallet(key, chainId, mode, walletName)
        } else {
            viewModel.startImporting()
        }
    }

    companion object {
        const val TAG = "importing"
        private const val ARG_KEY = "arg_key"
        private const val ARG_CHAIN_ID = "arg_chain_id"
        private const val ARG_WALLET_NAME = "arg_wallet_name"
        private const val ARG_MODE = "arg_mode"
        private const val ARG_FROM_DETAIL = "arg_from_detail"
        private const val ARG_CUSTOMER_SERVICE_SOURCE = "arg_customer_service_source"

        fun newInstance(customerServiceSource: String? = null): ImportingWalletFragment {
            return ImportingWalletFragment().apply {
                arguments = Bundle().apply {
                    customerServiceSource?.let { putString(ARG_CUSTOMER_SERVICE_SOURCE, it) }
                }
            }
        }

        fun newInstance(mode: WalletSecurityActivity.Mode): ImportingWalletFragment {
            val fragment = ImportingWalletFragment()
            val args = Bundle()
            args.putInt(ARG_MODE, mode.ordinal)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(
            key: String,
            chainId: String,
            mode: WalletSecurityActivity.Mode,
            walletName: String? = null
        ): ImportingWalletFragment {
            val fragment = ImportingWalletFragment()
            val args = Bundle()
            args.putString(ARG_KEY, key)
            args.putString(ARG_CHAIN_ID, chainId)
            walletName?.let { args.putString(ARG_WALLET_NAME, it) }
            args.putInt(ARG_MODE, mode.ordinal)
            args.putBoolean(ARG_FROM_DETAIL, true)
            fragment.arguments = args
            return fragment
        }
    }
}
