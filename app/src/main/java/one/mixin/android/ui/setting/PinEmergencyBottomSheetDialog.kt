package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.databinding.FragmentPinBottomSheetBinding
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class PinEmergencyBottomSheetDialog : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PinEmergencyBottomSheetDialog"

        fun newInstance() = PinEmergencyBottomSheetDialog()
    }

    private val binding by viewBinding(FragmentPinBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()

        binding.apply {
            title.setText(R.string.setting_emergency_pin_tip)
            biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
        }
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        pinEmergencyCallback?.onSuccess(pin)
        return true
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return bottomViewModel.verifyPin(pin)
    }

    override fun getBiometricInfo() =
        BiometricInfo(getString(R.string.Verify_by_Biometric), "", "", getString(R.string.Verify_with_PIN))

    var pinEmergencyCallback: PinEmergencyCallback? = null

    abstract class PinEmergencyCallback : Callback() {
        abstract fun onSuccess(pinCode: String)

        override fun onSuccess() {
        }
    }
}
