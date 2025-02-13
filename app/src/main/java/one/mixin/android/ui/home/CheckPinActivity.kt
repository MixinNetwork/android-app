package one.mixin.android.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import one.mixin.android.MixinApplication
import one.mixin.android.ui.common.BaseActivity
import android.app.ActivityOptions
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import one.mixin.android.R
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.ActivityCheckPinBinding
import one.mixin.android.ui.common.BottomSheetViewModel
import timber.log.Timber
import kotlin.getValue
import com.google.firebase.perf.FirebasePerformance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_LOGIN_VERIFY
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.ResponseError
import one.mixin.android.event.TipEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.exception.TipCounterNotSyncedException
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.tip.isTipNodeException
import one.mixin.android.ui.common.LoginVerifyBottomSheetDialogFragment.Companion.TAG
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricLayout
import one.mixin.android.ui.common.biometric.getUtxoExceptionMsg
import one.mixin.android.ui.common.biometric.isUtxoException
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.reportException
import one.mixin.android.vo.Account
import javax.inject.Inject

@AndroidEntryPoint
class CheckPinActivity : BaseActivity() {

    private lateinit var binding: ActivityCheckPinBinding
    private val bottomViewModel by viewModels<BottomSheetViewModel>()

    @Inject
    lateinit var tip: Tip

    @Inject
    lateinit var tipCounterSynced: TipCounterSyncedLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MixinApplication.get().isOnline.set(false)
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityCheckPinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            biometricLayout.setKeyboard(keyboard)
            biometricLayout.callback = biometricLayoutCallback
        }

        lifecycleScope.launch {
            checkTipCounter(requireNotNull(Session.getAccount()))
        }
    }

    private suspend fun verifyPin(pin: String): MixinResponse<*> {
        return bottomViewModel.verifyPin(pin)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private suspend fun checkTipCounter(account: Account) {
        binding.biometricLayout.showPb()
        try {
            tip.checkCounter(
                account.tipCounter,
                onNodeCounterNotEqualServer = { nodeMaxCounter, failedSigners ->
                    RxBus.publish(TipEvent(nodeMaxCounter, failedSigners))
                    withContext(Dispatchers.Main) {
                        // dismiss()
                    }
                },
                onNodeCounterInconsistency = { nodeMaxCounter, failedSigners ->
                    RxBus.publish(TipEvent(nodeMaxCounter, failedSigners))
                    withContext(Dispatchers.Main) {
                        // dismiss()
                    }
                },
            ).onSuccess {
                tipCounterSynced.synced = true

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

    private fun onPinComplete(pin: String) =
        lifecycleScope.launch {
            binding.biometricLayout.showPb()

            val response =
                try {
                    withContext(Dispatchers.IO) {
                        verifyPin(pin)
                    }
                } catch (t: Throwable) {
                    handleThrowableWithPin(t, pin)
                    return@launch
                }

            if (response.isSuccess) {
                defaultSharedPreferences.putBoolean(PREF_LOGIN_VERIFY, false)
                defaultSharedPreferences.putLong(
                    Constants.BIOMETRIC_PIN_CHECK,
                    System.currentTimeMillis(),
                )
                updatePinCheck()
                finish()
            } else {
                handleWithErrorCodeAndDesc(pin, requireNotNull(response.error))
            }
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

    private val biometricLayoutCallback =
        object : BiometricLayout.Callback {
            override fun onPinComplete(pin: String) {
                this@CheckPinActivity.onPinComplete(pin)
            }

            override fun onShowBiometric() {
            }

            override fun onDismiss() {
            }
        }

    private suspend fun handleThrowableWithPin(
        t: Throwable,
        pin: String,
    ) {
        when (t) {
            is TipNetworkException -> {
                handleWithErrorCodeAndDesc(pin, t.error)
            }
            is TipCounterNotSyncedException -> {
                window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                binding.biometricLayout.showPin(true)
            }
            else -> {
                window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                binding.biometricLayout.showPin(true)
                if (t.isTipNodeException()) {
                    showErrorInfo(t.getTipExceptionMsg(this, null), errorAction = BiometricLayout.ErrorAction.RetryPin)
                } else if (t.isUtxoException()) {
                    showErrorInfo(t.getUtxoExceptionMsg(this), errorAction = BiometricLayout.ErrorAction.Close)
                } else {
                    ErrorHandler.handleError(t)
                }
            }
        }
    }


    private suspend fun handleWithErrorCodeAndDesc(
        pin: String,
        error: ResponseError,
    ) {
        val errorCode = error.code
        val errorDescription = error.description
        val errorString = null

        binding.biometricLayout.let { layout ->
            layout.setErrorButton(layout.getErrorActionByErrorCode(errorCode))
            layout.pin.clear()
        }
        val errorInfo =
            if (errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                getString(R.string.error_pin_check_too_many_request)
            } else if (errorCode == ErrorHandler.PIN_INCORRECT) {
                val errorCount = bottomViewModel.errorCount()
                resources.getQuantityString(R.plurals.error_pin_incorrect_with_times, errorCount, errorCount)
            } else if (!errorString.isNullOrBlank()) {
                errorString
            } else {
                this.getMixinErrorStringByCode(errorCode, errorDescription)
            }
        showErrorInfo(errorInfo)
    }


    private fun showErrorInfo(
        content: String,
        tickMillis: Long = 0L,
        errorAction: BiometricLayout.ErrorAction? = null,
    ) {
        binding.biometricLayout.showErrorInfo(content, tickMillis, errorAction)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }


    companion object {
        fun start(activity: Activity) {
            val intent = Intent(activity, CheckPinActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val options = ActivityOptions.makeCustomAnimation(
                activity,
                R.anim.slide_in_bottom,
                R.anim.stay
            )
            activity.startActivity(intent, options.toBundle())
        }
    }
}