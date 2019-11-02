package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import java.math.BigDecimal
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.asset_icon
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.balance
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.balance_as
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.keyboard
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.title_view
import kotlinx.android.synthetic.main.layout_pin_pb_error.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.putLong
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.vibrate
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler.Companion.INSUFFICIENT_BALANCE
import one.mixin.android.util.ErrorHandler.Companion.INVALID_PIN_FORMAT
import one.mixin.android.util.ErrorHandler.Companion.PIN_INCORRECT
import one.mixin.android.util.ErrorHandler.Companion.TOO_MANY_REQUEST
import one.mixin.android.util.ErrorHandler.Companion.TOO_SMALL
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.Fiats
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

abstract class BiometricBottomSheetDialogFragment<T : BiometricItem> : MixinBottomSheetDialogFragment() {
    companion object {
        const val ARGS_BIOMETRIC_ITEM = "args_biometric_item"

        const val POS_PIN = 0
        const val POS_PB = 1
        const val POS_ERROR = 2
    }

    private var biometricDialog: BiometricDialog<BiometricItem>? = null

    private var keyboardHeight = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BiometricUtil.REQUEST_CODE_CREDENTIALS && resultCode == Activity.RESULT_OK) {
            showBiometricPrompt()
        }
    }

    private fun showBiometricPrompt() {
        biometricDialog = BiometricDialog(requireContext(), getBiometricItem())
        biometricDialog?.callback = biometricDialogCallback
        biometricDialog?.show()
    }

    @SuppressLint("SetTextI18n")
    private fun init() {
        contentView.title_view.right_iv.setOnClickListener { dismiss() }

        contentView.biometric_tv.setOnClickListener { showBiometricPrompt() }
        contentView.biometric_tv.isVisible = BiometricUtil.shouldShowBiometric(requireContext())

        contentView.pin.setListener(object : PinView.OnPinListener {
            override fun onUpdate(index: Int) {
                if (index == contentView.pin.getCount()) {
                    onPinCorrect(contentView.pin.code())
                }
            }
        })

        val t = getBiometricItem()

        contentView.asset_icon.bg.loadImage(t.asset.iconUrl, R.drawable.ic_avatar_place_holder)
        contentView.asset_icon.badge.loadImage(t.asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        contentView.balance.text = t.amount.numberFormat() + " " + t.asset.symbol
        contentView.balance_as.text = "≈ ${(BigDecimal(t.amount) *
            t.asset.priceFiat()).numberFormat2()} ${Fiats.currency}"

        contentView.keyboard.setKeyboardKeys(KEYS)
        contentView.keyboard.setOnClickKeyboardListener(object : Keyboard.OnClickKeyboardListener {
            override fun onKeyClick(position: Int, value: String) {
                context?.vibrate(longArrayOf(0, 30))
                if (position == 11) {
                    contentView.pin.delete()
                } else {
                    contentView.pin.append(value)
                }
            }

            override fun onLongClick(position: Int, value: String) {
                context?.vibrate(longArrayOf(0, 30))
                if (position == 11) {
                    contentView.pin.clear()
                } else {
                    contentView.pin.append(value)
                }
            }
        })

        checkState(t.state)
    }

    abstract fun checkState(state: String)

    abstract fun getBiometricItem(): T

    abstract suspend fun invokeNetwork(pin: String): MixinResponse<Void>

    abstract fun doWhenInvokeNetworkSuccess()

    protected fun showErrorInfo(content: String, animate: Boolean = false) {
        if (!isAdded) return

        contentView.pin_va?.displayedChild = POS_ERROR
        contentView.error_info?.text = content
        keyboardHeight = contentView.keyboard.height
        if (animate) {
            contentView.keyboard?.animateHeight(keyboardHeight, 0)
        } else {
            contentView.keyboard?.isVisible = false
        }
    }

    private fun showPin() {
        if (!isAdded) return

        contentView.pin_va?.displayedChild = POS_PIN
        contentView.keyboard?.animateHeight(0, keyboardHeight)
    }

    private fun onPinCorrect(pin: String) {
        lifecycleScope.launch {
            if (!isAdded) return@launch

            contentView.pin_va?.displayedChild = POS_PB
            handleMixinResponse(
                invokeNetwork = {
                    invokeNetwork(pin)
                },
                switchContext = Dispatchers.IO,
                successBlock = {
                    defaultSharedPreferences.putLong(
                        Constants.BIOMETRIC_PIN_CHECK,
                        System.currentTimeMillis()
                    )
                    context?.updatePinCheck()

                    doWhenInvokeNetworkSuccess()

                    dismiss()
                    callback.notNullWithElse({ action -> action.onSuccess() }, {
                        toast(R.string.successful)
                    })
                },
                doAfterNetworkSuccess = {
                    contentView.pin_va?.displayedChild = POS_PIN
                },
                failureBlock = {
                    contentView.pin?.clear()
                    val errorAction = getErrorAction(it.errorCode)
                    setErrorButton(errorAction)
                    showErrorInfo(requireContext().getMixinErrorStringByCode(it.errorCode, it.errorDescription), true)
                    return@handleMixinResponse true
                },
                exceptionBlock = {
                    contentView.pin?.clear()
                    contentView.pin_va?.displayedChild = POS_PIN
                    return@handleMixinResponse false
                }
            )
        }
    }

    private fun getErrorAction(errorCode: Int): ErrorAction {
        return when (errorCode) {
            TOO_MANY_REQUEST -> {
                ErrorAction.TryLater
            }
            INVALID_PIN_FORMAT, PIN_INCORRECT -> {
                ErrorAction.RetryPin
            }
            INSUFFICIENT_BALANCE, TOO_SMALL -> {
                ErrorAction.ChangeAmount
            }
            else -> {
                ErrorAction.Close
            }
        }
    }

    private fun setErrorButton(errorAction: ErrorAction) {
        when (errorAction) {
            ErrorAction.TryLater -> {
                contentView.error_btn.text = getString(R.string.group_ok)
                contentView.error_btn.setOnClickListener { dismiss() }
            }
            ErrorAction.RetryPin -> {
                contentView.error_btn.text = getString(R.string.try_again)
                contentView.error_btn.setOnClickListener { showPin() }
            }
            ErrorAction.ChangeAmount -> {
                contentView.error_btn.text = getString(R.string.bottom_withdrawal_change_amount)
                contentView.error_btn.setOnClickListener { dismiss() }
            }
            ErrorAction.Close -> {
                contentView.error_btn.text = getString(R.string.group_ok)
                contentView.error_btn.setOnClickListener { dismiss() }
            }
        }
    }

    private val biometricDialogCallback = object : BiometricDialog.Callback<BiometricItem> {
        override fun onStartTransfer(pin: String) {
            onPinCorrect(pin)
        }

        override fun showTransferBottom() {
        }

        override fun showAuthenticationScreen() {
            BiometricUtil.showAuthenticationScreen(this@BiometricBottomSheetDialogFragment)
        }

        override fun onCancel() {
            contentView.biometric_tv?.isVisible = BiometricUtil.shouldShowBiometric(requireContext())
        }
    }

    var callback: Callback? = null

    interface Callback {
        fun onSuccess()
    }

    enum class ErrorAction {
        TryLater, RetryPin, ChangeAmount, Close
    }
}
