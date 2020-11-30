package one.mixin.android.ui.conversation.tansfer

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
import one.mixin.android.databinding.FragmentTransferBottomSheetBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putStringSet
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricItem
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
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Trace
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class TransferBottomSheetDialogFragment : ValuableBiometricBottomSheetDialogFragment<BiometricItem>() {
    companion object {
        const val TAG = "TransferBottomSheetDialogFragment"

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            TransferBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_BIOMETRIC_ITEM, t)
            }
    }

    private val t: BiometricItem by lazy {
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
                            getString(R.string.wallet_bottom_transfer_to, it.user.fullName ?: "")
                        subTitle.text = "Mixin ID: ${it.user.identityNumber}"
                    }
                    biometricLayout.payTv.setText(R.string.wallet_pay_with_pwd)
                    biometricLayout.biometricTv.setText(R.string.wallet_pay_with_biometric)
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
        bottomViewModel.deletePreviousTraces()
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

    override fun doWithMixinErrorCode(errorCode: Int): String? {
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
                lifecycleScope.launch {
                    bottomViewModel.suspendDeleteTraceById(traceId)
                }
            }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        onDestroyListener?.onDestroy()
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
