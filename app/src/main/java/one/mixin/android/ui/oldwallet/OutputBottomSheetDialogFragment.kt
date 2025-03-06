package one.mixin.android.ui.oldwallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.TextUtils
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.databinding.FragmentOldwalletTransferBottomSheetBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putStringSet
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.oldwallet.biometric.AssetBiometricItem
import one.mixin.android.ui.oldwallet.biometric.BiometricInfo
import one.mixin.android.ui.oldwallet.biometric.BiometricItem
import one.mixin.android.ui.oldwallet.biometric.TransferBiometricItem
import one.mixin.android.ui.oldwallet.biometric.ValuableBiometricBottomSheetDialogFragment
import one.mixin.android.ui.oldwallet.biometric.WithdrawBiometricItem
import one.mixin.android.ui.oldwallet.biometric.displayAddress
import one.mixin.android.ui.oldwallet.biometric.hasAddress
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.BLOCKCHAIN_ERROR
import one.mixin.android.util.ErrorHandler.Companion.INSUFFICIENT_BALANCE
import one.mixin.android.util.ErrorHandler.Companion.INSUFFICIENT_TRANSACTION_FEE
import one.mixin.android.util.ErrorHandler.Companion.INVALID_PIN_FORMAT
import one.mixin.android.util.ErrorHandler.Companion.PIN_INCORRECT
import one.mixin.android.util.ErrorHandler.Companion.TOO_SMALL
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.notMessengerUser
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Trace
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class OutputBottomSheetDialogFragment : ValuableBiometricBottomSheetDialogFragment<AssetBiometricItem>() {
    companion object {
        const val TAG = "OutputBottomSheetDialogFragment"

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            OutputBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_BIOMETRIC_ITEM, t)
            }
    }

    private val t: AssetBiometricItem by lazy {
        requireArguments().getParcelableCompat(ARGS_BIOMETRIC_ITEM, AssetBiometricItem::class.java)!!
    }

    var onDestroyListener: OnDestroyListener? = null

    private val binding by viewBinding(FragmentOldwalletTransferBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
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
                        subTitle.text = if (it.user.notMessengerUser()) it.user.userId else "Mixin ID: ${it.user.identityNumber}"
                    }
                    biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
                }
                is WithdrawBiometricItem -> {
                    (t as WithdrawBiometricItem).let {
                        title.text =
                            if (it.hasAddress()) {
                                getString(R.string.withdrawal_to, it.label)
                            } else {
                                getString(R.string.Withdrawal)
                            }
                        subTitle.text = it.displayAddress()
                    }
                    biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
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
                        t.user.fullName,
                    ),
                    getString(
                        R.string.contact_mixin_id,
                        t.user.identityNumber,
                    ),
                    getDescription(),
                )
            }
            else -> {
                t as WithdrawBiometricItem
                BiometricInfo(
                    getString(R.string.withdrawal_to, t.label),
                    t.displayAddress().formatPublicKey(),
                    getDescription(),
                )
            }
        }
    }

    override fun getBiometricItem() = t

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        val trace: Trace
        val response =
            when (val t = this.t) {
                is TransferBiometricItem -> {
                    trace = Trace(t.traceId!!, t.asset.assetId, t.amount, t.user.userId, null, null, null, nowInUtc())
                    bottomViewModel.transfer(t.asset.assetId, t.user.userId, t.amount, pin, t.traceId, t.memo)
                }
                else -> {
                    throw Exception()
                }
            }
        bottomViewModel.insertTrace(trace)
        bottomViewModel.deletePreviousTraces()
        return response
    }

    override fun doWhenInvokeNetworkSuccess(
        response: MixinResponse<*>,
        pin: String,
    ): Boolean {
        var returnTo: String? = null

        when (val t = this@OutputBottomSheetDialogFragment.t) {
            is TransferBiometricItem -> {
                returnTo = t.returnTo
            }
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

        showDone(returnTo)
        return false
    }

    override suspend fun doWithMixinErrorCode(
        errorCode: Int,
        pin: String,
    ): String? {
        if (errorCode in
            arrayOf(
                INSUFFICIENT_BALANCE,
                INVALID_PIN_FORMAT,
                PIN_INCORRECT,
                TOO_SMALL,
                INSUFFICIENT_TRANSACTION_FEE,
                BLOCKCHAIN_ERROR,
            )
        ) {
            t.traceId?.let { traceId ->
                bottomViewModel.suspendDeleteTraceById(traceId)
            }

            if (errorCode == INSUFFICIENT_TRANSACTION_FEE && t is WithdrawBiometricItem) {
                val item = t as WithdrawBiometricItem
                return getString(
                    R.string.error_insufficient_transaction_fee_with_amount,
                    "${item.fee} ${t.asset.chainSymbol}",
                )
            }
        } else if (errorCode == ErrorHandler.WITHDRAWAL_FEE_TOO_SMALL) {
            if (t is WithdrawBiometricItem) {
                getString(R.string.wallet_refresh_address_failed)
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

    interface OnDestroyListener {
        fun onDestroy()
    }
}
