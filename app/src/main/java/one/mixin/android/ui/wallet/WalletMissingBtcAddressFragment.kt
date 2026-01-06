package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.databinding.FragmentWalletMissingBtcAddressIntroBinding
import one.mixin.android.extension.decodeBase64
import one.mixin.android.repository.Web3Repository
import one.mixin.android.session.Session
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.WalletCategory
import org.bitcoinj.base.ScriptType
import org.bitcoinj.crypto.ECKey
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class WalletMissingBtcAddressFragment : Fragment(R.layout.fragment_wallet_missing_btc_address_intro) {

    interface Callback {
        fun onWalletMissingBtcAddressPinSuccess()
    }

    private val binding by viewBinding(FragmentWalletMissingBtcAddressIntroBinding::bind)

    private val bottomViewModel: BottomSheetViewModel by viewModels()

    @Inject
    lateinit var web3Repository: Web3Repository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.unlockByPin.setOnClickListener {
            showPinInput()
        }
    }

    private fun showPinInput() {
        val biometricInfo: BiometricInfo? = null
        PinInputBottomSheetDialogFragment
            .newInstance(title = null, biometricInfo = biometricInfo, from = 1)
            .setOnComplete { pin, dialog ->
                this@WalletMissingBtcAddressFragment.lifecycleScope.launch {
                    dialog.dismiss()
                    addBtcAddressIfNeeded(pin)
                    (activity as? Callback)?.onWalletMissingBtcAddressPinSuccess()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
            .showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }

    private suspend fun addBtcAddressIfNeeded(pin: String): Boolean {
        Timber.d("$TAG addBtcAddressIfNeeded start")
        val wallets = web3Repository.getAllWallets().filter { walletItem ->
            walletItem.category == WalletCategory.CLASSIC.value ||
                walletItem.category == WalletCategory.IMPORTED_MNEMONIC.value
        }
        Timber.d("$TAG wallets count=${wallets.size}")
        if (wallets.isEmpty()) {
            Timber.d("$TAG no wallets, skip")
            return true
        }
        val spendKey: ByteArray = bottomViewModel.getSpendKey(requireContext(), pin)
        for (walletItem in wallets) {
            val hasBtcAddress: Boolean = web3Repository.getAddressesByChainId(walletItem.id, Constants.ChainId.BITCOIN_CHAIN_ID) != null
            Timber.d("$TAG walletId=${walletItem.id} hasBtcAddress=$hasBtcAddress")
            if (hasBtcAddress) {
                continue
            }
            val localAddresses = web3Repository.getAddresses(walletItem.id)
            val localPath: String? = localAddresses.firstOrNull { it.path.isNullOrBlank().not() }?.path
            val derivationIndex: Int = localPath?.let { CryptoWalletHelper.extractIndexFromPath(it) } ?: 0
            Timber.d("$TAG walletId=${walletItem.id} localPath=$localPath derivationIndex=$derivationIndex")
            val now: Instant = Instant.now()
            val userId: String = requireNotNull(Session.getAccountId())
            val btcWallet = if (walletItem.category == WalletCategory.CLASSIC.value) {
                val btcAddress: String = bottomViewModel.getTipAddress(
                    requireContext(),
                    pin,
                    Constants.ChainId.BITCOIN_CHAIN_ID,
                    derivationIndex,
                )
                val btcPrivateKey: ByteArray = bottomViewModel.getTipPrivateKey(
                    requireContext(),
                    pin,
                    Constants.ChainId.BITCOIN_CHAIN_ID,
                    derivationIndex,
                )
                Pair(btcAddress, btcPrivateKey)
            } else {
                val mnemonic: String = CryptoWalletHelper.getWeb3Mnemonic(requireContext(), spendKey, walletItem.id)
                    ?: throw IllegalArgumentException("Missing mnemonic")
                val derivedWallet = CryptoWalletHelper.mnemonicToBitcoinSegwitWallet(mnemonic, index = derivationIndex)
                Pair(derivedWallet.address, Numeric.hexStringToByteArray(derivedWallet.privateKey))
            }
            val btcAddress: String = btcWallet.first
            val btcPrivateKey: ByteArray = btcWallet.second
            val message = "$btcAddress\n$userId\n${now.epochSecond}"
            val ecKey: ECKey = ECKey.fromPrivate(BigInteger(1, btcPrivateKey), true)
            val signature: String = Numeric.toHexString(ecKey.signMessage(message, ScriptType.P2WPKH).decodeBase64())
            Timber.d("$TAG walletId=${walletItem.id} derived btcAddress=$btcAddress")
            val updateRequest = WalletRequest(
                name = null,
                category = null,
                addresses = listOf(
                    Web3AddressRequest(
                        destination = btcAddress,
                        chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
                        path = Bip44Path.bitcoinSegwitPathString(derivationIndex),
                        signature = signature,
                        timestamp = now.toString(),
                    ),
                ),
            )
            val updateResponse = web3Repository.updateWallet(walletItem.id, updateRequest)
            Timber.d(
                "$TAG walletId=${walletItem.id} updateWallet isSuccess=${updateResponse.isSuccess} errorCode=${updateResponse.errorCode} errorDescription=${updateResponse.errorDescription}",
            )
            if (updateResponse.isSuccess.not()) {
                return false
            }
        }
        Timber.d("$TAG addBtcAddressIfNeeded end")
        return true
    }

    companion object {
        const val TAG: String = "WalletMissingBtcAddressFragment"

        fun newInstance(): WalletMissingBtcAddressFragment {
            return WalletMissingBtcAddressFragment()
        }
    }
}
