package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_pin_bottom_sheet.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.ui.common.PinBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.PinView

class PinEmergencyBottomSheetDialog : PinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PinEmergencyBottomSheetDialog"

        fun newInstance() = PinEmergencyBottomSheetDialog()
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_pin_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.info_tv.setText(R.string.setting_emergency_pin_tip)
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
        handleMixinResponse(
            invokeNetwork = { bottomViewModel.verifyPin(pinCode) },
            switchContext = Dispatchers.IO,
            successBlock = {
                context?.updatePinCheck()
                pinEmergencyCallback?.onSuccess(pinCode)
                dismiss()
            },
            failureBlock = {
                if (it.errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                    toast(R.string.error_pin_check_too_many_request)
                    return@handleMixinResponse true
                }
                return@handleMixinResponse false
            },
            exceptionBlock = {
                contentView.pin_va?.displayedChild = POS_PIN
                contentView.pin.clear()
                return@handleMixinResponse false
            },
            doAfterNetworkSuccess = {
                contentView.pin_va?.displayedChild = POS_PIN
                contentView.pin.clear()
            }
        )
    }

    var pinEmergencyCallback: PinEmergencyCallback? = null

    abstract class PinEmergencyCallback : Callback {
        abstract fun onSuccess(pinCode: String)

        override fun onSuccess() {
        }
    }
}
