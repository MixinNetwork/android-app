package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.databinding.FragmentVerifyBottomSheetBinding
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class VerifyBottomSheetDialogFragment : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "VerifyBottomSheetDialogFragment"
        fun newInstance() =
            VerifyBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentVerifyBottomSheetBinding::inflate)

    init {
        autoDismiss = false
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()

        binding.biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
        binding.biometricLayout.measureAllChildren = false
        callback = object : BiometricBottomSheetDialogFragment.Callback() {
            override fun onSuccess() {
                continueCallback?.invoke(this@VerifyBottomSheetDialogFragment)
            }
        }
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return bottomViewModel.verifyPin(pin)
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        return true
    }

    override fun getBiometricInfo() = BiometricInfo(
        getString(R.string.Verify_by_Biometric),
        "",
        "",
        getString(R.string.Verify_with_PIN)
    )

    fun setContinueCallback(callback: (DialogFragment) -> Unit): VerifyBottomSheetDialogFragment {
        continueCallback = callback
        return this
    }

    private var continueCallback: ((DialogFragment) -> Unit)? = null
}
