package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.CountDownTimer
import android.view.View
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_precondition_bottom_sheet.view.*
import kotlinx.android.synthetic.main.fragment_precondition_bottom_sheet.view.asset_balance
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_DUPLICATE_TRANSFER
import one.mixin.android.R
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getRelativeTimeSpan
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.ValuableBiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.displayAddress
import one.mixin.android.vo.Fiats
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.textColor
import java.math.BigDecimal

@AndroidEntryPoint
class PreconditionBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PreconditionBottomSheetDialogFragment"

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            PreconditionBottomSheetDialogFragment().withArgs {
                putParcelable(ValuableBiometricBottomSheetDialogFragment.ARGS_BIOMETRIC_ITEM, t)
            }
    }

    private val t: BiometricItem by lazy {
        requireArguments().getParcelable(ValuableBiometricBottomSheetDialogFragment.ARGS_BIOMETRIC_ITEM)!!
    }

    private var mCountDownTimer: CountDownTimer? = null

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_precondition_bottom_sheet, null)
        (dialog as BottomSheet).run {
            setCustomView(contentView)
            dismissClickOutside = false
        }
        contentView.asset_balance.setInfo(t)
        contentView.cancel_tv.setOnClickListener {
            callback?.onCancel()
            dismiss()
        }

        val t = this.t
        if (t.state == PaymentStatus.pending.name) {
            if (t is TransferBiometricItem) {
                checkTransferTrace(t)
            } else if (t is WithdrawBiometricItem) {
                checkWithdrawTrace(t)
            }
        } else if (t.state == PaymentStatus.paid.name) {
            callback?.onSuccess()
            contentView.post { dismiss() }
        }
    }

    override fun dismiss() {
        mCountDownTimer?.cancel()
        super.dismiss()
    }

    private fun checkTransferTrace(t: TransferBiometricItem) {
        val trace = t.trace
        if (trace == null || isDuplicateTransferDisable()) {
            if (shouldShowTransferTip(t)) {
                showLargeAmountTip(t)
            } else {
                callback?.onSuccess()
                contentView.post { dismiss() }
            }
            return
        }

        val time = trace.createdAt.getRelativeTimeSpan()
        val amount = "${t.amount} ${t.asset.symbol}"
        contentView.title_tv.text = getString(R.string.transfer_duplicate_title)
        contentView.warning_tv.text = getString(R.string.wallet_transfer_recent_tip, time, t.user.fullName, amount)
        contentView.continue_tv.setOnClickListener {
            if (shouldShowTransferTip(t)) {
                showLargeAmountTip(t)
            } else {
                callback?.onSuccess()
                dismiss()
            }
        }
        startCountDown()
    }

    private fun checkWithdrawTrace(t: WithdrawBiometricItem) {
        val trace = t.trace
        if (trace == null || isDuplicateTransferDisable()) {
            if (shouldShowWithdrawalTip(t)) {
                showFirstWithdrawalTip(t)
            } else {
                callback?.onSuccess()
                contentView.post { dismiss() }
            }
            return
        }

        val time = trace.createdAt.getRelativeTimeSpan()
        val amount = "${t.amount} ${t.asset.symbol}"
        contentView.title_tv.text = getString(R.string.withdraw_duplicate_title)
        contentView.warning_tv.text = getString(
            R.string.wallet_withdrawal_recent_tip,
            time,
            t.displayAddress().formatPublicKey(),
            amount
        )
        contentView.continue_tv.setOnClickListener {
            callback?.onSuccess()
            dismiss()
        }
        startCountDown()
    }

    private fun showLargeAmountTip(t: TransferBiometricItem) {
        contentView.title_tv.text = getString(R.string.wallet_transaction_tip_title)
        val fiatAmount =
            (BigDecimal(t.amount) * t.asset.priceFiat()).numberFormat2()
        contentView.warning_tv.text = getString(
            R.string.wallet_transaction_tip,
            t.user.fullName,
            "${Fiats.getSymbol()}$fiatAmount",
            t.asset.symbol
        )
        contentView.continue_tv.setOnClickListener {
            callback?.onSuccess()
            dismiss()
        }
        startCountDown()
    }

    private fun showFirstWithdrawalTip(t: WithdrawBiometricItem) {
        contentView.title_tv.text = getString(R.string.bottom_withdrawal_title, t.asset.symbol)
        contentView.warning_tv.text = getString(R.string.bottom_withdrawal_address_tips)
        contentView.continue_tv.text = getString(R.string.bottom_withdrawal_change_amount)
        contentView.continue_tv.textColor = ContextCompat.getColor(requireContext(), R.color.white)
        contentView.continue_tv.setOnClickListener {
            callback?.onCancel()
            dismiss()
        }
        contentView.cancel_tv.text = getString(R.string.bottom_withdrawal_address_continue)
        contentView.cancel_tv.setTextColor(resources.getColor(R.color.colorDarkBlue, null))
        contentView.cancel_tv.setOnClickListener {
            callback?.onSuccess()
            dismiss()
        }
        contentView.continue_tv.isEnabled = true
        contentView.cancel_tv.isEnabled = true
    }

    private fun shouldShowWithdrawalTip(t: WithdrawBiometricItem): Boolean {
        val price = t.asset.priceUsd.toDoubleOrNull() ?: return false
        val amount = BigDecimal(t.amount).toDouble() * price
        if (amount <= 10) {
            return false
        }
        val hasWithdrawalAddressSet = defaultSharedPreferences.getStringSet(Constants.Account.PREF_HAS_WITHDRAWAL_ADDRESS_SET, null)
        return if (hasWithdrawalAddressSet == null) {
            true
        } else {
            !hasWithdrawalAddressSet.contains(t.addressId)
        }
    }

    private fun shouldShowTransferTip(t: TransferBiometricItem): Boolean {
        val price = t.asset.priceUsd.toDoubleOrNull() ?: return false
        val amount = BigDecimal(t.amount).toDouble() * price
        return amount >= (Session.getAccount()!!.transferConfirmationThreshold)
    }

    private fun isDuplicateTransferDisable() = !defaultSharedPreferences.getBoolean(PREF_DUPLICATE_TRANSFER, true)

    private fun startCountDown() {
        contentView.continue_tv.isEnabled = false
        contentView.continue_tv.textColor = ContextCompat.getColor(requireContext(), R.color.wallet_text_gray)
        mCountDownTimer?.cancel()
        mCountDownTimer = object : CountDownTimer(4000, 1000) {

            override fun onTick(l: Long) {
                if (isAdded) {
                    contentView.continue_tv.text = getString(R.string.wallet_transaction_continue_count, l / 1000)
                }
            }

            override fun onFinish() {
                if (isAdded) {
                    contentView.continue_tv.text = getString(R.string.common_continue)
                    contentView.continue_tv.textColor = ContextCompat.getColor(requireContext(), R.color.white)
                    contentView.continue_tv.isEnabled = true
                }
            }
        }
        mCountDownTimer?.start()
    }

    var callback: Callback? = null

    interface Callback {
        fun onSuccess()

        fun onCancel()
    }
}
