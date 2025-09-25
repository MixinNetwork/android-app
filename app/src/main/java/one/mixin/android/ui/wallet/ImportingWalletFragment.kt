package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.activityViewModels
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.member.MixinMemberUpgradeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.components.FetchWalletState
import one.mixin.android.ui.wallet.components.ImportErrorContent
import one.mixin.android.ui.wallet.components.ImportingContent
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel
import one.mixin.android.util.viewBinding

class ImportingWalletFragment : BaseFragment(R.layout.fragment_compose) {
    private val binding by viewBinding(FragmentComposeBinding::bind)
    private val viewModel by activityViewModels<FetchWalletViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { requireActivity().finish() }
        binding.titleView.leftIb.setImageResource(R.drawable.ic_close_black)
        binding.titleView.rightIb.setImageResource(R.drawable.ic_support)
        binding.titleView.rightAnimator.visibility = View.VISIBLE
        binding.titleView.rightAnimator.displayedChild = 0
        binding.titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE) }
        binding.compose.setContent {
            val state by viewModel.state.collectAsState()
            val errorCode by viewModel.errorCode.collectAsState()
            val errorMessage by viewModel.errorMessage.collectAsState()
            val partialSuccess by viewModel.partialSuccess.collectAsState()

            when (state) {
                FetchWalletState.IMPORTING -> {
                    ImportingContent {
                        requireActivity().finish()
                    }
                }
                FetchWalletState.IMPORT_ERROR -> {
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
                    requireActivity().finish()
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
            val modeOrdinal =
                arguments?.getInt(ARG_MODE, WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY.ordinal)
                    ?: WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY.ordinal
            val mode = WalletSecurityActivity.Mode.entries[modeOrdinal]
            viewModel.importWallet(key, chainId, mode)
        } else {
            viewModel.startImporting()
        }
    }

    companion object {
        const val TAG = "importing"
        private const val ARG_KEY = "arg_key"
        private const val ARG_CHAIN_ID = "arg_chain_id"
        private const val ARG_MODE = "arg_mode"
        private const val ARG_FROM_DETAIL = "arg_from_detail"

        fun newInstance(): ImportingWalletFragment {
            return ImportingWalletFragment()
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
            mode: WalletSecurityActivity.Mode
        ): ImportingWalletFragment {
            val fragment = ImportingWalletFragment()
            val args = Bundle()
            args.putString(ARG_KEY, key)
            args.putString(ARG_CHAIN_ID, chainId)
            args.putInt(ARG_MODE, mode.ordinal)
            args.putBoolean(ARG_FROM_DETAIL, true)
            fragment.arguments = args
            return fragment
        }
    }
}