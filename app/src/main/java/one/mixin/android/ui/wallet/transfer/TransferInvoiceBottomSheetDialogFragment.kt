package one.mixin.android.ui.wallet.transfer

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.INTERVAL_48_HOURS
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.DataErrorException
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ResponseError
import one.mixin.android.api.ServerErrorException
import one.mixin.android.databinding.FragmentTransferBottomSheetBinding
import one.mixin.android.event.BotCloseEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.openExternalUrl
import one.mixin.android.extension.putLong
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.visibleDisplayHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.tip.exception.TipNodeException
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.UtxoException
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.getUtxoExceptionMsg
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.ui.wallet.transfer.data.TransferStatus
import one.mixin.android.ui.wallet.transfer.data.TransferType
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.msg
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.MixinInvoice
import one.mixin.android.widget.BottomSheet
import org.chromium.net.CronetException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException

@AndroidEntryPoint
class TransferInvoiceBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "TransferInvoiceBottomSheetDialogFragment"
        const val ARGS_TRANSFER = "args_transfer"

        inline fun newInstance(t: String) =
            TransferInvoiceBottomSheetDialogFragment().withArgs {
                putString(ARGS_TRANSFER, t)
            }
    }

    private val invoice: MixinInvoice by lazy {
        MixinInvoice.fromString(requireArguments().getString(ARGS_TRANSFER)!!)
    }

    private val transferViewModel by viewModels<TransferViewModel>()

    private val binding by viewBinding(FragmentTransferBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        dialog.setCanceledOnTouchOutside(false)
        (dialog as BottomSheet).apply {
            onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
            setCustomView(contentView)
            setCustomViewHeight(requireActivity().visibleDisplayHeight())
        }
        initType()
        transferViewModel.updateStatus(TransferStatus.AWAITING_CONFIRMATION)

        binding.bottom.setOnClickListener({
            dismiss()
        }, {
            showPin()
        }, {
            dismiss()
        })

        RxBus.listen(BotCloseEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe { _ ->
                dismiss()
            }

        lifecycleScope.launch {
            val assets = transferViewModel.findTokenItems(invoice.entries.map { it.assetId })
            val tokenItems = invoice.entries.mapNotNull { entry ->
                assets.find { it.assetId == entry.assetId }
            }
            val receivers = if (invoice.recipient.uuidMembers.isNotEmpty()) {
                transferViewModel.findMultiUsers(invoice.recipient.uuidMembers)
            } else {
                null
            }

            binding.content.render(invoice, tokenItems, receivers) { user ->
                if (user.userId != Session.getAccountId()) {
                    showUserBottom(parentFragmentManager, user)
                }
            }

            transferViewModel.status.collect { status ->
                binding.bottom.updateStatus(status, canRetry)
                when (status) {
                    TransferStatus.AWAITING_CONFIRMATION -> {
                        binding.header.awaiting(transferType, assets)
                        preCheck()
                    }

                    TransferStatus.FAILED -> {
                        binding.header.filed(transferType, transferViewModel.errorMessage)
                    }

                    TransferStatus.IN_PROGRESS -> {
                        binding.header.progress(transferType)
                    }

                    TransferStatus.SUCCESSFUL -> {
                        binding.header.success(transferType)
                        finishCheck()
                    }

                    TransferStatus.SIGNED -> {
                        binding.header.success(transferType)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (dialog as BottomSheet).apply {
            lifecycleScope.launch {
                delay(200)
                // Hackfix: The height is not right at the first time.
                setCustomViewHeight(requireActivity().visibleDisplayHeight())
            }
            requireActivity().hideKeyboard()
        }
    }

    private val onBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (transferViewModel.status.value == TransferStatus.IN_PROGRESS) {
                    // do noting
                } else {
                    isEnabled = false
                    dismiss()
                }
            }
        }

    private var canRetry = true

    private lateinit var transferType: TransferType

    private fun initType() {
        transferType = TransferType.transfer
    }

    private fun preCheck() {
        lifecycleScope.launch {
            // Other case do nothing
            binding.transferAlert.isVisible = false
        }
    }

    private fun finishCheck() {
        val open = requireContext().defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
        val lastNotifyEnable = requireContext().defaultSharedPreferences.getLong(Constants.Account.PREF_NOTIFY_ENABLE_BIOMETRIC, 0)
        val enable = !open && (System.currentTimeMillis() - lastNotifyEnable > INTERVAL_48_HOURS) && BiometricUtil.isSupport(requireContext())
        binding.transferAlert.isVisible = enable
        if (enable) {
            binding.transferAlert.info(R.drawable.ic_transfer_fingerprint, getString(R.string.enable_biometric_description), R.string.Not_Now, R.string.Enable, {
                requireContext().defaultSharedPreferences.putLong(Constants.Account.PREF_NOTIFY_ENABLE_BIOMETRIC, System.currentTimeMillis())
                binding.transferAlert.isVisible = false
            }, {
                SettingActivity.showPinSetting(requireContext())
                binding.transferAlert.isVisible = false
            })
        }
    }

    private fun handleError(
        error: ResponseError?,
        updateState: () -> Unit,
    ) {
        lifecycleScope.launch {
            canRetry = false
            if (error != null) {
                val errorCode = error.code
                val errorDescription = error.description
                if (errorCode in
                    arrayOf(
                        ErrorHandler.INSUFFICIENT_BALANCE,
                        ErrorHandler.INVALID_PIN_FORMAT,
                        ErrorHandler.PIN_INCORRECT,
                        ErrorHandler.TOO_SMALL,
                        ErrorHandler.INSUFFICIENT_TRANSACTION_FEE,
                        ErrorHandler.BLOCKCHAIN_ERROR,
                    )
                ) {
                    // t.traceId.let { traceId ->
                    //     bottomViewModel.suspendDeleteTraceById(traceId)
                    // }
                }

                val errorInfo =
                    if (errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                        requireContext().getString(R.string.error_pin_check_too_many_request)
                    } else if (errorCode == ErrorHandler.PIN_INCORRECT) {
                        val errorCount = bottomViewModel.errorCount()
                        canRetry = true
                        requireContext().resources.getQuantityString(R.plurals.error_pin_incorrect_with_times, errorCount, errorCount)
                    } else {
                        requireContext().getMixinErrorStringByCode(errorCode, errorDescription)
                    }
                transferViewModel.errorMessage = errorInfo
            } else {
                // do nothing
            }
            updateState()
        }
    }

    private suspend fun handleError(throwable: Throwable) {
        canRetry = true
        transferViewModel.errorMessage =
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

                is UtxoException -> {
                    throwable.getUtxoExceptionMsg(requireContext())
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

    private fun showPin() {
        PinInputBottomSheetDialogFragment.newInstance(biometricInfo = getBiometricInfo(), from = 1).setOnPinComplete { pin ->
            lifecycleScope.launch(
                CoroutineExceptionHandler { _, error ->
                    lifecycleScope.launch {
                        handleError(error)
                        transferViewModel.updateStatus(TransferStatus.FAILED)
                    }
                },
            ) {
                transferViewModel.updateStatus(TransferStatus.IN_PROGRESS)
                val response = withContext(Dispatchers.IO) {
                    bottomViewModel.invoiceTransaction(pin, invoice)
                }
                if (response.isSuccess) {
                        defaultSharedPreferences.putLong(
                            Constants.BIOMETRIC_PIN_CHECK,
                            System.currentTimeMillis(),
                        )
                    context?.updatePinCheck()
                    isSuccess = true
                    transferViewModel.updateStatus(TransferStatus.SUCCESSFUL)
                } else {
                    handleError(response.error) {
                        transferViewModel.updateStatus(TransferStatus.FAILED)
                    }
                }
            }
        }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }

    private fun getBiometricInfo(): BiometricInfo? {
        return BiometricInfo(
            getString(R.string.Transfer),
            "",
            getDescription(),
        )
    }

    private fun getDescription(): String {
        return ""
    }

    override fun onDestroy() {
        super.onDestroy()
        callback?.onDismiss(isSuccess)
    }

    private var isSuccess = false
    private var callback: Callback? = null

    fun setCallback(cb: Callback) {
        callback = cb
    }

    // Keeping these callback methods can only be called at most once.
    open class Callback {
        open fun onDismiss(success: Boolean) {}
    }
}
