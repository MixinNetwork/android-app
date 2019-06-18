package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_pin_bottom_sheet.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.ui.common.PinBottomSheetDialogFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.widget.AndroidUtilities
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.PinView

class PinBiometricsBottomSheetDialogFragment : PinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PinBiometricsBottomSheetDialogFragment"
        const val FROM_WALLET_SETTING = "from_wallet_setting"

        fun newInstance(fromWalletSetting: Boolean) = PinBiometricsBottomSheetDialogFragment().apply {
            arguments = bundleOf(FROM_WALLET_SETTING to fromWalletSetting)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_pin_bottom_sheet, null)
        val tipTv = View.inflate(context, R.layout.view_pin_bottom_sheet_tip, null) as TextView
        tipTv.setText(getTipTextRes())
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            val dp16 = AndroidUtilities.dp(16f)
            topMargin = dp16
            bottomMargin = dp16
        }
        (contentView.pin_ll as ViewGroup).addView(tipTv, 2, lp)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    private val fromWalletSetting by lazy { arguments!!.getBoolean(FROM_WALLET_SETTING) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.pin.setListener(object : PinView.OnPinListener {
            override fun onUpdate(index: Int) {
                if (index == contentView.pin.getCount()) {
                    verify(contentView.pin.code())
                }
            }
        })
    }

    private fun verify(pinCode: String) = lifecycleScope.launch {
        contentView.pin_va?.displayedChild = POS_PB
        val response = try {
            withContext(Dispatchers.IO) {
                bottomViewModel.verifyPin(pinCode)
            }
        } catch (t: Throwable) {
            contentView.pin_va?.displayedChild = POS_PIN
            contentView.pin.clear()
            ErrorHandler.handleError(t)
            return@launch
        }
        contentView.pin_va?.displayedChild = POS_PIN
        contentView.pin.clear()
        if (response.isSuccess) {
            context?.updatePinCheck()
            response.data?.let {
                if (fromWalletSetting) {
                    val success = BiometricUtil.savePin(requireContext(), pinCode,
                        this@PinBiometricsBottomSheetDialogFragment)
                    if (success) callback?.onSuccess() else dismiss()
                } else {
                    callback?.onSuccess()
                }
            }
            dismiss()
        } else {
            ErrorHandler.handleMixinError(response.errorCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (fromWalletSetting && requestCode == BiometricUtil.REQUEST_CODE_CREDENTIALS && resultCode == Activity.RESULT_OK) {
            BiometricUtil.savePin(requireContext(), contentView.pin.code(), this@PinBiometricsBottomSheetDialogFragment)
        }
    }

    override fun getTipTextRes(): Int =
        if (fromWalletSetting) R.string.wallet_pin_open_biometrics else R.string.wallet_pin_modify_biometrics
}