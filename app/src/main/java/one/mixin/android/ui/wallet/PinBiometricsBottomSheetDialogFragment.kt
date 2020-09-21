package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import androidx.core.os.bundleOf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_pin_bottom_sheet.view.*
import kotlinx.android.synthetic.main.layout_pin_biometric.view.*
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.BiometricUtil
import one.mixin.android.widget.BottomSheet

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

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_pin_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()

        contentView.title.setText(getTipTextRes())
        contentView.biometric_tv.setText(R.string.verify_by_biometric)
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return bottomViewModel.verifyPin(pin)
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        response.data?.let {
            if (fromWalletSetting) {
                val success = BiometricUtil.savePin(
                    requireContext(),
                    pin,
                    this@PinBiometricsBottomSheetDialogFragment
                )
                if (success) callback?.onSuccess() else dismiss()
            } else {
                callback?.onSuccess()
            }
        }
        return true
    }

    override fun getBiometricInfo() = BiometricInfo(
        getString(R.string.verify_by_biometric),
        "",
        "",
        getString(R.string.verify_by_PIN)
    )

    private fun getTipTextRes(): Int =
        if (fromWalletSetting) R.string.wallet_pin_open_biometrics else R.string.wallet_pin_modify_biometrics
}
