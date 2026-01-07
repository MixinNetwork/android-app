package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.BTC_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.ChainId.BITCOIN_CHAIN_ID
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponse
import one.mixin.android.databinding.FragmentLoginVerifyBottomSheetBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.event.TipEvent
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.decodeBase64
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.repository.Web3Repository
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricLayout
import one.mixin.android.ui.logs.LogViewerBottomSheet
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.reportException
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Account
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.widget.BottomSheet
import timber.log.Timber
import org.bitcoinj.base.ScriptType
import org.bitcoinj.crypto.ECKey
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class LoginVerifyBottomSheetDialogFragment : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "LoginVerifyBottomSheetDialogFragment"

        fun newInstance() = LoginVerifyBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentLoginVerifyBottomSheetBinding::inflate)

    @Inject
    lateinit var tip: Tip

    @Inject
    lateinit var tipCounterSynced: TipCounterSyncedLiveData

    @Inject
    lateinit var web3Repository: Web3Repository

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        Timber.e("LoginVerifyBottomSheetDialogFragment setupDialog")
        contentView = binding.root
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
            dismissClickOutside = false
        }
        setBiometricLayout()
        binding.biometricLayout.apply {
            pin.isEnabled = false
            measureAllChildren = false
        }
        binding.debug.setOnLongClickListener{
            LogViewerBottomSheet.newInstance().showNow(parentFragmentManager, LogViewerBottomSheet.TAG)
            true
        }
        binding.support.setOnClickListener {
            context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
        }
        lifecycleScope.launch {
            checkTipCounter(Session.getAccount()!!)
        }
    }

    private suspend fun checkTipCounter(account: Account) {
        binding.biometricLayout.showPb()
        try {
            tip.checkCounter(
                account.tipCounter,
                onNodeCounterNotEqualServer = { nodeMaxCounter, failedSigners ->
                    RxBus.publish(TipEvent(nodeMaxCounter, failedSigners))
                    withContext(Dispatchers.Main) {
                        dismiss()
                    }
                },
                onNodeCounterInconsistency = { nodeMaxCounter, failedSigners ->
                    RxBus.publish(TipEvent(nodeMaxCounter, failedSigners))
                    withContext(Dispatchers.Main) {
                        dismiss()
                    }
                },
            ).onSuccess {
                tipCounterSynced.synced = true

                if (!isAdded) return
                withContext(Dispatchers.Main) {
                    binding.biometricLayout.pin.isEnabled = true
                    binding.biometricLayout.showPin(true)
                }
            }
        } catch (e: Exception) {
            val msg = "TIP $TAG checkCounter ${e.stackTraceToString()}"
            Timber.e(msg)
            reportException(msg, e)
            showErrorWhenCheckCounterFailed(e.message ?: "checkCounter failed", account)
        }
    }

    private fun showErrorWhenCheckCounterFailed(
        errorString: String,
        account: Account,
    ) {
        if (!isAdded) return
        binding.biometricLayout.apply {
            showErrorInfo(errorString, true, errorAction = BiometricLayout.ErrorAction.RetryPin)
            errorBtn.setOnClickListener {
                lifecycleScope.launch {
                    showPin(true)
                    checkTipCounter(account)
                }
            }
        }
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        val r = bottomViewModel.verifyPin(pin)
        if (r.isSuccess) {
            val solAddress = bottomViewModel.getTipAddress(requireContext(), pin, SOLANA_CHAIN_ID)
            PropertyHelper.updateKeyValue(SOLANA_ADDRESS, solAddress)
            Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Solana.name, solAddress)
            val evmAddress = bottomViewModel.getTipAddress(requireContext(), pin, ETHEREUM_CHAIN_ID)
            PropertyHelper.updateKeyValue(EVM_ADDRESS, evmAddress)
            Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Ethereum.name, evmAddress)
            val btcAddress = bottomViewModel.getTipAddress(requireContext(), pin, BITCOIN_CHAIN_ID)
            PropertyHelper.updateKeyValue(BTC_ADDRESS, btcAddress)
            addBtcAddressIfNeeded(pin)
            AnalyticsTracker.trackLoginEnd()
        }
        return r
    }

    private suspend fun addBtcAddressIfNeeded(pin: String): Boolean {
        val wallets = web3Repository.getAllWallets().filter { walletItem ->
            walletItem.category == WalletCategory.CLASSIC.value ||
                walletItem.category == WalletCategory.IMPORTED_MNEMONIC.value
        }
        if (wallets.isEmpty()) {
            return true
        }
        val hasAnyMissingBtcAddress: Boolean = wallets.any { walletItem ->
            web3Repository.getAddressesByChainId(walletItem.id, BITCOIN_CHAIN_ID) == null
        }
        if (!hasAnyMissingBtcAddress) {
            return true
        }
        val spendKey: ByteArray = bottomViewModel.getSpendKey(requireContext(), pin)
        for (walletItem in wallets) {
            val hasBtcAddress: Boolean = web3Repository.getAddressesByChainId(walletItem.id, BITCOIN_CHAIN_ID) != null
            if (hasBtcAddress) {
                continue
            }
            val localAddresses = web3Repository.getAddresses(walletItem.id)
            val localPath: String? = localAddresses.firstOrNull { it.path.isNullOrBlank().not() }?.path
            val derivationIndex: Int = localPath?.let { CryptoWalletHelper.extractIndexFromPath(it) } ?: 0
            val now: Instant = Instant.now()
            val userId: String = requireNotNull(Session.getAccountId())
            val btcWallet: Pair<String, ByteArray> = if (walletItem.category == WalletCategory.CLASSIC.value) {
                val btcAddress: String = bottomViewModel.getTipAddress(
                    requireContext(),
                    pin,
                    BITCOIN_CHAIN_ID,
                    derivationIndex,
                )
                val btcPrivateKey: ByteArray = bottomViewModel.getTipPrivateKey(
                    requireContext(),
                    pin,
                    BITCOIN_CHAIN_ID,
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
            val updateRequest = WalletRequest(
                name = null,
                category = null,
                addresses = listOf(
                    Web3AddressRequest(
                        destination = btcAddress,
                        chainId = BITCOIN_CHAIN_ID,
                        path = one.mixin.android.tip.bip44.Bip44Path.bitcoinSegwitPathString(derivationIndex),
                        signature = signature,
                        timestamp = now.toString(),
                    ),
                ),
            )
            val updateResponse = web3Repository.updateWallet(walletItem.id, updateRequest)
            if (updateResponse.isSuccess.not()) {
                return false
            }
        }
        return true
    }

    private var pinSuccess = false
    var onDismissCallback: ((Boolean) -> Unit)? = null

    override fun doWhenInvokeNetworkSuccess(
        response: MixinResponse<*>,
        pin: String,
    ): Boolean {
        pinSuccess = true
        return true
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke(pinSuccess)
    }

    override fun getBiometricInfo() =
        BiometricInfo(
            getString(R.string.Verify_by_Biometric),
            "",
            "",
        )
}
