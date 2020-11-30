package one.mixin.android.ui.common.biometric

import android.app.Activity
import android.content.Intent
import androidx.core.view.postDelayed
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.databinding.FragmentTransferBottomSheetBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putLong
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding

abstract class BiometricBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    private var biometricDialog: BiometricDialog? = null
    private var dismissRunnable: Runnable? = null

    protected open val biometricBinding by viewBinding(FragmentTransferBottomSheetBinding::inflate)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BiometricUtil.REQUEST_CODE_CREDENTIALS && resultCode == Activity.RESULT_OK) {
            showBiometricPrompt()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (dismissRunnable != null) {
            contentView.removeCallbacks(dismissRunnable)
            callback?.onSuccess()
        }
    }

    abstract fun getBiometricInfo(): BiometricInfo

    abstract suspend fun invokeNetwork(pin: String): MixinResponse<*>

    /**
     * @return Return true will dismiss the bottom sheet, otherwise do nothing.
     */
    abstract fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean

    open fun doWithMixinErrorCode(errorCode: Int): String? {
        return null
    }

    protected fun setBiometricLayout() {
        biometricBinding.apply {
            titleView.rightIv.setOnClickListener { dismiss() }
            biometricLayout.setKeyboard(biometricBinding.keyboard)
            biometricLayout.callback = biometricLayoutCallback
            root.post {
                biometricLayout.keyboardHeight = keyboard.height
            }
        }
    }

    protected fun showErrorInfo(
        content: String,
        animate: Boolean = false,
        tickMillis: Long = 0L,
        errorAction: BiometricLayout.ErrorAction? = null
    ) {
        if (!isAdded) return
        biometricBinding.biometricLayout.showErrorInfo(content, animate, tickMillis, errorAction)
    }

    protected fun showDone() {
        if (!isAdded) return
        biometricBinding.biometricLayout.showDone()
        dismissRunnable = biometricBinding.root.postDelayed(3000) {
            dismissRunnable = null
            dismiss()
            callback?.onSuccess()
        }
    }

    private fun showBiometricPrompt() {
        biometricDialog = BiometricDialog(
            requireContext(),
            getBiometricInfo()
        )
        biometricDialog?.callback = biometricDialogCallback
        biometricDialog?.show()
    }

    protected fun showPin() {
        if (!isAdded) return
        biometricBinding.biometricLayout.showPin(true)
    }

    private fun onPinComplete(pin: String) = bottomViewModel.viewModelScope.launch {
        if (!isAdded) return@launch

        biometricBinding.biometricLayout.showPb()
        val response = try {
            withContext(Dispatchers.IO) {
                invokeNetwork(pin)
            }
        } catch (t: Throwable) {
            biometricBinding.biometricLayout.showPin(true)
            ErrorHandler.handleError(t)
            return@launch
        }

        if (response.isSuccess) {
            defaultSharedPreferences.putLong(
                Constants.BIOMETRIC_PIN_CHECK,
                System.currentTimeMillis()
            )
            context?.updatePinCheck()

            if (doWhenInvokeNetworkSuccess(response, pin)) {
                dismiss()
                callback?.onSuccess() ?: toast(R.string.successful)
            }
        } else {
            val errorString = doWithMixinErrorCode(response.errorCode)

            biometricBinding.biometricLayout.let { layout ->
                layout.setErrorButton(layout.getErrorActionByErrorCode(response.errorCode))
                layout.pin.clear()
            }
            val errorInfo = if (response.errorCode == ErrorHandler.PIN_INCORRECT || response.errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                val errorCount = bottomViewModel.errorCount()
                getString(R.string.error_pin_incorrect_with_times, ErrorHandler.PIN_INCORRECT, errorCount)
            } else if (!errorString.isNullOrBlank()) {
                errorString
            } else {
                requireContext().getMixinErrorStringByCode(response.errorCode, response.errorDescription)
            }
            showErrorInfo(errorInfo, true)
        }
    }

    private val biometricDialogCallback = object : BiometricDialog.Callback {
        override fun onPinComplete(pin: String) {
            this@BiometricBottomSheetDialogFragment.onPinComplete(pin)
        }

        override fun showPin() {
            biometricBinding.biometricLayout.showPin(false)
        }

        override fun showAuthenticationScreen() {
            BiometricUtil.showAuthenticationScreen(this@BiometricBottomSheetDialogFragment)
        }

        override fun onCancel() {
            context?.let {
                biometricBinding.biometricLayout.isBiometricTextVisible(BiometricUtil.shouldShowBiometric(it))
            }
        }
    }

    private val biometricLayoutCallback = object : BiometricLayout.Callback {
        override fun onPinComplete(pin: String) {
            this@BiometricBottomSheetDialogFragment.onPinComplete(pin)
        }

        override fun onShowBiometric() {
            showBiometricPrompt()
        }

        override fun onDismiss() {
            dismiss()
        }
    }

    var callback: Callback? = null

    interface Callback {
        fun onSuccess()
    }
}
