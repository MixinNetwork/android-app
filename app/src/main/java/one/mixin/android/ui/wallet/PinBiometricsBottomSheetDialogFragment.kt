package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.security.keystore.UserNotAuthenticatedException
import androidx.core.os.bundleOf
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.databinding.FragmentPinBottomSheetBinding
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricDialog
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import timber.log.Timber

@AndroidEntryPoint
class PinBiometricsBottomSheetDialogFragment : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PinBiometricsBottomSheetDialogFragment"
        const val FROM_WALLET_SETTING = "from_wallet_setting"

        fun newInstance(fromWalletSetting: Boolean) =
            PinBiometricsBottomSheetDialogFragment().apply {
                arguments = bundleOf(FROM_WALLET_SETTING to fromWalletSetting)
            }
    }

    private val fromWalletSetting by lazy { requireArguments().getBoolean(FROM_WALLET_SETTING) }

    private val binding by viewBinding(FragmentPinBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()

        binding.title.setText(getTipTextRes())
        binding.biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return bottomViewModel.verifyPin(pin)
    }

    override fun doWhenInvokeNetworkSuccess(
        response: MixinResponse<*>,
        pin: String,
    ): Boolean {
        if (fromWalletSetting) {
            return try {
                val success = BiometricUtil.savePin(requireContext(), pin)
                if (success) {
                    onSavePinSuccess?.invoke()
                    return true
                } else {
                    dismiss()
                    return false
                }
            } catch (e: UserNotAuthenticatedException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // https://issuetracker.google.com/issues/301069939
                    BiometricUtil.deleteKey(requireContext())
                }

                val biometricDialog =
                    BiometricDialog(
                        requireActivity(),
                        getBiometricInfo(),
                        onlyVerify = true,
                    )
                biometricDialog.callback =
                    object : BiometricDialog.Callback {
                        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
                        override fun onPinComplete(empty: String) {
                            val success =
                                try {
                                    BiometricUtil.savePin(requireContext(), pin)
                                } catch (e: UserNotAuthenticatedException) {
                                    Timber.e("$TAG savePin meet UserNotAuthenticatedException")
                                    dismiss()
                                    return
                                }
                            if (success) {
                                onSavePinSuccess?.invoke()
                            }
                            dismiss()
                        }

                        override fun showPin() {
                            dismiss()
                        }

                        override fun onCancel() {
                            dismiss()
                        }
                    }
                biometricDialog.show()
                false
            }
        }
        return true
    }

    override fun getBiometricInfo() =
        BiometricInfo(
            getString(R.string.Verify_by_Biometric),
            "",
            "",
        )

    private fun getTipTextRes(): Int =
        if (fromWalletSetting) R.string.wallet_pin_open_biometrics else R.string.wallet_pin_modify_biometrics

    var onSavePinSuccess: (() -> Unit)? = null
}
