package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponse
import one.mixin.android.databinding.FragmentLoginVerifyBottomSheetBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.event.TipEvent
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.wc.WCChangeEvent
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricLayout
import one.mixin.android.ui.tip.wc.WalletUnlockBottomSheetDialogFragment.Companion.TYPE_SOLANA
import one.mixin.android.util.reportException
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Account
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.widget.BottomSheet
import timber.log.Timber
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

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
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
            JsSigner.updateAddress(JsSigner.JsSignerNetwork.Solana.name, solAddress)
            val evmAddress = bottomViewModel.getTipAddress(requireContext(), pin, ETHEREUM_CHAIN_ID)
            PropertyHelper.updateKeyValue(EVM_ADDRESS, evmAddress)
            JsSigner.updateAddress(JsSigner.JsSignerNetwork.Ethereum.name, evmAddress)
        }
        return r
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
