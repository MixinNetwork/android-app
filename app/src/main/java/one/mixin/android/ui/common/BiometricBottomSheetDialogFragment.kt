package one.mixin.android.ui.common

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.*
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.util.BiometricUtil

abstract class BiometricBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val ARGS_BIOMETRIC_ITEM = "args_biometric_item"

        const val POS_PIN = 0
        const val POS_PB = 1
    }

    private val t: BiometricItem by lazy {
        arguments!!.getParcelable<BiometricItem>(ARGS_BIOMETRIC_ITEM)!!
    }

    private var biometricDialog: BiometricDialog<BiometricItem>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        contentView.biometric_tv.setOnClickListener { showBiometricPrompt() }
        contentView.biometric_tv.isVisible = BiometricUtil.shouldShowBiometric(requireContext())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BiometricUtil.REQUEST_CODE_CREDENTIALS && resultCode == Activity.RESULT_OK) {
            showBiometricPrompt()
        }
    }

    private fun showBiometricPrompt() {
        biometricDialog = BiometricDialog(requireContext(), t)
        biometricDialog?.callback = biometricDialogCallback
        biometricDialog?.show()
    }

    abstract fun onPinCorrect(pin: String)

    private val biometricDialogCallback = object : BiometricDialog.Callback<BiometricItem> {
        override fun onStartTransfer(pin: String) {
            onPinCorrect(pin)
        }

        override fun showTransferBottom() {
        }

        override fun showAuthenticationScreen() {
            BiometricUtil.showAuthenticationScreen(this@BiometricBottomSheetDialogFragment)
        }

        override fun onCancel() {
            contentView.biometric_tv?.isVisible = BiometricUtil.shouldShowBiometric(requireContext())
        }
    }

    var callback: Callback? = null

    interface Callback {
        fun onSuccess()
    }
}
