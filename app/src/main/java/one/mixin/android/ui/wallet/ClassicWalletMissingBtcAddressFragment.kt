package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.databinding.FragmentClassicWalletMissingBtcAddressIntroBinding
import one.mixin.android.extension.toHex
import one.mixin.android.repository.Web3Repository
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.encodeToBase58WithChecksum
import one.mixin.android.util.viewBinding
import org.web3j.utils.Numeric
import timber.log.Timber
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
            .setOnComplete { pin, _ ->
                lifecycleScope.launch {
                    addBtcAddressIfNeeded(pin)
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
        Timber.d("$TAG getTipAddress start chainId=${Constants.ChainId.BITCOIN_CHAIN_ID}")
        val btcAddress: String = bottomViewModel.getTipAddress(requireContext(), pin, Constants.ChainId.BITCOIN_CHAIN_ID)
        Timber.d("$TAG getTipAddress success btcAddress=$btcAddress")
        val updateRequest = WalletRequest(
            name = null,
            category = null,
            addresses = listOf(
                Web3AddressRequest(
                    destination = btcAddress,
                    chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
                    path = Bip44Path.bitcoinSegwitPathString(),
                ),
            ),
        )
        Timber.d("$TAG updateRequest.addresses=${updateRequest.addresses}")
        Timber.w("$TAG wallet update logic is currently disabled (commented out). No updateWallet/getWalletAddresses/insert will be executed.")
//        classicWallets.forEach { walletItem ->
//            val hasBtcAddress: Boolean = web3Repository.getAddressesByChainId(walletItem.id, Constants.ChainId.BITCOIN_CHAIN_ID) != null
//            if (hasBtcAddress) return@forEach
//            val updateResponse = web3Repository.updateWallet(walletItem.id, updateRequest)
//            if (updateResponse.isSuccess.not()) {
//                return false
//            }
//            val addressesResponse = web3Repository.routeService.getWalletAddresses(walletItem.id)
//            if (addressesResponse.isSuccess.not() || addressesResponse.data.isNullOrEmpty()) {
//                return false
//            }
//            web3Repository.insertAddressList(addressesResponse.data!!)
//        }
        classicWallets.forEach { walletItem ->
            val hasBtcAddress: Boolean = web3Repository.getAddressesByChainId(walletItem.id, Constants.ChainId.BITCOIN_CHAIN_ID) != null
            Timber.d("$TAG walletId=${walletItem.id} hasBtcAddress=$hasBtcAddress")
        }
        Timber.d("$TAG addBtcAddressIfNeeded end (skipped actual update)")
        return true
    }

    companion object {
        const val TAG: String = "ClassicWalletMissingBtcAddressFragment"

        fun newInstance(): ClassicWalletMissingBtcAddressFragment {
            return ClassicWalletMissingBtcAddressFragment()
        }
    }
}
