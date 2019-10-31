package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import java.math.BigDecimal
import kotlinx.android.synthetic.main.fragment_multisigs_bottom_sheet.view.*
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.asset_icon
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.balance
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.balance_as
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.biometric_tv
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.keyboard
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.pin
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.title_view
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
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
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Fiats
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

abstract class BiometricBottomSheetDialogFragment<T : BiometricItem> : MixinBottomSheetDialogFragment() {
    companion object {
        const val ARGS_BIOMETRIC_ITEM = "args_biometric_item"

        const val POS_PIN = 0
        const val POS_PB = 1
    }

    private var biometricDialog: BiometricDialog<BiometricItem>? = null

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
    }

    abstract fun getBiometricItem(): T

    abstract suspend fun invokeNetwork(pin: String): MixinResponse<Void>

    abstract fun doWhenInvokeNetworkSuccess()

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
                    if (it.errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                        toast(R.string.error_pin_check_too_many_request)
                    } else {
                        ErrorHandler.handleMixinError(it.errorCode, it.errorDescription)
                    }
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
}
