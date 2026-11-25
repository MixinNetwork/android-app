package one.mixin.android.ui.oldwallet.biometric

import android.content.DialogInterface
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.ResponseError
import one.mixin.android.crypto.PinCipher
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putLong
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.tip.exception.TipCounterNotSyncedException
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.tip.isTipNodeException
import one.mixin.android.ui.oldwallet.MixinBottomSheetDialogFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.FlagSecureReason
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.RoundTitleView
import javax.inject.Inject

abstract class BiometricBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    override val centerOnExpandedScreen = true

    private var biometricDialog: BiometricDialog? = null
    private var pinSecure = true
    private var flagSecureReason: FlagSecureReason? = null

    @Inject
    lateinit var pinCipher: PinCipher

    private fun setPinSecure(secure: Boolean) {
        pinSecure = secure
        flagSecureReason?.invalidate()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheet {
        val dialog = super.onCreateDialog(savedInstanceState)
        pinSecure = true
        flagSecureReason?.detach()
        flagSecureReason = FlagSecureReason(dialog.window) { pinSecure }.apply { attach() }
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        flagSecureReason?.detach()
        flagSecureReason = null
        biometricDialog?.callback = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback?.onDismiss(isSuccess)
    }

    abstract fun getBiometricInfo(): BiometricInfo

    abstract suspend fun invokeNetwork(pin: String): MixinResponse<*>

    /**
     * @return Return true will dismiss the bottom sheet, otherwise do nothing.
     */
    abstract fun doWhenInvokeNetworkSuccess(
        response: MixinResponse<*>,
        pin: String,
    ): Boolean

    open suspend fun doWithMixinErrorCode(
        errorCode: Int,
        pin: String,
    ): String? {
        return null
    }

    open fun onClickBiometricLayoutClose(): Boolean = false

    private val titleView by lazy {
        contentView.findViewById<RoundTitleView>(R.id.title_view)
    }

    private val biometricLayout by lazy {
        contentView.findViewById<BiometricLayout>(R.id.biometric_layout)
    }
    private val keyboard by lazy {
        contentView.findViewById<Keyboard>(R.id.keyboard)
    }

    protected fun setBiometricLayout() {
        titleView.rightIv.setOnClickListener { dismiss() }
        biometricLayout.setKeyboard(keyboard)
        biometricLayout.callback = biometricLayoutCallback
        contentView.post {
            biometricLayout.keyboardHeight = keyboard.height
        }
    }

    protected fun showErrorInfo(
        content: String,
        animate: Boolean = false,
        tickMillis: Long = 0L,
        errorAction: BiometricLayout.ErrorAction? = null,
    ) {
        if (!isAdded) return
        biometricLayout.showErrorInfo(content, animate, tickMillis, errorAction)
        setPinSecure(false)
    }

    private var isSuccess = false

    protected fun showDone(
        returnTo: String? = null,
    ) {
        if (!isAdded) return
        biometricLayout.showDone(returnTo) {
            isSuccess = true
            dismiss()
            setPinSecure(false)
        }
    }

    private fun showBiometricPrompt() {
        biometricDialog =
            BiometricDialog(
                requireActivity(),
                getBiometricInfo(),
            )
        biometricDialog?.callback = biometricDialogCallback
        biometricDialog?.show()
    }

    protected fun showPin() {
        if (!isAdded) return
        setPinSecure(true)
        biometricLayout.showPin(true)
    }

    private fun onPinComplete(pin: String) =
        lifecycleScope.launch {
            if (!isAdded) return@launch

            biometricLayout.showPb()

            val response =
                try {
                    // initialize this in main thread
                    bottomViewModel

                    withContext(Dispatchers.IO) {
                        invokeNetwork(pin)
                    }
                } catch (t: Throwable) {
                    handleThrowableWithPin(t, pin)
                    return@launch
                }

            if (response.isSuccess) {
                defaultSharedPreferences.putLong(
                    Constants.BIOMETRIC_PIN_CHECK,
                    System.currentTimeMillis(),
                )
                context?.updatePinCheck()
                isSuccess = true

                if (doWhenInvokeNetworkSuccess(response, pin)) {
                    dismiss()
                }
            } else {
                handleWithErrorCodeAndDesc(pin, requireNotNull(response.error))
            }
            setPinSecure(false)
        }

    protected suspend fun handleThrowableWithPin(
        t: Throwable,
        pin: String,
    ) {
        when (t) {
            is TipNetworkException -> {
                handleWithErrorCodeAndDesc(pin, t.error)
            }
            is TipCounterNotSyncedException -> {
                setPinSecure(true)
                biometricLayout.showPin(true)
            }
            else -> {
                setPinSecure(true)
                biometricLayout.showPin(true)
                if (t.isTipNodeException()) {
                    showErrorInfo(t.getTipExceptionMsg(requireContext(), null), true, errorAction = BiometricLayout.ErrorAction.Close)
                } else {
                    ErrorHandler.handleError(t)
                }
            }
        }
    }

    protected suspend fun handleWithErrorCodeAndDesc(
        pin: String,
        error: ResponseError,
    ) {
        val errorCode = error.code
        val errorDescription = error.description
        val errorString = doWithMixinErrorCode(errorCode, pin)

        biometricLayout.let { layout ->
            layout.setErrorButton(layout.getErrorActionByErrorCode(errorCode))
            layout.pin.clear()
        }
        val errorInfo =
            if (errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                requireContext().getString(R.string.error_pin_check_too_many_request)
            } else if (errorCode == ErrorHandler.PIN_INCORRECT) {
                val errorCount = bottomViewModel.errorCount()
                requireContext().resources.getQuantityString(R.plurals.error_pin_incorrect_with_times, errorCount, errorCount)
            } else if (!errorString.isNullOrBlank()) {
                errorString
            } else {
                requireContext().getMixinErrorStringByCode(errorCode, errorDescription)
            }
        showErrorInfo(errorInfo, true)
    }

    private val biometricDialogCallback =
        object : BiometricDialog.Callback {
            override fun onPinComplete(pin: String) {
                this@BiometricBottomSheetDialogFragment.onPinComplete(pin)
            }

            override fun showPin() {
                setPinSecure(true)
                biometricLayout.showPin(false)
            }

            override fun onCancel() {
                context?.let {
                    biometricLayout.isBiometricTextVisible(BiometricUtil.shouldShowBiometric(it))
                }
            }
        }

    private val biometricLayoutCallback =
        object : BiometricLayout.Callback {
            override fun onPinComplete(pin: String) {
                this@BiometricBottomSheetDialogFragment.onPinComplete(pin)
            }

            override fun onShowBiometric() {
                showBiometricPrompt()
            }

            override fun onDismiss() {
                if (onClickBiometricLayoutClose()) return

                dismiss()
            }
        }

    private var callback: Callback? = null

    fun setCallback(cb: Callback) {
        callback = cb
    }

    // Keeping these callback methods can only be called at most once.
    open class Callback {
        open fun onDismiss(success: Boolean) {}
    }
}
