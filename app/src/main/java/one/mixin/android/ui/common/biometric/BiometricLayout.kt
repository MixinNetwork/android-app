package one.mixin.android.ui.common.biometric

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ViewAnimator
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import kotlinx.android.synthetic.main.layout_pin_biometric.view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.vibrate
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView
import org.jetbrains.anko.textColor

class BiometricLayout(context: Context, attributeSet: AttributeSet) : ViewAnimator(context, attributeSet) {
    init {
        LayoutInflater.from(context).inflate(R.layout.layout_pin_biometric, this, true)
    }

    var callback: Callback? = null

    var keyboardHeight = 0
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

    fun showErrorInfo(
        content: String,
        animate: Boolean = false,
        tickMillis: Long = 0L,
        errorAction: ErrorAction? = null,
        clickCallback: (() -> Unit)? = null
    ) {
        displayedChild = POS_ERROR
        error_info?.text = content
        val dp32 = context.dpToPx(32f)
        error_btn?.updateLayoutParams<MarginLayoutParams> {
            bottomMargin = dp32
        }
        if (animate) {
            keyboard?.animateHeight(keyboardHeight, 0)
        } else {
            keyboard?.isVisible = false
        }
        if (tickMillis > 0) {
            startCountDown(tickMillis)
        }
        errorAction?.let { setErrorButton(it, clickCallback) }
    }

    fun showPin(clearPin: Boolean) {
        displayedChild = POS_PIN
        if (clearPin) {
            pin?.clear()
        }
        error_btn?.updateLayoutParams<MarginLayoutParams> {
            bottomMargin = 0
        }
        keyboard?.isVisible = true
        keyboard?.animateHeight(
            0,
            if (keyboardHeight == 0) keyboard?.measuredHeight ?: 0 else keyboardHeight,
            onEndAction = {
                if (keyboardHeight == 0) {
                    keyboardHeight = keyboard?.measuredHeight ?: 0
                }
            }
        )
    }

    fun showDone() {
        displayedChild = POS_DONE
        keyboard?.animateHeight(keyboardHeight, 0)
    }

    fun showPb() {
        displayedChild = POS_PB
    }

    fun isBiometricTextVisible(isVisible: Boolean) {
        biometric_tv?.isVisible = isVisible
    }

    fun setErrorButton(
        errorAction: ErrorAction,
        clickCallback: (() -> Unit)? = null
    ) {
        when (errorAction) {
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
            ErrorAction.LargeAmount -> {
                error_btn.text = getString(R.string.common_continue)
                error_btn.setOnClickListener {
                    clickCallback?.invoke()
                }
            }
        ErrorAction.RecentPaid -> {
                error_btn.text = getString(R.string.common_continue)
                error_btn.setOnClickListener {
                    showPin(true)
                    clickCallback?.invoke()
                }
            }
            ErrorAction.Close -> {
                error_btn.text = getString(R.string.group_ok)
                error_btn.setOnClickListener { callback?.onDismiss() }
            }
        }
    }

    fun getErrorActionByErrorCode(errorCode: Int): ErrorAction {
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        countDownTimer?.cancel()
    }

    private var countDownTimer: CountDownTimer? = null

    private fun startCountDown(tickMillis: Long) {
        countDownTimer?.cancel()
        error_btn.isEnabled = false
        error_btn.textColor = context.getColor(R.color.wallet_text_gray)
        countDownTimer = object : CountDownTimer(tickMillis, 1000) {

            override fun onTick(l: Long) {
                error_btn.text =
                    context.getString(R.string.wallet_transaction_continue_count, l / 1000)
            }

            override fun onFinish() {
                error_btn.text = getString(R.string.wallet_transaction_continue)
                error_btn.isEnabled = true
                error_btn.textColor = context.getColor(R.color.white)
            }
        }
        countDownTimer?.start()
    }

    private fun getString(resId: Int) = context.getString(resId)

    enum class ErrorAction {
        TryLater, RetryPin, ChangeAmount, LargeAmount, Close, RecentPaid
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
        const val POS_DONE = 3
    }
}
