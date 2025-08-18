package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View.GONE
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.databinding.FragmentUtxoConsolidationBottomSheetBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.ValuableBiometricBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler.Companion.BLOCKCHAIN_ERROR
import one.mixin.android.util.ErrorHandler.Companion.INSUFFICIENT_BALANCE
import one.mixin.android.util.ErrorHandler.Companion.INSUFFICIENT_TRANSACTION_FEE
import one.mixin.android.util.ErrorHandler.Companion.INVALID_PIN_FORMAT
import one.mixin.android.util.ErrorHandler.Companion.PIN_INCORRECT
import one.mixin.android.util.ErrorHandler.Companion.TOO_SMALL
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Trace
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class UtxoConsolidationBottomSheetDialogFragment : ValuableBiometricBottomSheetDialogFragment<AssetBiometricItem>() {
    companion object {
        const val TAG = "UtxoConsolidationBottomSheetDialogFragment"

        fun newInstance(t: TransferBiometricItem) =
            UtxoConsolidationBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_BIOMETRIC_ITEM, t)
            }
    }

    private val t: TransferBiometricItem by lazy {
        requireArguments().getParcelableCompat(ARGS_BIOMETRIC_ITEM, TransferBiometricItem::class.java)!!
    }

    private var onDestroyListener: OnDestroyListener? = null

    private val binding by viewBinding(FragmentUtxoConsolidationBottomSheetBinding::inflate)

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
            biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
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
        return runCatching {
            BiometricInfo(
                getString(
                    R.string.transfer_to,
                    t.users.firstOrNull()?.fullName ?: "",
                ),
                getString(
                    R.string.contact_mixin_id,
                    t.users.firstOrNull()?.identityNumber ?: "",
                ),
                getDescription(),
            )
        }.getOrElse {
            BiometricInfo("", "", getDescription())
        }
    }

    override fun getBiometricItem() = t

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        val asset = requireNotNull(t.asset) { "required token can not be null" }
        val opponentId = if (t.users.size == 1) t.users.first().userId else ""
        val trace = Trace(t.traceId, asset.assetId, t.amount, opponentId, null, null, null, nowInUtc())
        val receiverIds = t.users.map { it.userId }
        val response = bottomViewModel.kernelTransaction(asset.assetId, receiverIds, t.threshold, t.amount, pin, t.traceId, t.memo)
        bottomViewModel.insertTrace(trace)
        bottomViewModel.deletePreviousTraces()
        return response
    }

    override fun doWhenInvokeNetworkSuccess(
        response: MixinResponse<*>,
        pin: String,
    ): Boolean {
        showDone()
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
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        onDestroyListener?.onDestroy()
    }

    interface OnDestroyListener {
        fun onDestroy()
    }
}
