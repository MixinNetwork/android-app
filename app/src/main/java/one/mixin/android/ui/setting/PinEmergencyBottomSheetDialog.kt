package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_pin_bottom_sheet.view.*
import kotlinx.android.synthetic.main.layout_pin_biometric.view.*
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class PinEmergencyBottomSheetDialog : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PinEmergencyBottomSheetDialog"

        fun newInstance() = PinEmergencyBottomSheetDialog()
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_pin_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()

        contentView.title.setText(R.string.setting_emergency_pin_tip)
        contentView.biometric_tv.setText(R.string.verify_by_biometric)
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        pinEmergencyCallback?.onSuccess(pin)
        return true
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return bottomViewModel.verifyPin(pin)
    }

    override fun getBiometricInfo() =
        BiometricInfo(getString(R.string.verify_by_biometric), "", "", getString(R.string.verify_by_PIN))

    var pinEmergencyCallback: PinEmergencyCallback? = null

    abstract class PinEmergencyCallback : Callback {
        abstract fun onSuccess(pinCode: String)

        override fun onSuccess() {
        }
    }
}
