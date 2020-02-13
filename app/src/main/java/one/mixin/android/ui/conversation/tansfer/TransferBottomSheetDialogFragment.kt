package one.mixin.android.ui.conversation.tansfer

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import java.math.BigDecimal
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.*
import kotlinx.android.synthetic.main.layout_pin_biometric.view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.putStringSet
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.BiometricLayout
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.ValuableBiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.util.Session
import one.mixin.android.vo.Fiats
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.textSizeDimen

class TransferBottomSheetDialogFragment : ValuableBiometricBottomSheetDialogFragment<BiometricItem>() {

    companion object {
        const val TAG = "TransferBottomSheetDialogFragment"

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            TransferBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_BIOMETRIC_ITEM, t)
            }
    }

    private val t: BiometricItem by lazy {
        arguments!!.getParcelable<BiometricItem>(ARGS_BIOMETRIC_ITEM)!!
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_transfer_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        when (t) {
            is TransferBiometricItem -> {
                (t as TransferBiometricItem).let {
                    if (shouldShowTransferTip()) {
                        contentView.title.text = getString(R.string.wallet_transaction_tip_title)
                        contentView.title.textSize = 18f
                    } else {
                        contentView.title.text =
                            getString(R.string.wallet_bottom_transfer_to, it.user.fullName ?: "")
                    }
                    contentView.sub_title.text = "Mixin ID: ${it.user.identityNumber}"
                }
                contentView.pay_tv.setText(R.string.wallet_pay_with_pwd)
                contentView.biometric_tv.setText(R.string.wallet_pay_with_biometric)
            }
            is WithdrawBiometricItem -> {
                (t as WithdrawBiometricItem).let {
                    contentView.title.text = getString(R.string.withdrawal_to, it.label)
                    contentView.sub_title.text = it.destination
                }
                contentView.pay_tv.setText(R.string.withdrawal_with_pwd)
                contentView.biometric_tv.setText(R.string.withdrawal_with_biometric)
            }
        }
        if (!TextUtils.isEmpty(t.memo)) {
            contentView.memo.visibility = VISIBLE
            contentView.memo.text = t.memo
        }
    }

    override fun checkState(state: String) {
        if (state == PaymentStatus.paid.name) {
            contentView.error_btn.visibility = GONE
            showErrorInfo(getString(R.string.pay_paid))
        } else if (state == PaymentStatus.pending.name && shouldShowTransferTip() && t is TransferBiometricItem) {
            (t as TransferBiometricItem).let {
                val fiatAmount =
                    (BigDecimal(t.amount) * t.asset.priceFiat()).numberFormat2()
                showErrorInfo(
                    getString(
                        R.string.wallet_transaction_tip, it.user.fullName,
                        "$fiatAmount${Fiats.getSymbol()}", t.asset.symbol
                    ),
                    tickMillis = 4000L,
                    errorAction = BiometricLayout.ErrorAction.LargeAmount
                ) {
                    contentView.title.text =
                        getString(R.string.wallet_bottom_transfer_to, it.user.fullName ?: "")
                    contentView.title.textSizeDimen = R.dimen.wallet_balance_text
                }
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
                t.destination.formatPublicKey(),
                getDescription(),
                getString(R.string.wallet_pay_with_pwd))
            }
        }
    }

    override fun getBiometricItem() = t

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return when (val t = this.t) {
            is TransferBiometricItem ->
                bottomViewModel.transfer(t.asset.assetId, t.user.userId, t.amount, pin, t.trace, t.memo)
            else -> {
                t as WithdrawBiometricItem
                bottomViewModel.withdrawal(t.addressId, t.amount, pin, t.trace!!, t.memo)
            }
        }
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        if (t is WithdrawBiometricItem) {
            updateFirstWithdrawalSet(t as WithdrawBiometricItem)
        }
        showDone()
        return false
    }

    private fun shouldShowTransferTip() =
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
}
