package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.DataErrorException
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ResponseError
import one.mixin.android.api.ServerErrorException
import one.mixin.android.databinding.FragmentCashAccountPreviewBottomSheetBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.putLong
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.tip.exception.TipNodeException
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.ui.wallet.transfer.data.TransferStatus
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.msg
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.Trace
import one.mixin.android.widget.BottomSheet
import org.chromium.net.CronetException
import java.io.IOException
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException

@AndroidEntryPoint
class CashAccountPreviewBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "CashAccountPreviewBottomSheetDialogFragment"
        private const val ARGS_TRANSFER = "args_transfer"

        fun newInstance(item: TransferBiometricItem) =
            CashAccountPreviewBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_TRANSFER, item)
            }
    }

    private val item: TransferBiometricItem by lazy {
        requireArguments().getParcelableCompat(ARGS_TRANSFER, TransferBiometricItem::class.java)!!
    }

    private val binding by viewBinding(FragmentCashAccountPreviewBottomSheetBinding::inflate)
    private var isSuccess = false
    private var canRetry = true
    private var errorMessage: String? = null
    private var callback: Callback? = null
    private var dismissNotified = false

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        dialog.setCanceledOnTouchOutside(false)
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }
        binding.content.render(item)
        binding.content.setOnCloseClickListener {
            notifyDismiss(false)
            dismiss()
        }
        binding.bottom.setCancelBackgroundResource(R.drawable.bg_cash_account_preview_cancel_button)
        binding.bottom.setInProgressInPlace(true)
        updateStatus(TransferStatus.AWAITING_CONFIRMATION)
        binding.bottom.setOnClickListener(
            {
                notifyDismiss(false)
                dismiss()
            },
            {
                showPin()
            },
            {
                notifyDismiss(isSuccess)
                dismiss()
            },
        )
    }

    private fun showPin() {
        errorMessage = null
        PinInputBottomSheetDialogFragment.newInstance(biometricInfo = getBiometricInfo(), from = 1)
            .setOnPinComplete { pin ->
                lifecycleScope.launch(
                    CoroutineExceptionHandler { _, error ->
                        lifecycleScope.launch {
                            handleError(error)
                            updateStatus(TransferStatus.FAILED)
                        }
                    },
                ) {
                    updateStatus(TransferStatus.IN_PROGRESS)
                    val asset = requireNotNull(item.asset)
                    val receiverIds = item.users.map { it.userId }
                    val response = withContext(Dispatchers.IO) {
                        bottomViewModel.kernelTransaction(
                            asset.assetId,
                            receiverIds,
                            item.threshold,
                            item.amount,
                            pin,
                            item.traceId,
                            item.memo,
                            item.reference,
                        )
                    }
                    if (response.isSuccess) {
                        bottomViewModel.insertTrace(
                            Trace(
                                item.traceId,
                                asset.assetId,
                                item.amount,
                                receiverIds.firstOrNull(),
                                null,
                                null,
                                null,
                                nowInUtc(),
                            ),
                        )
                        defaultSharedPreferences.putLong(
                            Constants.BIOMETRIC_PIN_CHECK,
                            System.currentTimeMillis(),
                        )
                        context?.updatePinCheck()
                        AnalyticsTracker.trackAssetSendEnd()
                        isSuccess = true
                        updateStatus(TransferStatus.SUCCESSFUL)
                    } else {
                        handleError(response.error) {
                            updateStatus(TransferStatus.FAILED)
                        }
                    }
                }
            }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }

    private fun updateStatus(status: TransferStatus) {
        binding.bottom.updateStatus(status, canRetry)
        binding.content.setCloseEnabled(status != TransferStatus.IN_PROGRESS)
        binding.errorText.text = errorMessage
        binding.errorText.isVisible = status == TransferStatus.FAILED && !errorMessage.isNullOrBlank()
    }

    private fun handleError(
        error: ResponseError?,
        updateState: () -> Unit,
    ) {
        lifecycleScope.launch {
            canRetry = false
            error?.let {
                errorMessage = handleWithErrorCodeAndDesc(it)
                if (it.code == ErrorHandler.PIN_INCORRECT) {
                    canRetry = true
                }
            }
            updateState()
        }
    }

    private suspend fun handleError(throwable: Throwable) {
        canRetry = true
        errorMessage =
            when (throwable) {
                is TipNetworkException -> {
                    handleWithErrorCodeAndDesc(throwable.error)
                }

                is IOException ->
                    when (throwable) {
                        is SocketTimeoutException -> getString(R.string.error_connection_timeout)
                        is UnknownHostException -> getString(R.string.No_network_connection)
                        is NetworkException -> getString(R.string.No_network_connection)
                        is DataErrorException -> getString(R.string.Data_error)
                        is CronetException -> {
                            val extra =
                                if (throwable is org.chromium.net.NetworkException) {
                                    val e = throwable
                                    "${e.errorCode}, ${e.cronetInternalErrorCode}"
                                } else {
                                    ""
                                }
                            "${getString(R.string.error_connection_error)} $extra"
                        }

                        is ServerErrorException -> getString(R.string.error_server_5xx_code, throwable.code)

                        else -> getString(R.string.error_unknown_with_message, throwable.msg())
                    }

                is TipNodeException -> {
                    throwable.getTipExceptionMsg(requireContext())
                }

                is ExecutionException -> {
                    if (throwable.cause is CronetException) {
                        val extra =
                            if (throwable.cause is org.chromium.net.NetworkException) {
                                val e = throwable.cause as org.chromium.net.NetworkException
                                "${e.errorCode}, ${e.cronetInternalErrorCode}"
                            } else {
                                ""
                            }
                        "${getString(R.string.error_connection_error)} $extra"
                    } else {
                        getString(R.string.error_connection_error)
                    }
                }

                else -> getString(R.string.error_unknown_with_message, throwable.msg())
            }
    }

    private suspend fun handleWithErrorCodeAndDesc(error: ResponseError): String {
        val errorCode = error.code
        val errorDescription = error.description
        return if (errorCode == ErrorHandler.TOO_MANY_REQUEST) {
            requireContext().getString(R.string.error_pin_check_too_many_request)
        } else if (errorCode == ErrorHandler.PIN_INCORRECT) {
            val errorCount = bottomViewModel.errorCount()
            requireContext().resources.getQuantityString(R.plurals.error_pin_incorrect_with_times, errorCount, errorCount)
        } else {
            requireContext().getMixinErrorStringByCode(errorCode, errorDescription)
        }
    }

    private fun getBiometricInfo(): BiometricInfo {
        val asset = requireNotNull(item.asset)
        val fiatAmount = (item.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) * asset.priceFiat()
        return BiometricInfo(
            getString(R.string.cash_account_add_cash),
            getString(R.string.Cash_Account),
            "${item.amount} ${asset.symbol} (≈ ${Fiats.getSymbol()}${fiatAmount.numberFormat2()})",
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        notifyDismiss(isSuccess)
        super.onDismiss(dialog)
    }

    private fun notifyDismiss(success: Boolean) {
        if (dismissNotified) return
        dismissNotified = true
        callback?.onDismiss(success)
        callback = null
    }

    fun setCallback(cb: Callback) {
        callback = cb
    }

    open class Callback {
        open fun onDismiss(success: Boolean) {}
    }
}
