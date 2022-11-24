package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ARGS_TITLE
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.databinding.FragmentVerifyBottomSheetBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class VerifyBottomSheetDialogFragment : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "VerifyBottomSheetDialogFragment"
        const val ARGS_DISABLE_BIOMETRIC = "args_disable_biometric"

        fun newInstance(title: String? = null, disableBiometric: Boolean = false) = VerifyBottomSheetDialogFragment().withArgs {
            title?.let { putString(ARGS_TITLE, it) }
            putBoolean(ARGS_DISABLE_BIOMETRIC, disableBiometric)
        }
    }

    private val binding by viewBinding(FragmentVerifyBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()

        val title = arguments?.getString(ARGS_TITLE)
        if (!title.isNullOrBlank()) {
            binding.title.text = title
        }

        val disableBiometric = arguments?.getBoolean(ARGS_DISABLE_BIOMETRIC) ?: false
        if (disableBiometric) {
            binding.biometricLayout.biometricTv.isVisible = false
        } else {
            binding.biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
        }
        binding.biometricLayout.measureAllChildren = false
        setCallback(object : Callback() {
            override fun onDismiss(success: Boolean) {
                if (success) {
                    continueCallback?.invoke(this@VerifyBottomSheetDialogFragment)
                }
            }
        })
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return bottomViewModel.verifyPin(pin)
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        onPinSuccess?.invoke(pin)
        return true
    }

    override fun getBiometricInfo() = BiometricInfo(
        getString(R.string.Verify_by_Biometric),
        "",
        ""
    )

    fun setContinueCallback(callback: (DialogFragment) -> Unit): VerifyBottomSheetDialogFragment {
        continueCallback = callback
        return this
    }

    fun setOnPinSuccess(callback: (String) -> Unit): VerifyBottomSheetDialogFragment {
        onPinSuccess = callback
        return this
    }

    private var continueCallback: ((DialogFragment) -> Unit)? = null

    private var onPinSuccess: ((String) -> Unit)? = null
}
