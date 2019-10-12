package one.mixin.android.ui.conversation.tansfer

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.VISIBLE
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.BIOMETRIC_PIN_CHECK
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.putLong
import one.mixin.android.extension.putStringSet
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.vibrate
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BiometricDialog
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Fiats
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView
import org.jetbrains.anko.support.v4.toast
import java.math.BigDecimal

class TransferBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "TransferBottomSheetDialogFragment"

        const val ARGS_BIOMETRIC_ITEM = "args_biometric_item"

        const val POS_PIN = 0
        const val POS_PB = 1

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            TransferBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_BIOMETRIC_ITEM, t)
            }
    }

    private val t: BiometricItem by lazy { arguments!!.getParcelable<BiometricItem>(ARGS_BIOMETRIC_ITEM)!! }

    private var biometricDialog: BiometricDialog<BiometricItem>? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_transfer_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_view.right_iv.setOnClickListener { dismiss() }
        contentView.title_view.roundClose()
        when (t) {
            is TransferBiometricItem -> {
                (t as TransferBiometricItem).let {
                    contentView.title.text = it.user.fullName ?: ""
                    contentView.sub_title.text = "MixinID:${it.user.identityNumber}"
                }
                contentView.pay_tv.setText(R.string.wallet_pay_with_pwd)
            }
            is WithdrawBiometricItem -> {
                (t as WithdrawBiometricItem).let {
                    contentView.title.text = getString(R.string.withdrawal_to, it.label)
                    contentView.sub_title.text = it.destination
                }
                contentView.pay_tv.setText(R.string.withdrawal_with_pwd)
            }
        }
        if (!TextUtils.isEmpty(t.tag)) {
            contentView.memo.visibility = VISIBLE
            contentView.memo.text = t.tag
        }
        contentView.asset_icon.bg.loadImage(t.asset.iconUrl, R.drawable.ic_avatar_place_holder)
        lifecycleScope.launch(Dispatchers.IO) {
            if (!isAdded) return@launch

            bottomViewModel.simpleAssetItem(t.asset.assetId)?.let {
                withContext(Dispatchers.Main) {
                    contentView.asset_icon.badge.loadImage(it.chainIconUrl, R.drawable.ic_avatar_place_holder)
                }
            }
        }
        contentView.balance.text = t.amount.numberFormat() + " " + t.asset.symbol
        contentView.balance_as.text = "≈ ${(BigDecimal(t.amount) * t.asset.priceFiat()).numberFormat2()} ${Fiats.currency}"
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
        contentView.pin.setListener(object : PinView.OnPinListener {
            override fun onUpdate(index: Int) {
                if (index == contentView.pin.getCount()) {
                    startTransfer(contentView.pin.code())
                }
            }
        })
        contentView.biometric_tv.setOnClickListener { showBiometricPrompt() }
        contentView.biometric_tv.isVisible = BiometricUtil.shouldShowBiometric(requireContext())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BiometricUtil.REQUEST_CODE_CREDENTIALS && resultCode == Activity.RESULT_OK) {
            showBiometricPrompt()
        }
    }

    private fun startTransfer(pin: String) {
        contentView.pin_va?.displayedChild = POS_PB
        when (t) {
            is TransferBiometricItem ->
                (t as TransferBiometricItem).let {
                    bottomViewModel.transfer(t.asset.assetId, it.user.userId, t.amount, pin, t.trace, t.tag)
                }
            else ->
                (t as WithdrawBiometricItem).let {
                    bottomViewModel.withdrawal(it.addressId, it.amount, pin, it.trace!!, it.tag)
                }
        }.autoDispose(stopScope)
            .subscribe({
                contentView.pin_va?.displayedChild = POS_PIN
                if (it.isSuccess) {
                    defaultSharedPreferences.putLong(BIOMETRIC_PIN_CHECK, System.currentTimeMillis())
                    context?.updatePinCheck()

                    if (t is WithdrawBiometricItem) {
                        updateFirstWithdrawalSet(t as WithdrawBiometricItem)
                    }

                    dismiss()
                    callback.notNullWithElse({ action -> action.onSuccess() }, {
                        toast(R.string.successful)
                    })
                } else {
                    contentView.pin?.clear()
                    if (it.errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                        toast(R.string.error_pin_check_too_many_request)
                    } else {
                        ErrorHandler.handleMixinError(it.errorCode, it.errorDescription)
                    }
                }
            }, {
                ErrorHandler.handleError(it)
                contentView.pin?.clear()
                contentView.pin_va?.displayedChild = POS_PIN
            })
    }

    private fun updateFirstWithdrawalSet(item: WithdrawBiometricItem) {
        var firsSet = defaultSharedPreferences.getStringSet(Constants.Account.PREF_HAS_WITHDRAWAL_ADDRESS_SET, null)
        if (firsSet == null) {
            firsSet = setOf(item.addressId)
        } else {
            firsSet.add(item.addressId)
        }
        defaultSharedPreferences.putStringSet(Constants.Account.PREF_HAS_WITHDRAWAL_ADDRESS_SET, firsSet)
    }

    private fun showBiometricPrompt() {
        biometricDialog = BiometricDialog(requireContext(), t)
        biometricDialog?.callback = biometricDialogCallback
        biometricDialog?.show()
    }

    private val biometricDialogCallback = object : BiometricDialog.Callback<BiometricItem> {
        override fun onStartTransfer(t: BiometricItem) {
            startTransfer(t.pin!!)
        }

        override fun showTransferBottom(t: BiometricItem) {
        }

        override fun showAuthenticationScreen() {
            BiometricUtil.showAuthenticationScreen(this@TransferBottomSheetDialogFragment)
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
