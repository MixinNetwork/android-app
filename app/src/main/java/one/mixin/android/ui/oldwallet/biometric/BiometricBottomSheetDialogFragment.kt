package one.mixin.android.ui.oldwallet.biometric

import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
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
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.tip.exception.TipCounterNotSyncedException
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.tip.isTipNodeException
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.oldwallet.MixinBottomSheetDialogFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.RoundTitleView
import javax.inject.Inject

abstract class BiometricBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    private var biometricDialog: BiometricDialog? = null

    @Inject
    lateinit var pinCipher: PinCipher

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheet {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private var isSuccess = false

    protected fun showDone(returnTo: String? = null, from:Int = LinkBottomSheetDialogFragment.FROM_INTERNAL) {
        if (!isAdded) return
        biometricLayout.showDone(returnTo, from) {
            isSuccess = true
            dismiss()
            dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
                    toast(R.string.Successful)
                }
            } else {
                handleWithErrorCodeAndDesc(pin, requireNotNull(response.error))
            }
            dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
                dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                biometricLayout.showPin(true)
            }
            else -> {
                dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
                dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
