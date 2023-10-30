package one.mixin.android.ui.tip

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RegisterRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.databinding.FragmentCheckRegisterBottomSheetBinding
import one.mixin.android.event.TipEvent
import one.mixin.android.extension.toHex
import one.mixin.android.extension.toast
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.job.updateAccount
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricLayout
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Account
import one.mixin.android.widget.BottomSheet
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CheckRegisterBottomSheetDialogFragment : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "CheckRegisterBottomSheetDialogFragment"

        fun newInstance() = CheckRegisterBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentCheckRegisterBottomSheetBinding::inflate)

    @Inject
    lateinit var tip: Tip

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var tipCounterSynced: TipCounterSyncedLiveData

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
            dismissClickOutside = false
        }
        setBiometricLayout()
        binding.titleView.rightIv.isVisible = false
        binding.biometricLayout.measureAllChildren = false
        syncAccount()
    }

    private fun syncAccount() = lifecycleScope.launch {
        binding.biometricLayout.showPb()
        handleMixinResponse(
            invokeNetwork = { accountService.getMeSuspend() },
            successBlock = {
                val account = it.data
                if (account == null) {
                    binding.biometricLayout.showErrorInfo("account is null", true, errorAction = BiometricLayout.ErrorAction.Close)
                    return@handleMixinResponse
                }
                withContext(Dispatchers.IO) {
                    updateAccount(account)
                }
                if (account.hasSafe) {
                    dismiss()
                    return@handleMixinResponse
                }
                withContext(Dispatchers.IO) {
                    checkTipCounter(account)
                }
            },
            exceptionBlock = { t ->
                Timber.e("TIP $TAG sync account ${t.stackTraceToString()}")
                showErrorWhenRefreshAccountFailed(t.message ?: "refresh account failed")
                return@handleMixinResponse false
            },
            failureBlock = {
                val error = requireNotNull(it.error)
                val errorCode = error.code
                val errorDescription = error.description
                val errStr = requireContext().getMixinErrorStringByCode(errorCode, errorDescription)
                Timber.e("TIP $TAG sync account errorString $errStr")
                showErrorWhenRefreshAccountFailed(errStr)
                return@handleMixinResponse true
            }
        )
    }

    private suspend fun checkTipCounter(account: Account) {
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
                    binding.biometricLayout.showPin(true)
                }
            }
        } catch (e: Exception) {
            Timber.e("TIP $TAG checkCounter ${e.stackTraceToString()}")
            showErrorWhenCheckCounterFailed(e.message ?: "checkCounter failed", account)
        }
    }

    private suspend fun registerPublicKey(pin: String) {
        try {
            val seed = tip.getOrRecoverTipPriv(requireContext(), pin).getOrThrow()
            val (saltBase64, keyPair) = tip.generateSaltAndKeyPair(pin, seed)
            val pkHex = keyPair.publicKey.toHex()
            val selfId = requireNotNull(Session.getAccountId()) { "self userId can not be null at this step" }
            val resp = bottomViewModel.registerPublicKey(
                registerRequest = RegisterRequest(
                    publicKey = pkHex,
                    signature = Session.getRegisterSignature(selfId, keyPair.privateKey),
                    pin = bottomViewModel.getEncryptedTipBody(selfId, pkHex, pin),
                    salt = saltBase64,
                )
            )
            if (resp.isSuccess) {
                resp.data?.let { account ->
                    Session.storeAccount(account)
                    dismiss()
                    toast(R.string.Successful)
                }
            } else {
                val error = requireNotNull(resp.error)
                val errorCode = error.code
                val errorDescription = error.description
                val errStr = requireContext().getMixinErrorStringByCode(errorCode, errorDescription)
                Timber.e("TIP $TAG register public key errorString $errStr")
                showErrorWhenRegisterFailed(pin, errStr)
            }
        } catch (e: Exception) {
            Timber.e("TIP $TAG register public key ${e.stackTraceToString()}")
            showErrorWhenRegisterFailed(pin, e.message ?: "register public key failed, please retry")
        }
    }

    private fun showErrorWhenRefreshAccountFailed(errorString: String) {
        if (!isAdded) return
        binding.biometricLayout.apply {
            showErrorInfo(errorString, true, errorAction = BiometricLayout.ErrorAction.RetryPin)
            errorBtn.setOnClickListener {
                showPin(true)
                syncAccount()
            }
        }
    }

    private fun showErrorWhenCheckCounterFailed(errorString: String, account: Account) {
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

    private fun showErrorWhenRegisterFailed(pin: String, errorString: String) {
        if (!isAdded) return
        binding.biometricLayout.apply {
            showErrorInfo(errorString, true, errorAction = BiometricLayout.ErrorAction.RetryPin)
            errorBtn.setOnClickListener {
                lifecycleScope.launch {
                    showPb()
                    registerPublicKey(pin)
                }
            }
        }
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return bottomViewModel.verifyPin(pin)
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        lifecycleScope.launch {
            registerPublicKey(pin)
        }
        return false
    }

    override fun getBiometricInfo() = BiometricInfo(
        getString(R.string.Verify_by_Biometric),
        "",
        "",
    )
}
