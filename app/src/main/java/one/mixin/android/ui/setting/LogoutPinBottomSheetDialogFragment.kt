package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.databinding.FragmentDeleteAccountPinBottomSheetBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.oldwallet.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.oldwallet.biometric.BiometricInfo
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class LogoutPinBottomSheetDialogFragment : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "LogoutPinBottomSheetDialogFragment"
        const val ARGS_SESSION = "agrs_session"

        fun newInstance(sessionId: String? =null) =
            LogoutPinBottomSheetDialogFragment().withArgs {
                putString(ARGS_SESSION, sessionId)
            }
    }

    private val binding by viewBinding(FragmentDeleteAccountPinBottomSheetBinding::inflate)
    private val sessionId by lazy {
        requireArguments().getString(ARGS_SESSION, null)
    }

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
        if (sessionId == null) {
            binding.content.setText(R.string.logout_description)
        } else {
            binding.content.text = ""
        }
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return if (sessionId != null) {
            bottomViewModel.logout(sessionId, pin)
        } else {
            bottomViewModel.verifyPin(pin)
        }
    }

    private var isSuccess = false
    override fun doWhenInvokeNetworkSuccess(
        response: MixinResponse<*>,
        pin: String,
    ): Boolean {
        if (sessionId == null) {
            MixinApplication.get().closeAndClear()
        }
        isSuccess = response.isSuccess
        return true
    }

    override fun getBiometricInfo() =
        BiometricInfo(
            getString(R.string.Enter_your_PIN_to_log_out),
            "",
            "",
        )

    override fun onDestroy() {
        super.onDestroy()
        onSuccess?.invoke(isSuccess)
    }

    private var onSuccess:((Boolean)-> Unit)? = null

    fun setOnSuccess(onSuccess:(Boolean)-> Unit){
        this.onSuccess = onSuccess
    }
}
