package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.core.content.ContextCompat
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
        const val ARGS_IS_HINT_RED = "args_is_hint_red"
        const val ARGS_SUBTITLE = "args_subtitle"

        fun newInstance(
            title: String? = null,
            disableBiometric: Boolean = false,
            isHintRed: Boolean = false,
            subtitle: String? = null,
        ) =
            VerifyBottomSheetDialogFragment().withArgs {
                title?.let { putString(ARGS_TITLE, it) }
                subtitle?.let { putString(ARGS_SUBTITLE, it) }
                putBoolean(ARGS_DISABLE_BIOMETRIC, disableBiometric)
                putBoolean(ARGS_IS_HINT_RED, isHintRed)
            }
    }

    private val isHintRed by lazy {
        requireArguments().getBoolean(ARGS_IS_HINT_RED, false)
    }

    private val binding by viewBinding(FragmentVerifyBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()

        val title = arguments?.getString(ARGS_TITLE)
        if (!title.isNullOrBlank()) {
            binding.title.text = title
            if (isHintRed) {
                binding.title.setTextColor(ContextCompat.getColor(requireContext(), R.color.wallet_pink))
            }
        }

        val subtitle = arguments?.getString(ARGS_SUBTITLE)
        if (!subtitle.isNullOrBlank()) {
            binding.subTitle.text = subtitle
            binding.subTitle.isVisible = true
        } else {
            binding.subTitle.isVisible = false
        }

        val disableBiometric = arguments?.getBoolean(ARGS_DISABLE_BIOMETRIC) ?: false
        val isHintRed = arguments?.getBoolean(ARGS_IS_HINT_RED) ?: false
        if (disableBiometric) {
            binding.biometricLayout.biometricTv.isVisible = false
        } else {
            binding.biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
        }
        binding.biometricLayout.measureAllChildren = false
        setCallback(
            object : Callback() {
                override fun onDismiss(success: Boolean) {
                    if (success) {
                        continueCallback?.invoke(this@VerifyBottomSheetDialogFragment)
                    }
                }
            },
        )
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return bottomViewModel.verifyPin(pin)
    }

    var disableToast = false

    override fun doWhenInvokeNetworkSuccess(
        response: MixinResponse<*>,
        pin: String,
    ): Boolean {
        onPinSuccess?.invoke(pin)
        if (disableToast) {
            dismiss()
            return false
        } else {
            return true
        }
    }

    override fun getBiometricInfo() =
        BiometricInfo(
            getString(R.string.Verify_by_Biometric),
            "",
            "",
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
