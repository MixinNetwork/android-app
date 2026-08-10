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
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.event.WalletOperationType
import one.mixin.android.event.WalletRefreshedEvent
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshSingleWalletJob
import one.mixin.android.repository.Web3Repository
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.WalletCategory
import org.web3j.utils.Numeric
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ReImportMnemonicFragment : BaseFragment(R.layout.fragment_compose) {


    private val binding by viewBinding(FragmentComposeBinding::bind)

    // state for scanned mnemonic list
    private var scannedMnemonicList by mutableStateOf<List<String>>(emptyList())
    private lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>

    private var evmAddressInfo: Web3Address? = null
    private var solAddressInfo: Web3Address? = null
    private var btcAddressInfo: Web3Address? = null
    private var pearlAddressInfo: Web3Address? = null

    @Inject
    lateinit var web3Repository: Web3Repository

    @Inject
    lateinit var jobManager: MixinJobManager

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
                // Solana addresses are derived differently (Ed25519 / Base58), so we validate against a dedicated Solana address entry.
                solAddressInfo = viewModel.getAddressesByChainId(it, Constants.ChainId.SOLANA_CHAIN_ID)
                btcAddressInfo = viewModel.getAddressesByChainId(it, Constants.ChainId.BITCOIN_CHAIN_ID)
                pearlAddressInfo = viewModel.getAddressesByChainId(it, Constants.ChainId.PEARL_CHAIN_ID)
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
                    lifecycleScope.launch {
                        val saved = viewModel.saveWeb3PrivateKey(requireContext(), viewModel.getSpendKey()!!, walletId!!, words)
                        if (!saved || !ensureUtxoAddresses(walletId, words)) {
                            toast(R.string.Save_failure)
                            return@launch
                        }
                        validateMnemonicForOtherWallets(words)
                        toast(R.string.Success)
                        RxBus.publish(WalletRefreshedEvent(walletId, WalletOperationType.CREATE))
                        activity?.finish()
                    }
                },
                onScan = { getScanResult.launch(Pair(CaptureActivity.ARGS_FOR_SCAN_RESULT, true)) },
                validate = ::validateMnemonic
            )
        }
    }

    private fun validateMnemonic(mnemonic: List<String>): String? {
        val mnemonicPhrase = mnemonic.joinToString(" ")
        val index: Int? = resolveDerivationIndex()
        evmAddressInfo?.let {
            val derivedAddress = CryptoWalletHelper.mnemonicToAddress(mnemonicPhrase, Constants.ChainId.ETHEREUM_CHAIN_ID, "", requireNotNull(index))
            if (!derivedAddress.equals(it.destination, ignoreCase = true)) {
                return getString(R.string.reimport_mnemonic_phrase_error)
            }
        }

        solAddressInfo?.let {
            val derivedAddress = CryptoWalletHelper.mnemonicToAddress(mnemonicPhrase, Constants.ChainId.SOLANA_CHAIN_ID, "", requireNotNull(index))
            if (!derivedAddress.equals(it.destination, ignoreCase = true)) {
                return getString(R.string.reimport_mnemonic_phrase_error)
            }
        }

        btcAddressInfo?.let {
            val derivedAddress = CryptoWalletHelper.mnemonicToAddress(mnemonicPhrase, Constants.ChainId.BITCOIN_CHAIN_ID, "", requireNotNull(index))
            if (!derivedAddress.equals(it.destination, ignoreCase = true)) {
                return getString(R.string.reimport_mnemonic_phrase_error)
            }
        }
        pearlAddressInfo?.let {
            val derivedAddress = CryptoWalletHelper.mnemonicToAddress(mnemonicPhrase, Constants.ChainId.PEARL_CHAIN_ID, "", requireNotNull(index))
            if (!derivedAddress.equals(it.destination, ignoreCase = true)) {
                return getString(R.string.reimport_mnemonic_phrase_error)
            }
        }
        return null
    }

    private fun resolveDerivationIndex(): Int? {
        return CryptoWalletHelper.extractIndexFromPaths(
            listOf(evmAddressInfo?.path, solAddressInfo?.path, btcAddressInfo?.path, pearlAddressInfo?.path)
        )
    }

    private suspend fun ensureUtxoAddresses(walletId: String, mnemonic: List<String>): Boolean {
        val btcAddress: Web3Address? = viewModel.getAddressesByChainId(walletId, Constants.ChainId.BITCOIN_CHAIN_ID)
        val pearlAddress: Web3Address? = viewModel.getAddressesByChainId(walletId, Constants.ChainId.PEARL_CHAIN_ID)
        if (btcAddress != null && pearlAddress != null) {
            return true
        }
        val evmAddress: Web3Address? = viewModel.getAddressesByChainId(walletId, Constants.ChainId.ETHEREUM_CHAIN_ID)
        val solAddress: Web3Address? = viewModel.getAddressesByChainId(walletId, Constants.ChainId.SOLANA_CHAIN_ID)
        val derivationIndex = requireNotNull(
            CryptoWalletHelper.extractIndexFromPaths(
                listOf(evmAddress?.path, solAddress?.path, btcAddress?.path, pearlAddress?.path)
            )
        )
        val mnemonicPhrase: String = mnemonic.joinToString(" ")
        val addresses = mutableListOf<Web3AddressRequest>()
        if (btcAddress == null) {
            val derivedWallet = CryptoWalletHelper.mnemonicToBitcoinSegwitWallet(mnemonicPhrase, index = derivationIndex)
            addresses += createSignedWeb3AddressRequest(
                destination = derivedWallet.address,
                chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
                path = Bip44Path.bitcoinSegwitPathString(derivationIndex),
                privateKey = Numeric.hexStringToByteArray(derivedWallet.privateKey),
                category = WalletCategory.IMPORTED_MNEMONIC.value,
            )
        }
        if (pearlAddress == null) {
            val derivedWallet = CryptoWalletHelper.mnemonicToPearlWallet(mnemonicPhrase, index = derivationIndex)
            addresses += createSignedWeb3AddressRequest(
                destination = derivedWallet.address,
                chainId = Constants.ChainId.PEARL_CHAIN_ID,
                path = Bip44Path.pearlPathString(derivationIndex),
                privateKey = Numeric.hexStringToByteArray(derivedWallet.privateKey),
                category = WalletCategory.IMPORTED_MNEMONIC.value,
            )
        }
        val updateRequest = WalletRequest(
            name = null,
            category = null,
            addresses = addresses,
        )
        val response = web3Repository.updateWallet(walletId, updateRequest)
        if (!response.isSuccess) {
            Timber.e("Failed to update UTXO addresses walletId=$walletId errorCode=${response.errorCode} errorDescription=${response.errorDescription}")
            return false
        }
        val updatedWallet = response.data ?: return false
        updatedWallet.addresses?.let { addresses ->
            web3Repository.insertAddressList(addresses)
        }
        jobManager.addJobInBackground(RefreshSingleWalletJob(updatedWallet.id))
        return true
    }

    private suspend fun validateMnemonicForOtherWallets(mnemonic: List<String>) {
        val mnemonicPhrase: String = mnemonic.joinToString(" ")
        val currentSpendKey: ByteArray? = viewModel.getSpendKey()
        val context: Context = requireContext()
        val wallets = viewModel.getAllNoKeyWallets()
        wallets.forEach { wallet ->
            if (wallet.category == WalletCategory.IMPORTED_MNEMONIC.value) {
                val evmAddress: Web3Address? = viewModel.getAddressesByChainId(wallet.id, Constants.ChainId.ETHEREUM_CHAIN_ID)
                val solAddress: Web3Address? = viewModel.getAddressesByChainId(wallet.id, Constants.ChainId.SOLANA_CHAIN_ID)
                val btcAddress: Web3Address? = viewModel.getAddressesByChainId(wallet.id, Constants.ChainId.BITCOIN_CHAIN_ID)
                val pearlAddress: Web3Address? = viewModel.getAddressesByChainId(wallet.id, Constants.ChainId.PEARL_CHAIN_ID)
                val isEvmMatch: Boolean = evmAddress?.let { address: Web3Address ->
                    val index: Int = requireNotNull(CryptoWalletHelper.extractIndexFromPath(address.path!!))
                    val derivedAddress: String = CryptoWalletHelper.mnemonicToAddress(mnemonicPhrase, Constants.ChainId.ETHEREUM_CHAIN_ID, "", index)
                    derivedAddress.equals(address.destination, ignoreCase = true)
                } ?: false
                val isSolanaMatch: Boolean = solAddress?.let { address: Web3Address ->
                    val index: Int = requireNotNull(CryptoWalletHelper.extractIndexFromPath(address.path!!))
                    val derivedAddress: String = CryptoWalletHelper.mnemonicToAddress(mnemonicPhrase, Constants.ChainId.SOLANA_CHAIN_ID, "", index)
                    derivedAddress.equals(address.destination, ignoreCase = true)
                } ?: false
                val isBtcMatch: Boolean = btcAddress?.let { address: Web3Address ->
                    val index: Int = requireNotNull(CryptoWalletHelper.extractIndexFromPath(address.path!!))
                    val derivedAddress: String = CryptoWalletHelper.mnemonicToAddress(mnemonicPhrase, Constants.ChainId.BITCOIN_CHAIN_ID, "", index)
                    derivedAddress.equals(address.destination, ignoreCase = true)
                } ?: false
                val isPearlMatch: Boolean = pearlAddress?.let { address: Web3Address ->
                    val index: Int = requireNotNull(CryptoWalletHelper.extractIndexFromPath(address.path!!))
                    val derivedAddress: String = CryptoWalletHelper.mnemonicToAddress(mnemonicPhrase, Constants.ChainId.PEARL_CHAIN_ID, "", index)
                    derivedAddress.equals(address.destination, ignoreCase = true)
                } ?: false
                val isWalletMatch: Boolean =
                    (evmAddress == null || isEvmMatch) &&
                        (solAddress == null || isSolanaMatch) &&
                        (btcAddress == null || isBtcMatch) &&
                        (pearlAddress == null || isPearlMatch) &&
                        (isEvmMatch || isSolanaMatch || isBtcMatch || isPearlMatch)
                if (!isWalletMatch) {
                    return@forEach
                }
                if (currentSpendKey == null) {
                    Timber.e("Spend key is null, cannot save wallets.")
                    return@forEach
                }
                val isSaved: Boolean = viewModel.saveWeb3PrivateKey(context, currentSpendKey, wallet.id, mnemonic)
                if (!isSaved) {
                    return@forEach
                }
                val updated = runCatching { ensureUtxoAddresses(wallet.id, mnemonic) }
                    .onFailure { Timber.e(it, "Failed to update reimported wallet ${wallet.id}") }
                    .getOrDefault(false)
                if (!updated) {
                    return@forEach
                }
                RxBus.publish(WalletRefreshedEvent(wallet.id, WalletOperationType.CREATE))
                Timber.e("Save wallet key: ${wallet.id}")
                return@forEach
            }
        }
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
