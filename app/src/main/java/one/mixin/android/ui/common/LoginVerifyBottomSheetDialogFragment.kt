package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.view.ContextThemeWrapper
import android.view.View
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId.BITCOIN_CHAIN_ID
import one.mixin.android.Constants.ChainId.PEARL_CHAIN_ID
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.databinding.FragmentLoginVerifyBottomSheetBinding
import one.mixin.android.databinding.ViewLoginVerifyMoreBottomBinding
import one.mixin.android.event.TipEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putBoolean
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.repository.Web3Repository
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricLayout
import one.mixin.android.ui.logs.LogViewerBottomSheet
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.ui.tip.TipFlowInteractor
import one.mixin.android.ui.wallet.signUtxoAddressMessage
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.reportException
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Account
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.widget.BottomSheet
import org.web3j.utils.Numeric
import timber.log.Timber
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

    @Inject
    lateinit var tipFlowInteractor: TipFlowInteractor

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
        binding.more.setOnClickListener {
            showMoreMenu()
        }
        binding.support.setOnClickListener {
            context?.openUrl(
                Constants.HelpLink.CUSTOMER_SERVICE,
                source = AnalyticsTracker.CustomerServiceSource.LOGIN_PIN_VERIFY,
            )
        }
        lifecycleScope.launch {
            checkTipCounter(Session.getAccount()!!)
        }
    }

    private fun showMoreMenu() {
        if (!isAdded) return
        val builder = BottomSheet.Builder(requireActivity())
        val view: View =
            View.inflate(
                ContextThemeWrapper(requireActivity(), R.style.Custom),
                R.layout.view_login_verify_more_bottom,
                null,
            )
        val menuBinding = ViewLoginVerifyMoreBottomBinding.bind(view)
        builder.setCustomView(menuBinding.root)
        val bottomSheet = builder.create()
        menuBinding.closeIv.setOnClickListener { bottomSheet.dismiss() }
        menuBinding.forgetPinTv.setOnClickListener {
            context?.openUrl(getString(R.string.forget_pin_help_url))
            bottomSheet.dismiss()
        }
        menuBinding.switchAccountTv.setOnClickListener {
            bottomSheet.dismiss()
            dismissAllowingStateLoss()
            MixinApplication.get().closeAndClear(force = true)
        }
        menuBinding.logsTv.setOnClickListener {
            bottomSheet.dismiss()
            context?.let(SettingActivity::showPinLogs)
        }
        bottomSheet.show()
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
            if (tipFlowInteractor.ensureClassicWallet(requireContext(), pin) == null) {
                return MixinResponse<Any>(IllegalStateException(getString(R.string.Save_failure)))
            }
            MixinApplication.appContext.defaultSharedPreferences.putBoolean(Constants.Account.PREF_WEB3_ADDRESSES_SYNCED, true)
            addUtxoAddressesIfNeeded(pin)?.let { return it }
            synchronizeSelectedWalletSigner()
            AnalyticsTracker.trackLoginEnd()
        }
        return r
    }

    private suspend fun synchronizeSelectedWalletSigner() {
        val expectedWalletId = Web3Signer.currentWalletIdSnapshot()
        val selectedWallet = expectedWalletId
            .takeIf(String::isNotBlank)
            ?.let { walletId -> web3Repository.findWalletById(walletId) }
        val wallet = selectedWallet ?: web3Repository.getClassicWalletId()
            ?.let { walletId -> web3Repository.findWalletById(walletId) }
            ?: return
        val addresses = web3Repository.getAddresses(wallet.id)
        Web3Signer.setWalletIfCurrent(expectedWalletId, wallet.id, wallet.category) { queryWalletId ->
            if (queryWalletId == wallet.id) addresses else emptyList()
        }
    }

    private suspend fun addUtxoAddressesIfNeeded(pin: String): MixinResponse<*>? {
        if (!defaultSharedPreferences.getBoolean(Constants.Account.PREF_WEB3_ADDRESSES_SYNCED, false)) return null
        val wallets = web3Repository.getAllWallets().filter { walletItem ->
            walletItem.category == WalletCategory.CLASSIC.value ||
                (walletItem.category == WalletCategory.IMPORTED_MNEMONIC.value && walletItem.hasLocalPrivateKey)
        }
        if (wallets.isEmpty()) {
            return null
        }
        val hasAnyMissingUtxoAddress: Boolean = wallets.any { walletItem ->
            val addresses = web3Repository.getAddresses(walletItem.id)
            CryptoWalletHelper.hasMissingUtxoAddress(
                chainIds = addresses.map { it.chainId },
            )
        }
        if (!hasAnyMissingUtxoAddress) {
            return null
        }
        val spendKey: ByteArray = bottomViewModel.getSpendKey(requireContext(), pin)
        for (walletItem in wallets) {
            val localAddresses = web3Repository.getAddresses(walletItem.id)
            val derivationIndex = CryptoWalletHelper.extractIndexFromPaths(localAddresses.map { it.path }) ?: 0
            val hasBtcAddress: Boolean = localAddresses.any { it.chainId == BITCOIN_CHAIN_ID }
            val hasPearlAddress: Boolean = localAddresses.any { it.chainId == PEARL_CHAIN_ID }
            if (hasBtcAddress && hasPearlAddress) {
                continue
            }
            val now: Instant = Instant.now()
            val userId: String = requireNotNull(Session.getAccountId())
            val mnemonic: String? = if (walletItem.category == WalletCategory.CLASSIC.value) {
                null
            } else {
                val decryptedMnemonic = CryptoWalletHelper.getWeb3Mnemonic(requireContext(), spendKey, walletItem.id)
                when (importedMnemonicBackfillAction(walletItem.category, decryptedMnemonic)) {
                    ImportedMnemonicBackfillAction.PROCESS -> requireNotNull(decryptedMnemonic)
                    ImportedMnemonicBackfillAction.SKIP -> continue
                }
            }
            val addressRequests = mutableListOf<Web3AddressRequest>()
            if (!hasBtcAddress) {
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
                    val importedMnemonic = requireNotNull(mnemonic)
                    val derivedWallet = CryptoWalletHelper.mnemonicToBitcoinSegwitWallet(importedMnemonic, index = derivationIndex)
                    Pair(derivedWallet.address, Numeric.hexStringToByteArray(derivedWallet.privateKey))
                }
                val message = "${btcWallet.first}\n$userId\n${now.epochSecond}"
                addressRequests += Web3AddressRequest(
                    destination = btcWallet.first,
                    chainId = BITCOIN_CHAIN_ID,
                    path = Bip44Path.bitcoinSegwitPathString(derivationIndex),
                    signature = signUtxoAddressMessage(btcWallet.second, message, BITCOIN_CHAIN_ID),
                    timestamp = now.toString(),
                )
            }
            if (!hasPearlAddress) {
                val pearlWallet: Pair<String, ByteArray> = if (walletItem.category == WalletCategory.CLASSIC.value) {
                    val pearlAddress: String = bottomViewModel.getTipAddress(
                        requireContext(),
                        pin,
                        PEARL_CHAIN_ID,
                        derivationIndex,
                    )
                    val pearlPrivateKey: ByteArray = bottomViewModel.getTipPrivateKey(
                        requireContext(),
                        pin,
                        PEARL_CHAIN_ID,
                        derivationIndex,
                    )
                    Pair(pearlAddress, pearlPrivateKey)
                } else {
                    val importedMnemonic = requireNotNull(mnemonic)
                    val derivedWallet = CryptoWalletHelper.mnemonicToPearlWallet(importedMnemonic, index = derivationIndex)
                    Pair(derivedWallet.address, Numeric.hexStringToByteArray(derivedWallet.privateKey))
                }
                val message = "${pearlWallet.first}\n$userId\n${now.epochSecond}"
                addressRequests += Web3AddressRequest(
                    destination = pearlWallet.first,
                    chainId = PEARL_CHAIN_ID,
                    path = Bip44Path.pearlPathString(derivationIndex),
                    signature = signUtxoAddressMessage(pearlWallet.second, message, PEARL_CHAIN_ID),
                    timestamp = now.toString(),
                )
            }
            val updateRequest = WalletRequest(
                name = null,
                category = null,
                addresses = addressRequests,
            )
            val updateResponse = web3Repository.updateWallet(walletItem.id, updateRequest)
            if (updateResponse.isSuccess.not()) {
                return updateResponse
            } else {
                updateResponse.data?.addresses?.let { addresses ->
                    web3Repository.insertAddressList(addresses)
                }
            }
        }
        return null
    }

    private var pinSuccess = false
    private var verifiedPin: String? = null
    var onDismissCallback: ((Boolean, String?) -> Unit)? = null

    override fun doWhenInvokeNetworkSuccess(
        response: MixinResponse<*>,
        pin: String,
    ): Boolean {
        pinSuccess = true
        verifiedPin = pin
        return true
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke(pinSuccess, verifiedPin)
    }

    override fun getBiometricInfo() =
        BiometricInfo(
            getString(R.string.Verify_by_Biometric),
            "",
            "",
        )
}
