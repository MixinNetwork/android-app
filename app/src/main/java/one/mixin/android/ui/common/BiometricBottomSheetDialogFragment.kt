package one.mixin.android.ui.common

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.*
import kotlinx.android.synthetic.main.layout_pin_biometric.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.putLong
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.ui.common.biometric.BiometricDialog
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricLayout
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.getMixinErrorStringByCode

abstract class BiometricBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    private var biometricDialog: BiometricDialog? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_view.right_iv.setOnClickListener { dismiss() }
        contentView.biometric_layout.setKeyboard(contentView.keyboard)
        contentView.biometric_layout.callback = biometricLayoutCallback
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BiometricUtil.REQUEST_CODE_CREDENTIALS && resultCode == Activity.RESULT_OK) {
            showBiometricPrompt()
        }
    }

    abstract fun getBiometricInfo(): BiometricInfo

    abstract suspend fun invokeNetwork(pin: String): MixinResponse<Void>

    abstract fun doWhenInvokeNetworkSuccess()

    protected fun showErrorInfo(content: String, animate: Boolean = false) {
        if (!isAdded) return
        contentView.biometric_layout.showErrorInfo(content, animate)
    }

    private fun showBiometricPrompt() {
        biometricDialog = BiometricDialog(
            requireContext(),
            getBiometricInfo()
        )
        biometricDialog?.callback = biometricDialogCallback
        biometricDialog?.show()
    }

    private fun showPin() {
        if (!isAdded) return
        contentView.biometric_layout.showPin(true)
    }

    private fun onPinComplete(pin: String) {
        lifecycleScope.launch {
            if (!isAdded) return@launch

            contentView.biometric_layout.showPb()
            handleMixinResponse(
                invokeNetwork = {
                    invokeNetwork(pin)
                },
                switchContext = Dispatchers.IO,
                successBlock = {
                    defaultSharedPreferences.putLong(
                        Constants.BIOMETRIC_PIN_CHECK,
                        System.currentTimeMillis()
                    )
                    context?.updatePinCheck()

                    doWhenInvokeNetworkSuccess()

                    dismiss()
                    callback.notNullWithElse({ action -> action.onSuccess() }, {
                        toast(R.string.successful)
                    })
                },
                doAfterNetworkSuccess = {
                    contentView.biometric_layout?.pin?.clear()
                },
                failureBlock = {
                    contentView.biometric_layout?.let { layout ->
                        layout.setErrorButton(it.errorCode)
                        layout.showPin(true)
                    }
                    showErrorInfo(requireContext().getMixinErrorStringByCode(it.errorCode, it.errorDescription), true)
                    return@handleMixinResponse true
                },
                exceptionBlock = {
                    contentView.biometric_layout?.showPin(true)
                    return@handleMixinResponse false
                }
            )
        }
    }

    private val biometricDialogCallback = object : BiometricDialog.Callback {
        override fun onPinComplete(pin: String) {
            this@BiometricBottomSheetDialogFragment.onPinComplete(pin)
        }

        override fun showPin() {
            contentView.biometric_layout?.showPin(false)
        }

        override fun showAuthenticationScreen() {
            BiometricUtil.showAuthenticationScreen(this@BiometricBottomSheetDialogFragment)
        }

        override fun onCancel() {
            contentView.biometric_layout?.isBiometricTextVisible(BiometricUtil.shouldShowBiometric(requireContext()))
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
