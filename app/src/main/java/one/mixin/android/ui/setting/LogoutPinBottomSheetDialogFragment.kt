package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.databinding.FragmentDeleteAccountPinBottomSheetBinding
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.ui.oldwallet.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.oldwallet.biometric.BiometricInfo
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class LogoutPinBottomSheetDialogFragment : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "LogoutPinBottomSheetDialogFragment"

        fun newInstance() =
            LogoutPinBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentDeleteAccountPinBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()

        binding.biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
        binding.biometricLayout.measureAllChildren = false
        binding.title.setText(R.string.Enter_your_PIN_to_log_out)

        binding.content.setText(R.string.logout_description)
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return bottomViewModel.verifyPin(pin)
    }

    override fun doWhenInvokeNetworkSuccess(
        response: MixinResponse<*>,
        pin: String,
    ): Boolean {
        MixinApplication.get().closeAndClear()
        return true
    }

    override fun getBiometricInfo() =
        BiometricInfo(
            getString(R.string.Enter_your_PIN_to_log_out),
            "",
            "",
        )
}
