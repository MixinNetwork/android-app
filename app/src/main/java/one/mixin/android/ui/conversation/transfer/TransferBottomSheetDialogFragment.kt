package one.mixin.android.ui.conversation.transfer

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.TextUtils
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.databinding.FragmentTransferBottomSheetBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putStringSet
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.ValuableBiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.displayAddress
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.BLOCKCHAIN_ERROR
import one.mixin.android.util.ErrorHandler.Companion.INSUFFICIENT_BALANCE
import one.mixin.android.util.ErrorHandler.Companion.INSUFFICIENT_TRANSACTION_FEE
import one.mixin.android.util.ErrorHandler.Companion.INVALID_PIN_FORMAT
import one.mixin.android.util.ErrorHandler.Companion.PIN_INCORRECT
import one.mixin.android.util.ErrorHandler.Companion.TOO_SMALL
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Trace
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class TransferBottomSheetDialogFragment : ValuableBiometricBottomSheetDialogFragment<AssetBiometricItem>() {
    companion object {
        const val TAG = "TransferBottomSheetDialogFragment"

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            TransferBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_BIOMETRIC_ITEM, t)
            }
    }

    private val t: AssetBiometricItem by lazy {
        requireArguments().getParcelable(ARGS_BIOMETRIC_ITEM)!!
    }

    var onDestroyListener: OnDestroyListener? = null

    private val binding by viewBinding(FragmentTransferBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()
        binding.apply {
            when (t) {
                is TransferBiometricItem -> {
                    (t as TransferBiometricItem).let {
                        title.text =
                            getString(R.string.transfer_to, it.user.fullName ?: "")
                        subTitle.text = if (it.user.identityNumber == "0") it.user.userId else "Mixin ID: ${it.user.identityNumber}"
                    }
                    biometricLayout.payTv.setText(R.string.Pay_with_PIN)
                    biometricLayout.biometricTv.setText(R.string.Biometric_Payment)
                }
                is WithdrawBiometricItem -> {
                    (t as WithdrawBiometricItem).let {
                        title.text = getString(R.string.withdrawal_to, it.label)
                        subTitle.text = it.displayAddress()
                    }
                    biometricLayout.payTv.setText(R.string.withdrawal_with_pwd)
                    biometricLayout.biometricTv.setText(R.string.withdrawal_with_biometric)
                }
            }
            if (!TextUtils.isEmpty(t.memo)) {
                memo.visibility = VISIBLE
                memo.text = t.memo
            }
        }
        setBiometricItem()
    }

    override fun checkState(t: BiometricItem) {
        val state = t.state
        if (state == PaymentStatus.paid.name) {
            binding.biometricLayout.errorBtn.visibility = GONE
            showErrorInfo(getString(R.string.pay_paid))
        }
    }

    override fun getBiometricInfo(): BiometricInfo {
        return when (val t = this.t) {
            is TransferBiometricItem -> {
                BiometricInfo(
                    getString(
                        R.string.transfer_to,
                        t.user.fullName
                    ),
                    getString(
                        R.string.contact_mixin_id,
                        t.user.identityNumber
                    ),
                    getDescription(),
                    getString(R.string.Pay_with_PIN)
                )
            }
            else -> {
                t as WithdrawBiometricItem
                BiometricInfo(
                    getString(R.string.withdrawal_to, t.label),
                    t.displayAddress().formatPublicKey(),
                    getDescription(),
                    getString(R.string.Pay_with_PIN)
                )
            }
        }
    }

    override fun getBiometricItem() = t

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        val trace: Trace
        val response = when (val t = this.t) {
            is TransferBiometricItem -> {
                trace = Trace(t.traceId!!, t.asset.assetId, t.amount, t.user.userId, null, null, null, nowInUtc())
                bottomViewModel.transfer(t.asset.assetId, t.user.userId, t.amount, pin, t.traceId, t.memo)
            }
            else -> {
                t as WithdrawBiometricItem
                trace = Trace(t.traceId!!, t.asset.assetId, t.amount, null, t.destination, t.tag, null, nowInUtc())
                bottomViewModel.withdrawal(t.addressId, t.amount, pin, t.traceId!!, t.memo, t.fee)
            }
        }
        bottomViewModel.insertTrace(trace)
        bottomViewModel.deletePreviousTraces()
        return response
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        when (val t = this@TransferBottomSheetDialogFragment.t) {
            is TransferBiometricItem -> {}
            else -> {
                t as WithdrawBiometricItem
                updateFirstWithdrawalSet(t)
            }
        }
        val data = response.data
        if (data is Snapshot) {
            bottomViewModel.insertSnapshot(data)
        }

        t.traceId?.let { traceId ->
            lifecycleScope.launch {
                val trace = bottomViewModel.suspendFindTraceById(traceId)
                if (trace != null) {
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

    override suspend fun doWithMixinErrorCode(errorCode: Int, pin: String): String? {
        if (errorCode in arrayOf(
                INSUFFICIENT_BALANCE,
                INVALID_PIN_FORMAT,
                PIN_INCORRECT,
                TOO_SMALL,
                INSUFFICIENT_TRANSACTION_FEE,
                BLOCKCHAIN_ERROR
            )
        ) {
            t.traceId?.let { traceId ->
                bottomViewModel.suspendDeleteTraceById(traceId)
            }

            if (errorCode == INSUFFICIENT_TRANSACTION_FEE && t is WithdrawBiometricItem) {
                val item = t as WithdrawBiometricItem
                return getString(
                    R.string.error_insufficient_transaction_fee_with_amount,
                    INSUFFICIENT_TRANSACTION_FEE, "${item.fee} ${t.asset.chainSymbol}"
                )
            }
        } else if (errorCode == ErrorHandler.WITHDRAWAL_FEE_TOO_SMALL) {
            if (t is WithdrawBiometricItem) {
                val item = t as WithdrawBiometricItem
                val oldFee = item.fee
                val newFee = withContext(Dispatchers.IO) {
                    refreshAddressAndGetFee(item, pin)
                }
                return if (newFee != null) {
                    (t as WithdrawBiometricItem).fee = newFee
                    setBiometricItem()

                    val symbol = item.asset.chainSymbol
                    getString(R.string.wallet_withdrawal_changed, "${oldFee}$symbol", "${newFee}$symbol")
                } else {
                    getString(R.string.wallet_refresh_address_failed)
                }
            }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        onDestroyListener?.onDestroy()
    }

    @SuppressLint("MutatingSharedPrefs")
    private fun updateFirstWithdrawalSet(item: WithdrawBiometricItem) {
        var firsSet = defaultSharedPreferences.getStringSet(Constants.Account.PREF_HAS_WITHDRAWAL_ADDRESS_SET, null)
        if (firsSet == null) {
            firsSet = setOf(item.addressId)
        } else {
            firsSet.add(item.addressId)
        }
        defaultSharedPreferences.putStringSet(Constants.Account.PREF_HAS_WITHDRAWAL_ADDRESS_SET, firsSet)
    }

    private suspend fun refreshAddressAndGetFee(item: WithdrawBiometricItem, pin: String): String? {
        return try {
            val response = bottomViewModel.syncAddr(item.asset.assetId, item.destination, item.label, item.tag, pin)
            if (response.isSuccess) {
                val address = response.data as Address
                bottomViewModel.saveAddr(address)
                return address.fee
            } else {
                ErrorHandler.handleMixinError(response.errorCode, response.errorDescription)
                null
            }
        } catch (e: Exception) {
            ErrorHandler.handleError(e)
            null
        }
    }

    interface OnDestroyListener {
        fun onDestroy()
    }
}
