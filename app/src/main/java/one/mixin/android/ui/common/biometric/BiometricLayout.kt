package one.mixin.android.ui.common.biometric

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ViewAnimator
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.LayoutPinBiometricBinding
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.textColor
import one.mixin.android.extension.tickVibrate
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

class BiometricLayout(context: Context, attributeSet: AttributeSet) : ViewAnimator(context, attributeSet) {
    private val binding: LayoutPinBiometricBinding = LayoutPinBiometricBinding.inflate(LayoutInflater.from(context), this)
    val biometricTv get() = binding.biometricTv
    val payTv get() = binding.payTv
    val pin get() = binding.pin
    val errorBtn get() = binding.errorBtn
    val enableBiometricTv get() = binding.enableBiometricTv

    var callback: Callback? = null

    var keyboardHeight = 0
    private var keyboard: Keyboard? = null

    init {
        measureAllChildren = false
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding.apply {
            biometricTv.setOnClickListener { callback?.onShowBiometric() }
            biometricTv.isVisible = BiometricUtil.shouldShowBiometric(context)

            pin.setListener(
                object : PinView.OnPinListener {
                    override fun onUpdate(index: Int) {
                        if (index == pin.getCount()) {
                            callback?.onPinComplete(pin.code())
                        }
                    }
                }
            )
        }
    }

    fun setKeyboard(keyboard: Keyboard) {
        this.keyboard = keyboard
        keyboard.setKeyboardKeys(Constants.KEYS)
        keyboard.setOnClickKeyboardListener(
            object : Keyboard.OnClickKeyboardListener {
                override fun onKeyClick(position: Int, value: String) {
                    context?.tickVibrate()
                    if (position == 11) {
                        binding.pin.delete()
                    } else {
                        binding.pin.append(value)
                    }
                }

                override fun onLongClick(position: Int, value: String) {
                    context?.clickVibrate()
                    if (position == 11) {
                        binding.pin.clear()
                    } else {
                        binding.pin.append(value)
                    }
                }
            }
        )
    }

    fun showErrorInfo(
        content: String,
        animate: Boolean = false,
        tickMillis: Long = 0L,
        errorAction: ErrorAction? = null
    ) {
        displayedChild = POS_ERROR
        binding.errorInfo.text = content
        val dp32 = context.dpToPx(32f)
        binding.errorBtn.updateLayoutParams<MarginLayoutParams> {
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
        errorAction?.let { setErrorButton(it) }
    }

    fun showPin(clearPin: Boolean) {
        displayedChild = POS_PIN
        if (clearPin) {
            binding.pin.clear()
        }
        binding.errorBtn.updateLayoutParams<MarginLayoutParams> {
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

    fun showDone(): Long {
        displayedChild = POS_DONE

        val open = context.defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
        val enable = !open && BiometricUtil.isSupport(context)
        enableBiometricTv.isVisible = enable
        enableBiometricTv.setOnClickListener {
            SettingActivity.showPinSetting(context)
        }

        keyboard?.animateHeight(keyboardHeight, 0)

        return if (enable) 4000L else 3000L
    }

    fun showPb() {
        displayedChild = POS_PB
    }

    fun isBiometricTextVisible(isVisible: Boolean) {
        binding.biometricTv.isVisible = isVisible
    }

    fun setErrorButton(
        errorAction: ErrorAction
    ) {
        binding.apply {
            when (errorAction) {
                ErrorAction.RetryPin -> {
                    errorBtn.text = getString(R.string.try_again)
                    errorBtn.setOnClickListener { showPin(true) }
                }
                ErrorAction.Close -> {
                    errorBtn.text = getString(R.string.capital_ok)
                    errorBtn.setOnClickListener { callback?.onDismiss() }
                }
                ErrorAction.Continue -> {
                    errorBtn.text = getString(R.string.wallet_transaction_continue)
                    errorBtn.setOnClickListener { showPin(true) }
                }
            }
        }
    }

    fun getErrorActionByErrorCode(errorCode: Int): ErrorAction {
        return when (errorCode) {
            ErrorHandler.INVALID_PIN_FORMAT, ErrorHandler.PIN_INCORRECT -> {
                ErrorAction.RetryPin
            }
            ErrorHandler.WITHDRAWAL_FEE_TOO_SMALL -> {
                ErrorAction.Continue
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
        binding.apply {
            countDownTimer?.cancel()
            errorBtn.isEnabled = false
            errorBtn.textColor = context.getColor(R.color.wallet_text_gray)
            countDownTimer = object : CountDownTimer(tickMillis, 1000) {

                override fun onTick(l: Long) {
                    errorBtn.text =
                        context.getString(R.string.wallet_transaction_continue_count_down, l / 1000)
                }

                override fun onFinish() {
                    errorBtn.text = getString(R.string.wallet_transaction_continue)
                    errorBtn.isEnabled = true
                    errorBtn.textColor = context.getColor(R.color.white)
                }
            }
            countDownTimer?.start()
        }
    }

    private fun getString(resId: Int) = context.getString(resId)

    enum class ErrorAction {
        RetryPin, Close, Continue
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
