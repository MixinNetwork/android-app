package one.mixin.android.ui.conversation.tansfer

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.VISIBLE
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putStringSet
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.widget.BottomSheet

class TransferBottomSheetDialogFragment : BiometricBottomSheetDialogFragment<BiometricItem>() {

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

    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        when (t) {
            is TransferBiometricItem -> {
                (t as TransferBiometricItem).let {
                    contentView.title.text = getString(R.string.wallet_bottom_transfer_to, it.user.fullName ?: "")
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
        if (!TextUtils.isEmpty(t.memo)) {
            contentView.memo.visibility = VISIBLE
            contentView.memo.text = t.memo
        }
    }

    override fun getBiometricItem() = t

    override suspend fun invokeNetwork(pin: String): MixinResponse<Void> {
        return when (t) {
            is TransferBiometricItem ->
                (t as TransferBiometricItem).let {
                    bottomViewModel.transfer(t.asset.assetId, it.user.userId, t.amount, pin, t.trace, t.memo)
                }
            else ->
                (t as WithdrawBiometricItem).let {
                    bottomViewModel.withdrawal(it.addressId, it.amount, pin, it.trace!!, it.memo)
                }
        }
    }

    override fun doWhenInvokeNetworkSuccess() {
        if (t is WithdrawBiometricItem) {
            updateFirstWithdrawalSet(t as WithdrawBiometricItem)
        }
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
