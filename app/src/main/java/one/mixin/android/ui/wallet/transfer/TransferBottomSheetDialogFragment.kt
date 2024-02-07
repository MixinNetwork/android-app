package one.mixin.android.ui.wallet.transfer

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
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
import one.mixin.android.api.response.signature.SignatureAction
import one.mixin.android.databinding.FragmentTransferBottomSheetBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getRelativeTimeSpan
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.openExternalUrl
import one.mixin.android.extension.putLong
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.tip.exception.TipNodeException
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AddressManageBiometricItem
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.SafeMultisigsBiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.displayAddress
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.ui.wallet.WithdrawalSuspendedBottomSheet
import one.mixin.android.ui.wallet.transfer.data.TransferStatus
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.Trace
import one.mixin.android.vo.UserRelationship
import one.mixin.android.vo.safe.formatDestination
import one.mixin.android.widget.BottomSheet
import org.chromium.net.CronetException
import java.io.IOException
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException

@AndroidEntryPoint
class TransferBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "TransferBottomSheetDialogFragment"
        const val ARGS_TRANSFER = "args_transfer"
        const val DELETE = 0
        const val ADD = 1

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            TransferBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_TRANSFER, t)
            }
    }

    private val t: AssetBiometricItem by lazy {
        requireArguments().getParcelableCompat(ARGS_TRANSFER, AssetBiometricItem::class.java)!!
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
        (dialog as BottomSheet).setCustomView(contentView)
        transferViewModel.updateStatus(TransferStatus.AWAITING_CONFIRMATION)

        if (t is SafeMultisigsBiometricItem) {
            lifecycleScope.launch {
                val item = t as SafeMultisigsBiometricItem
                val result = bottomViewModel.findMultiUsers(item.senders, item.receivers)
                if (result != null) {
                    val senders = result.first
                    val receivers = result.second
                    binding.content.render(t as SafeMultisigsBiometricItem, senders, receivers)
                }
            }
        } else {
            binding.content.render(t)
        }

        binding.bottom.setOnClickListener({
            dismiss()
        }, {
            showPin()
        }, {
            dismiss()
        })

        lifecycleScope.launch {
            transferViewModel.status.collect { status ->
                binding.bottom.updateStatus(status)
                when (status) {
                    TransferStatus.AWAITING_CONFIRMATION -> {
                        renderHeader()
                        preCheck()
                    }

                    TransferStatus.FAILED -> {
                        binding.header.filed(R.string.Transfer_confirmation, transferViewModel.errorMessage)
                    }

                    TransferStatus.IN_PROGRESS -> {
                        binding.header.progress(R.string.Transfer_confirmation)
                    }

                    TransferStatus.SUCCESSFUL -> {
                        binding.header.success(R.string.Transfer_confirmation)
                        finishCheck()
                    }
                }
            }
        }
    }

    private fun renderHeader() {
        when (t) {
            is TransferBiometricItem -> {
                binding.header.setContent(R.string.Transfer_confirmation, R.string.Transfer_confirmation_desc, t.asset!!)
            }

            is WithdrawBiometricItem -> {
                binding.header.setContent(R.string.Withdrawal_confirmation, R.string.Transfer_confirmation_desc, t.asset!!)
            }

            is AddressTransferBiometricItem -> {
                binding.header.setContent(R.string.Transfer_confirmation, R.string.Transfer_confirmation_desc, t.asset!!)
            }

            is AddressManageBiometricItem -> {
                val addressManageBiometricItem = t as AddressManageBiometricItem
                val title = if (addressManageBiometricItem.type == ADD) {
                    R.string.Confirm_Adding_Address
                } else {
                    R.string.Confirm_Deleting_Address
                }
                val description = if (addressManageBiometricItem.type == ADD) {
                    R.string.Adding_address_description
                } else {
                    R.string.delete_address_description
                }
                binding.header.setContent(title, description, t.asset!!)
            }

            is SafeMultisigsBiometricItem -> {
                val multisigsBiometricItem = t as SafeMultisigsBiometricItem
                val title = if (multisigsBiometricItem.action == SignatureAction.unlock.name) {
                    R.string.Revoke_Multisig_Signature
                } else {
                    R.string.Multisig_Transaction
                }
                binding.header.setContent(title, R.string.Transfer_confirmation_desc, t.asset!!)
            }
        }
    }

    private fun preCheck() {
        lifecycleScope.launch {
            if (t is TransferBiometricItem) {
                // check Stranger
                // check DuplicateTransfer by trace
                // check Large
                val transferBiometricItem = t as TransferBiometricItem
                val tips = mutableListOf<String>()
                if (!isStrangerTransferDisable() && transferBiometricItem.users.first().relationship != UserRelationship.FRIEND.name) {
                    tips.add(getString(R.string.bottom_transfer_stranger_tip, transferBiometricItem.users.first().identityNumber))
                }
                if (isDuplicateTransferDisable() && transferBiometricItem.trace != null) {
                    val trace = transferBiometricItem.trace!!
                    val amount = "${t.amount} ${t.asset?.symbol}"
                    val time = trace.createdAt.getRelativeTimeSpan()
                    tips.add(getString(R.string.wallet_transfer_recent_tip, time, transferBiometricItem.users.first().fullName, amount))
                }
                val showLarge = checkLargeAmount(t)
                if (showLarge) {
                    val asset = t.asset ?: return@launch
                    val fiatAmount =
                        (BigDecimal(t.amount) * asset.priceFiat()).numberFormat2()
                    tips.add(
                        getString(
                            R.string.wallet_transaction_tip,
                            transferBiometricItem.users.first().fullName,
                            "${Fiats.getSymbol()}$fiatAmount",
                            asset.symbol,
                        )
                    )
                }
                if (tips.isEmpty()) {
                    binding.transferAlert.isVisible = false
                } else {
                    binding.transferAlert.isVisible = true
                    binding.transferAlert.warning(R.drawable.ic_transfer_warning, tips) {
                        dismiss()
                    }
                }
            } else if (t is WithdrawBiometricItem) {
                // check withdraw transfer
                val withdrawBiometricItem = t as WithdrawBiometricItem
                val exist = withContext(Dispatchers.IO) {
                    transferViewModel.find30daysWithdrawByAddress(formatDestination(withdrawBiometricItem.address.destination, withdrawBiometricItem.address.tag)) != null
                }
                if (exist) {
                    binding.transferAlert.isVisible = false
                } else {
                    binding.transferAlert.isVisible = true
                    binding.transferAlert.warning(R.drawable.ic_transfer_warning, listOf(getString(R.string.transfer_address_warning, formatDestination(withdrawBiometricItem.address.destination, withdrawBiometricItem.address.tag)))) {
                        dismiss()
                    }
                }
            } else if (t is AddressTransferBiometricItem) {
                // check large amount
                val showLarge = checkLargeAmount(t)
                val asset = t.asset
                if (!showLarge || asset == null) {
                    binding.transferAlert.isVisible = false
                    return@launch
                }
                val fiatAmount =
                    (BigDecimal(t.amount) * asset.priceFiat()).numberFormat2()
                val tips = listOf<String>(
                    getString(
                        R.string.wallet_transaction_tip, (t as AddressTransferBiometricItem).address,
                        "${Fiats.getSymbol()}$fiatAmount",
                        asset.symbol,
                    )
                )
                binding.transferAlert.isVisible = true
                R.drawable.ic_transfer_fingerprint
                binding.transferAlert.warning(R.drawable.ic_transfer_warning, tips) {
                    dismiss()
                }
            } else {
                // Other case do nothing
                binding.transferAlert.isVisible = false
            }
        }
    }

    private fun checkLargeAmount(t: AssetBiometricItem): Boolean {
        val threshold = BigDecimal(Session.getAccount()!!.transferConfirmationThreshold)
        if (threshold == BigDecimal.ZERO) return false
        val price = t.asset?.priceUsd?.toBigDecimalOrNull() ?: return false
        val amount = BigDecimal(t.amount).multiply(price)
        return amount > BigDecimal.ZERO && amount >= threshold
    }

    private suspend fun isDuplicateTransferDisable() =
        withContext(Dispatchers.IO) {
            !(PropertyHelper.findValueByKey(Constants.Account.PREF_DUPLICATE_TRANSFER, true))
        }

    private suspend fun isStrangerTransferDisable() =
        withContext(Dispatchers.IO) {
            !(PropertyHelper.findValueByKey(Constants.Account.PREF_STRANGER_TRANSFER, true))
        }

    private fun finishCheck() {
        val returnTo = when (val t = this.t) {
            is TransferBiometricItem -> t.returnTo
            is AddressTransferBiometricItem -> t.returnTo
            else -> null
        }
        if (returnTo.isNullOrBlank()) {
            val open = requireContext().defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
            val enable = !open && BiometricUtil.isSupport(requireContext())
            binding.transferAlert.isVisible = enable
            if (enable) {
                binding.transferAlert.info(R.drawable.ic_transfer_fingerprint, getString(R.string.enable_biometric_description), R.string.Not_Now, R.string.Enable, {
                    binding.transferAlert.isVisible = false
                }, {
                    SettingActivity.showPinSetting(requireContext())
                    binding.transferAlert.isVisible = false
                })
            }
        } else {
            binding.transferAlert.isVisible = true
            binding.transferAlert.info(R.drawable.ic_transfer_done, getString(R.string.return_to_merchant_description), R.string.Stay_in_Mixin, R.string.Back_To_Merchant, {
                binding.transferAlert.isVisible = false
            }, {
                requireContext().openExternalUrl(returnTo)
                binding.transferAlert.isVisible = false
            })
        }
    }

    private fun handleError(error: ResponseError?) {
        lifecycleScope.launch {
            if (error?.code == ErrorHandler.WITHDRAWAL_SUSPEND) {
                WithdrawalSuspendedBottomSheet.newInstance(t.asset!!).show(parentFragmentManager, WithdrawalSuspendedBottomSheet.TAG)
                dismissNow()
            } else if (error != null) {
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
                    t.traceId.let { traceId ->
                        bottomViewModel.suspendDeleteTraceById(traceId)
                    }
                }

                val errorInfo =
                    if (errorCode == ErrorHandler.INSUFFICIENT_TRANSACTION_FEE && t is WithdrawBiometricItem) {
                        val item = t as WithdrawBiometricItem
                        getString(
                            R.string.error_insufficient_transaction_fee_with_amount,
                            "${item.fee} ${t.asset!!.chainSymbol}",
                        )
                    } else if (errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                        requireContext().getString(R.string.error_pin_check_too_many_request)
                    } else if (errorCode == ErrorHandler.PIN_INCORRECT) {
                        val errorCount = bottomViewModel.errorCount()
                        requireContext().resources.getQuantityString(R.plurals.error_pin_incorrect_with_times, errorCount, errorCount)
                    } else {
                        requireContext().getMixinErrorStringByCode(errorCode, errorDescription)
                    }
                transferViewModel.errorMessage = errorInfo
            } else {
                // do nothing
            }
        }
    }

    private fun handleError(throwable: Throwable) {
        transferViewModel.errorMessage =
            when (throwable) {
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

                        else -> getString(R.string.error_unknown_with_message, throwable.message)
                    }

                is TipNodeException -> {
                    throwable.getTipExceptionMsg(requireContext())
                }

                is ExecutionException -> {
                    if (throwable.cause is CronetException) {
                        val extra =
                            if (throwable is org.chromium.net.NetworkException) {
                                val e = throwable as org.chromium.net.NetworkException
                                "${e.errorCode}, ${e.cronetInternalErrorCode}"
                            } else {
                                ""
                            }
                        "${getString(R.string.error_connection_error)} $extra"
                    } else {
                        getString(R.string.error_connection_error)
                    }
                }

                else -> getString(R.string.error_unknown_with_message, throwable.message)
            }
    }

    private fun showPin() {
        PinInputBottomSheetDialogFragment.newInstance(biometricInfo = getBiometricInfo()).setOnPinComplete { pin ->
            lifecycleScope.launch(CoroutineExceptionHandler { _, error ->
                handleError(error)
                transferViewModel.updateStatus(TransferStatus.FAILED)
            }) {
                transferViewModel.updateStatus(TransferStatus.IN_PROGRESS)
                val t = this@TransferBottomSheetDialogFragment.t
                val trace: Trace?
                val asset = requireNotNull(t.asset)
                val response = withContext(Dispatchers.IO) {
                    when (t) {
                        is TransferBiometricItem -> {
                            val opponentId = if (t.users.size == 1) t.users.first().userId else ""
                            trace = Trace(t.traceId, asset.assetId, t.amount, opponentId, null, null, null, nowInUtc())
                            val receiverIds = t.users.map { it.userId }
                            bottomViewModel.kernelTransaction(asset.assetId, receiverIds, t.threshold, t.amount, pin, t.traceId, t.memo)
                        }

                        is AddressTransferBiometricItem -> {
                            trace = Trace(t.traceId, asset.assetId, t.amount, null, t.address, null, null, nowInUtc())
                            bottomViewModel.kernelAddressTransaction(asset.assetId, t.address, t.amount, pin, t.traceId, t.memo)
                        }

                        is SafeMultisigsBiometricItem -> {
                            trace = Trace(t.traceId, asset.assetId, t.amount, null, null, null, null, nowInUtc())
                            bottomViewModel.transactionMultisigs(t, pin)
                        }

                        is AddressManageBiometricItem -> {
                            val addressManageBiometricItem = t
                            trace = null
                            if (addressManageBiometricItem.type == ADD) {
                                val assetId = addressManageBiometricItem.asset!!.assetId
                                val destination = addressManageBiometricItem.destination
                                val label = addressManageBiometricItem.label
                                val tag = addressManageBiometricItem.tag
                                bottomViewModel.syncAddr(assetId, destination, label, tag, pin).apply {
                                    if (isSuccess) {
                                        bottomViewModel.saveAddr(data as Address)
                                    }
                                }
                            } else {
                                val addressId = requireNotNull(addressManageBiometricItem.addressId)
                                bottomViewModel.deleteAddr(addressId, pin).apply {
                                    if (isSuccess) {
                                        bottomViewModel.deleteLocalAddr(addressId)
                                    }
                                }
                            }
                        }
                        is WithdrawBiometricItem -> {
                            trace = Trace(t.traceId, asset.assetId, t.amount, null, t.address.destination, t.address.tag, null, nowInUtc())
                            val fee = requireNotNull(t.fee) { "required fee can not be null" }
                            bottomViewModel.kernelWithdrawalTransaction(Constants.MIXIN_FEE_USER_ID, t.traceId, asset.assetId, fee.token.assetId, t.amount, fee.fee, t.address.destination, t.address.tag, t.memo, pin)
                        }

                        else -> {
                            throw IllegalArgumentException("Don't support")
                        }
                    }
                }
                if (trace != null) {
                    bottomViewModel.insertTrace(trace)
                }
                if (response.isSuccess) {
                    defaultSharedPreferences.putLong(
                        Constants.BIOMETRIC_PIN_CHECK,
                        System.currentTimeMillis(),
                    )
                    context?.updatePinCheck()
                    transferViewModel.updateStatus(TransferStatus.SUCCESSFUL)
                } else {
                    handleError(response.error)
                    transferViewModel.updateStatus(TransferStatus.FAILED)
                }
            }
        }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }

    private fun getBiometricInfo(): BiometricInfo? {
        return when (val t = this.t) {
            is TransferBiometricItem -> {
                if (t.users.size == 1) {
                    val user = t.users.first()
                    BiometricInfo(
                        getString(
                            R.string.transfer_to,
                            user.fullName,
                        ),
                        getString(
                            R.string.contact_mixin_id,
                            user.identityNumber,
                        ),
                        getDescription(),
                    )
                } else {
                    BiometricInfo(
                        getString(R.string.Multisig_Transaction),
                        t.memo ?: "",
                        getDescription(),
                    )
                }
            }

            is AddressTransferBiometricItem -> {
                BiometricInfo(
                    getString(
                        R.string.transfer_to,
                        t.address,
                    ),
                    "",
                    getDescription(),
                )
            }

            is AddressManageBiometricItem -> {
                null
            }

            else -> {
                t as WithdrawBiometricItem
                BiometricInfo(
                    getString(R.string.withdrawal_to, t.address.label),
                    t.displayAddress().formatPublicKey(),
                    getDescription(),
                )
            }
        }
    }

    private fun getDescription(): String {
        val t = this.t
        val asset = t.asset ?: return ""
        val pre = "${t.amount} ${asset.symbol}"
        val post = "≈ ${Fiats.getSymbol()}${(BigDecimal(t.amount) * asset.priceFiat()).numberFormat2()}"
        return "$pre ($post)"
    }
}