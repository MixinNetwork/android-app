package one.mixin.android.ui.common.biometric

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ViewAnimator
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.layout_pin_biometric.view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.vibrate
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

class BiometricLayout(context: Context, attributeSet: AttributeSet) : ViewAnimator(context, attributeSet) {
    init {
        LayoutInflater.from(context).inflate(R.layout.layout_pin_biometric, this, true)
    }

    var callback: Callback? = null

    private var keyboardHeight = 0
    private var keyboard: Keyboard? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        biometric_tv.setOnClickListener { callback?.onShowBiometric() }
        biometric_tv.isVisible = BiometricUtil.shouldShowBiometric(context)

        pin.setListener(object : PinView.OnPinListener {
            override fun onUpdate(index: Int) {
                if (index == pin.getCount()) {
                    callback?.onPinComplete(pin.code())
                }
            }
        })
    }

    fun setKeyboard(keyboard: Keyboard) {
        this.keyboard = keyboard
        keyboard.setKeyboardKeys(Constants.KEYS)
        keyboard.setOnClickKeyboardListener(object : Keyboard.OnClickKeyboardListener {
            override fun onKeyClick(position: Int, value: String) {
                context?.vibrate(longArrayOf(0, 30))
                if (position == 11) {
                    pin.delete()
                } else {
                    pin.append(value)
                }
            }

            override fun onLongClick(position: Int, value: String) {
                context?.vibrate(longArrayOf(0, 30))
                if (position == 11) {
                    pin.clear()
                } else {
                    pin.append(value)
                }
            }
        })
    }

    fun showErrorInfo(content: String, animate: Boolean = false) {
        displayedChild = POS_ERROR
        error_info?.text = content
        keyboardHeight = keyboard?.height ?: 0
        if (animate) {
            keyboard?.animateHeight(keyboardHeight, 0)
        } else {
            keyboard?.isVisible = false
        }
    }

    fun showPin(clearPin: Boolean) {
        displayedChild = POS_PIN
        if (clearPin) {
            pin.clear()
        }
        keyboard?.animateHeight(0, keyboardHeight)
    }

    fun showPb() {
        displayedChild = POS_PB
    }

    fun isBiometricTextVisible(isVisible: Boolean) {
        biometric_tv?.isVisible = isVisible
    }

    fun setErrorButton(errorCode: Int) {
        when (getErrorAction(errorCode)) {
            ErrorAction.TryLater -> {
                error_btn.text = getString(R.string.group_ok)
                error_btn.setOnClickListener { callback?.onDismiss() }
            }
            ErrorAction.RetryPin -> {
                error_btn.text = getString(R.string.try_again)
                error_btn.setOnClickListener { showPin(true) }
            }
            ErrorAction.ChangeAmount -> {
                error_btn.text = getString(R.string.bottom_withdrawal_change_amount)
                error_btn.setOnClickListener { callback?.onDismiss() }
            }
            ErrorAction.Close -> {
                error_btn.text = getString(R.string.group_ok)
                error_btn.setOnClickListener { callback?.onDismiss() }
            }
        }
    }

    private fun getErrorAction(errorCode: Int): ErrorAction {
        return when (errorCode) {
            ErrorHandler.TOO_MANY_REQUEST -> {
                ErrorAction.TryLater
            }
            ErrorHandler.INVALID_PIN_FORMAT, ErrorHandler.PIN_INCORRECT -> {
                ErrorAction.RetryPin
            }
            ErrorHandler.INSUFFICIENT_BALANCE, ErrorHandler.TOO_SMALL -> {
                ErrorAction.ChangeAmount
            }
            else -> {
                ErrorAction.Close
            }
        }
    }

    private fun getString(resId: Int) = context.getString(resId)

    enum class ErrorAction {
        TryLater, RetryPin, ChangeAmount, Close
    }

    interface Callback {
        fun onPinComplete(pin: String)

        fun onShowBiometric()

        fun onDismiss()
    }

    companion object {
        const val POS_PIN = 0
        const val POS_PB = 1
        const val POS_ERROR = 2
    }
}
