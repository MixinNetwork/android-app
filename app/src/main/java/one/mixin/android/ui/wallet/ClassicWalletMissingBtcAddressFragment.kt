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
import one.mixin.android.databinding.FragmentClassicWalletMissingBtcAddressIntroBinding
import one.mixin.android.repository.Web3Repository
import one.mixin.android.session.Session
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.viewBinding
import org.bitcoinj.crypto.ECKey
import timber.log.Timber
import java.math.BigInteger
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class ClassicWalletMissingBtcAddressFragment : Fragment(R.layout.fragment_classic_wallet_missing_btc_address_intro) {

    interface Callback {
        fun onClassicWalletMissingBtcAddressPinSuccess()
    }

    private val binding by viewBinding(FragmentClassicWalletMissingBtcAddressIntroBinding::bind)

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
                this@ClassicWalletMissingBtcAddressFragment.lifecycleScope.launch {
                    dialog.dismiss()
                    addBtcAddressIfNeeded(pin)
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
            .showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }

    private suspend fun addBtcAddressIfNeeded(pin: String): Boolean {
        Timber.d("$TAG addBtcAddressIfNeeded start")
        val classicWallets = web3Repository.web3WalletDao.getAllClassicWallets()
        Timber.d("$TAG classic wallets count=${classicWallets.size}")
        if (classicWallets.isEmpty()) {
            Timber.d("$TAG no classic wallets, skip")
            return true
        }
        for (walletItem in classicWallets) {
            val hasBtcAddress: Boolean = web3Repository.getAddressesByChainId(walletItem.id, Constants.ChainId.BITCOIN_CHAIN_ID) != null
            Timber.d("$TAG walletId=${walletItem.id} hasBtcAddress=$hasBtcAddress")
            if (hasBtcAddress) {
                continue
            }
            val localAddresses = web3Repository.getAddresses(walletItem.id)
            val localPath: String? = localAddresses.firstOrNull { it.path.isNullOrBlank().not() }?.path
            val derivationIndex: Int = localPath?.let { CryptoWalletHelper.extractIndexFromPath(it) } ?: 0

            Timber.d("$TAG walletId=${walletItem.id} localPath=$localPath derivationIndex=$derivationIndex")
            val btcAddress: String = bottomViewModel.getTipAddress(
                requireContext(),
                pin,
                Constants.ChainId.BITCOIN_CHAIN_ID,
                derivationIndex,
            )
            val now: Instant = Instant.now()
            val userId: String = requireNotNull(Session.getAccountId())
            val message = "$btcAddress\n$userId\n${now.epochSecond}"
            val btcPrivateKey: ByteArray = bottomViewModel.getTipPrivateKey(
                requireContext(),
                pin,
                Constants.ChainId.BITCOIN_CHAIN_ID,
                derivationIndex,
            )
            val ecKey: ECKey = ECKey.fromPrivate(BigInteger(1, btcPrivateKey), true)
            @Suppress("DEPRECATION")
            val signature: String = ecKey.signMessage(message)
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
            Timber.d("$TAG walletId=${walletItem.id} updateRequest.addresses=${updateRequest.addresses}")
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
        const val TAG: String = "ClassicWalletMissingBtcAddressFragment"

        fun newInstance(): ClassicWalletMissingBtcAddressFragment {
            return ClassicWalletMissingBtcAddressFragment()
        }
    }
}
