package one.mixin.android.ui.wallet

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.event.WalletOperationType
import one.mixin.android.event.WalletRefreshedEvent
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class ReImportMnemonicFragment : BaseFragment(R.layout.fragment_compose) {


    private val binding by viewBinding(FragmentComposeBinding::bind)

    // state for scanned mnemonic list
    private var scannedMnemonicList by mutableStateOf<List<String>>(emptyList())
    private lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>

    private var evmAddressInfo: Web3Address? = null
    private var solAddressInfo: Web3Address? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getScanResult = registerForActivityResult(
            CaptureActivity.CaptureContract()
        ) { intent ->
            intent?.getStringExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT)
                ?.split(" ")
                ?.takeIf { it.isNotEmpty() }
                ?.let { scannedMnemonicList = it }
        }
    }

    private val viewModel by activityViewModels<FetchWalletViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val walletId = arguments?.getString(ARG_WALLET_ID)
        lifecycleScope.launch {
            walletId?.let {
                evmAddressInfo = viewModel.getAddressesByChainId(it, Constants.ChainId.ETHEREUM_CHAIN_ID)
                solAddressInfo = viewModel.getAddressesByChainId(it, Constants.ChainId.SOLANA_CHAIN_ID)
            }
        }

        binding.titleView.leftIb.setOnClickListener {
            requireActivity().finish()
        }
        binding.titleView.rightIb.setImageResource(R.drawable.ic_support)
        binding.titleView.rightAnimator.visibility = View.VISIBLE
        binding.titleView.rightAnimator.displayedChild = 0
        binding.titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE) }
        binding.compose.setContent {
            MnemonicPhraseInput(
                state = MnemonicState.Import,
                mnemonicList = scannedMnemonicList,
                onComplete = { words ->
                    viewModel.saveWeb3PrivateKey(requireContext(), viewModel.getSpendKey()!!, walletId!!, words)
                    toast(R.string.Success)
                    RxBus.publish(WalletRefreshedEvent(walletId, WalletOperationType.CREATE))
                    activity?.finish()
                },
                onScan = { getScanResult.launch(Pair(CaptureActivity.ARGS_FOR_SCAN_RESULT, true)) },
                validate = ::validateMnemonic
            )
        }
    }

    private fun validateMnemonic(mnemonic: List<String>): String? {
        val mnemonicPhrase = mnemonic.joinToString(" ")
        evmAddressInfo?.let {
            val index = CryptoWalletHelper.extractIndexFromPath(it.path!!)
            val derivedAddress = CryptoWalletHelper.mnemonicToAddress(mnemonicPhrase, Constants.ChainId.ETHEREUM_CHAIN_ID, "", index!!)
            if (!derivedAddress.equals(it.destination, ignoreCase = true)) {
                return getString(R.string.reimport_mnemonic_phrase_error)
            }
        }

        solAddressInfo?.let {
            val index = CryptoWalletHelper.extractIndexFromPath(it.path!!)
            val derivedAddress = CryptoWalletHelper.mnemonicToAddress(mnemonicPhrase, Constants.ChainId.SOLANA_CHAIN_ID, "", index!!)
            if (!derivedAddress.equals(it.destination, ignoreCase = true)) {
                return getString(R.string.reimport_mnemonic_phrase_error)
            }
        }
        return null
    }

    companion object {
        const val TAG = "ReImportMnemonicFragment"
        private const val ARG_WALLET_ID = "arg_wallet_id"

        fun newInstance(walletId: String?): ReImportMnemonicFragment {
            val fragment = ReImportMnemonicFragment()
            val args = Bundle()
            walletId?.let { args.putString(ARG_WALLET_ID, it) }
            fragment.arguments = args
            return fragment
        }
    }
}
