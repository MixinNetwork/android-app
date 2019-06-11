package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModel
import kotlinx.android.synthetic.main.fragment_verification.*
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.extension.vibrate
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Account
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.VerificationCodeView

abstract class PinCodeFragment<VH : ViewModel> : BaseViewModelFragment<VH>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        back_iv.setOnClickListener { activity?.onBackPressed() }
        pin_verification_view.setOnCodeEnteredListener(mPinVerificationListener)
        verification_keyboard.setKeyboardKeys(KEYS)
        verification_keyboard.setOnClickKeyboardListener(mKeyboardListener)
        verification_cover.isClickable = true
        verification_next_fab.setOnClickListener { clickNextFab() }
    }

    protected fun handleFailure(r: MixinResponse<Account>) {
        pin_verification_view.error()
        pin_verification_tip_tv.visibility = View.VISIBLE
        pin_verification_tip_tv.text = getString(R.string.landing_validation_error)
        if (r.errorCode == ErrorHandler.PHONE_VERIFICATION_CODE_INVALID ||
            r.errorCode == ErrorHandler.PHONE_VERIFICATION_CODE_EXPIRED) {
            verification_next_fab.visibility = View.INVISIBLE
        }
        ErrorHandler.handleMixinError(r.errorCode)
    }

    protected fun handleError(t: Throwable) {
        verification_next_fab.hide()
        verification_cover.visibility = View.GONE
        ErrorHandler.handleError(t)
    }

    protected fun showLoading() {
        verification_next_fab.visibility = View.VISIBLE
        verification_next_fab.show()
        verification_cover.visibility = View.VISIBLE
    }

    protected open fun hideLoading() {
        verification_next_fab.hide()
        verification_next_fab.visibility = View.GONE
        verification_cover.visibility = View.GONE
    }

    abstract fun clickNextFab()

    private val mKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.vibrate(longArrayOf(0, 30))
            if (position == 11) {
                pin_verification_view?.delete()
            } else {
                pin_verification_view?.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.vibrate(longArrayOf(0, 30))
            if (position == 11) {
                pin_verification_view?.clear()
            } else {
                pin_verification_view?.append(value)
            }
        }
    }

    private val mPinVerificationListener = object : VerificationCodeView.OnCodeEnteredListener {
        override fun onCodeEntered(code: String) {
            pin_verification_tip_tv.visibility = View.INVISIBLE
            if (code.isEmpty() || code.length != pin_verification_view.count) {
                if (isAdded) {
                    hideLoading()
                }
                return
            }
            clickNextFab()
        }
    }
}