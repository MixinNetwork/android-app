package one.mixin.android.ui.conversation.tansfer

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.TextUtils
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.*
import kotlinx.android.synthetic.main.layout_pin_biometric.view.*
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getRelativeTimeSpan
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.putStringSet
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.BiometricLayout
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.ValuableBiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.displayAddress
import one.mixin.android.util.ErrorHandler.Companion.BLOCKCHAIN_ERROR
import one.mixin.android.util.ErrorHandler.Companion.INSUFFICIENT_BALANCE
import one.mixin.android.util.ErrorHandler.Companion.INSUFFICIENT_TRANSACTION_FEE
import one.mixin.android.util.ErrorHandler.Companion.INVALID_PIN_FORMAT
import one.mixin.android.util.ErrorHandler.Companion.PIN_INCORRECT
import one.mixin.android.util.ErrorHandler.Companion.TOO_SMALL
import one.mixin.android.util.Session
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Trace
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.textSizeDimen
import java.math.BigDecimal

class TransferBottomSheetDialogFragment : ValuableBiometricBottomSheetDialogFragment<BiometricItem>() {
    companion object {
        const val TAG = "TransferBottomSheetDialogFragment"

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            TransferBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_BIOMETRIC_ITEM, t)
            }
    }

    private val t: BiometricItem by lazy {
        requireArguments().getParcelable<BiometricItem>(ARGS_BIOMETRIC_ITEM)!!
    }

    var onDestroyListener: OnDestroyListener? = null

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_transfer_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()
        when (t) {
            is TransferBiometricItem -> {
                (t as TransferBiometricItem).let {
                    contentView.title.text =
                        getString(R.string.wallet_bottom_transfer_to, it.user.fullName ?: "")
                    contentView.sub_title.text = "Mixin ID: ${it.user.identityNumber}"
                }
                contentView.pay_tv.setText(R.string.wallet_pay_with_pwd)
                contentView.biometric_tv.setText(R.string.wallet_pay_with_biometric)
            }
            is WithdrawBiometricItem -> {
                (t as WithdrawBiometricItem).let {
                    contentView.title.text = getString(R.string.withdrawal_to, it.label)
                    contentView.sub_title.text = it.displayAddress()
                }
                contentView.pay_tv.setText(R.string.withdrawal_with_pwd)
                contentView.biometric_tv.setText(R.string.withdrawal_with_biometric)
            }
        }
        if (!TextUtils.isEmpty(t.memo)) {
            contentView.memo.visibility = VISIBLE
            contentView.memo.text = t.memo
        }
        setBiometricItem()
    }

    override fun checkState(t: BiometricItem) {
        val state = t.state
        if (state == PaymentStatus.paid.name) {
            contentView.error_btn.visibility = GONE
            showErrorInfo(getString(R.string.pay_paid))
        } else if (state == PaymentStatus.pending.name) {
            if (t is TransferBiometricItem) {
                checkTransferTrace(t)
            } else if (t is WithdrawBiometricItem) {
                checkWithdrawTrace(t)
            }
        }
    }

    override fun getBiometricInfo(): BiometricInfo {
        return when (val t = this.t) {
            is TransferBiometricItem -> {
                BiometricInfo(
                    getString(
                        R.string.wallet_bottom_transfer_to,
                        t.user.fullName
                    ),
                    getString(
                        R.string.contact_mixin_id,
                        t.user.identityNumber
                    ),
                    getDescription(),
                    getString(R.string.wallet_pay_with_pwd)
                )
            }
            else -> {
                t as WithdrawBiometricItem
                BiometricInfo(
                    getString(R.string.withdrawal_to, t.label),
                    t.displayAddress().formatPublicKey(),
                    getDescription(),
                    getString(R.string.wallet_pay_with_pwd)
                )
            }
        }
    }

    override fun getBiometricItem() = t

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        val trace: Trace
        val request = when (val t = this.t) {
            is TransferBiometricItem -> {
                trace = Trace(t.traceId!!, t.asset.assetId, t.amount, t.user.userId, null, null, null, nowInUtc())
                bottomViewModel.transfer(t.asset.assetId, t.user.userId, t.amount, pin, t.traceId, t.memo)
            }
            else -> {
                t as WithdrawBiometricItem
                trace = Trace(t.traceId!!, t.asset.assetId, t.amount, null, t.destination, t.tag, null, nowInUtc())
                bottomViewModel.withdrawal(t.addressId, t.amount, pin, t.traceId!!, t.memo)
            }
        }
        bottomViewModel.insertTrace(trace)
        bottomViewModel.delete1DayAgoTraces()
        return request
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        when (val t = this@TransferBottomSheetDialogFragment.t) {
            is TransferBiometricItem -> {}
            else -> {
                t as WithdrawBiometricItem
                updateFirstWithdrawalSet(t)
            }
        }

        t.traceId?.let { traceId ->
            lifecycleScope.launch {
                val trace = bottomViewModel.suspendFindTraceById(traceId)
                if (trace != null) {
                    val data = response.data
                    if (data is Snapshot) {
                        trace.snapshotId = data.snapshotId
                        bottomViewModel.insertTrace(trace)
                    }
                }
            }
        }

        showDone()
        return false
    }

    override fun doWithMixinErrorCode(errorCode: Int) {
        if (errorCode in arrayOf(
            INSUFFICIENT_BALANCE, INVALID_PIN_FORMAT, PIN_INCORRECT,
            TOO_SMALL, INSUFFICIENT_TRANSACTION_FEE, BLOCKCHAIN_ERROR
        )
        ) {
            t.traceId?.let { traceId ->
                lifecycleScope.launch {
                    bottomViewModel.suspendDeleteTraceById(traceId)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        onDestroyListener?.onDestroy()
    }

    private fun checkTransferTrace(t: TransferBiometricItem) {
        val trace = t.trace
        if (trace == null) {
            if (shouldShowTransferTip(t)) {
                showLargeAmountTip(t)
            }
            return
        }

        val time = trace.createdAt.getRelativeTimeSpan()
        val amount = "${t.amount} ${t.asset.symbol}"
        showErrorInfo(
            getString(R.string.wallet_transfer_recent_tip, time, t.user.fullName, amount),
            tickMillis = 4000L,
            errorAction = BiometricLayout.ErrorAction.RecentPaid
        ) {
            if (shouldShowTransferTip(t)) {
                showLargeAmountTip(t)
            } else {
                contentView.title.text =
                    getString(R.string.wallet_bottom_transfer_to, t.user.fullName ?: "")
                contentView.title.textSizeDimen = R.dimen.wallet_balance_text
                showPin()
            }
        }
    }

    private fun checkWithdrawTrace(t: WithdrawBiometricItem) {
        val trace = t.trace ?: return

        val time = trace.createdAt.getRelativeTimeSpan()
        val amount = "${t.amount} ${t.asset.symbol}"
        showErrorInfo(
            getString(
                R.string.wallet_withdrawal_recent_tip, time, t.displayAddress().formatPublicKey(), amount
            ),
            tickMillis = 4000L,
            errorAction = BiometricLayout.ErrorAction.RecentPaid
        ) {
            contentView.title.text = getString(R.string.withdrawal_to, t.label)
            contentView.sub_title.text = t.destination
            contentView.title.textSizeDimen = R.dimen.wallet_balance_text
            showPin()
        }
    }

    private fun showLargeAmountTip(t: TransferBiometricItem) {
        contentView.title.text = getString(R.string.wallet_transaction_tip_title)
        contentView.title.textSize = 18f
        val fiatAmount =
            (BigDecimal(t.amount) * t.asset.priceFiat()).numberFormat2()
        showErrorInfo(
            getString(
                R.string.wallet_transaction_tip, t.user.fullName,
                "$fiatAmount${Fiats.getSymbol()}", t.asset.symbol
            ),
            tickMillis = 4000L,
            errorAction = BiometricLayout.ErrorAction.LargeAmount
        ) {
            contentView.title.text =
                getString(R.string.wallet_bottom_transfer_to, t.user.fullName ?: "")
            contentView.title.textSizeDimen = R.dimen.wallet_balance_text
            showPin()
        }
    }

    private fun shouldShowTransferTip(t: TransferBiometricItem) =
        try {
            val amount = BigDecimal(t.amount).toDouble() * t.asset.priceUsd.toDouble()
            amount >= (Session.getAccount()!!.transferConfirmationThreshold)
        } catch (e: NumberFormatException) {
            false
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

    interface OnDestroyListener {
        fun onDestroy()
    }
}
