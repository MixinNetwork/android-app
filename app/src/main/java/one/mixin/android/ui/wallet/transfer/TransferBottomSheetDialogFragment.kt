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
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.INTERVAL_48_HOURS
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.DataErrorException
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ResponseError
import one.mixin.android.api.ServerErrorException
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.api.response.TransactionResponse
import one.mixin.android.api.response.signature.SignatureAction
import one.mixin.android.api.response.signature.SignatureState
import one.mixin.android.databinding.FragmentTransferBottomSheetBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.event.BotCloseEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getRelativeTimeSpan
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.isCreatedAtWithinLast30Days
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.numberFormat2
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
import one.mixin.android.ui.common.biometric.AddressManageBiometricItem
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.NftBiometricItem
import one.mixin.android.ui.common.biometric.SafeMultisigsBiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.UtxoException
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.displayAddress
import one.mixin.android.ui.common.biometric.getUtxoExceptionMsg
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.ui.wallet.WithdrawalSuspendedBottomSheet
import one.mixin.android.ui.wallet.transfer.data.TransferStatus
import one.mixin.android.ui.wallet.transfer.data.TransferType
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
        dialog.setCanceledOnTouchOutside(false)
        (dialog as BottomSheet).apply {
            onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
            setCustomView(contentView)
            setCustomViewHeight(requireActivity().visibleDisplayHeight())
        }
        initType()
        if (t is SafeMultisigsBiometricItem){
            val item = t as SafeMultisigsBiometricItem
            if (item.safe != null) {
                binding.bottom.setText(if(item.action == SignatureAction.sign.name) R.string.Approve else R.string.Reject)
            }
            if (t.state == PaymentStatus.paid.name || t.state == SignatureState.signed.name) {
                transferViewModel.updateStatus(TransferStatus.SIGNED)
                binding.transferAlert.isVisible = false
            }
        } else {
            transferViewModel.updateStatus(TransferStatus.AWAITING_CONFIRMATION)
        }
        when (t) {
            is SafeMultisigsBiometricItem -> {
                val item = t as SafeMultisigsBiometricItem
                if (item.safe != null) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            item.safe.operation.transaction.recipients.forEach { item ->
                                item.label = bottomViewModel.findAddressByReceiver(item.address, "")
                            }
                        }
                        binding.content.render(item, emptyList(), emptyList()) {}
                    }

                } else {
                    lifecycleScope.launch {
                        val result = bottomViewModel.findMultiUsers(item.senders, item.receivers)
                        if (result != null) {
                            val senders = result.first
                            val receivers = result.second
                            binding.content.render(item, senders, receivers) { user ->
                                if (user.userId == Session.getAccountId()) return@render
                                showUserBottom(parentFragmentManager, user)
                            }
                        }
                    }
                }
            }

            is NftBiometricItem -> {
                binding.content.render(t) { user ->
                    if (user.userId == Session.getAccountId()) return@render
                    showUserBottom(parentFragmentManager, user)
                }
            }

            else -> {
                binding.content.render(t) { user ->
                    if (user.userId == Session.getAccountId()) return@render
                    showUserBottom(parentFragmentManager, user)
                }
            }
        }

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
            transferViewModel.status.collect { status ->
                binding.bottom.updateStatus(status, canRetry)
                when (status) {
                    TransferStatus.AWAITING_CONFIRMATION -> {
                        if (t is NftBiometricItem) {
                            binding.header.awaiting(transferType, (t as NftBiometricItem).inscriptionItem, t.asset!!)
                        } else {
                            binding.header.awaiting(transferType, t.asset!!)
                        }
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
        transferType =
            when (t) {
                is TransferBiometricItem -> {
                    TransferType.transfer
                }

                is WithdrawBiometricItem -> {
                    TransferType.withdraw
                }

                is AddressTransferBiometricItem -> {
                    TransferType.addressTransfer
                }

                is AddressManageBiometricItem -> {
                    val addressManageBiometricItem = t as AddressManageBiometricItem
                    if (addressManageBiometricItem.type == ADD) {
                        TransferType.addAddress
                    } else {
                        TransferType.deleteAddress
                    }
                }

                is SafeMultisigsBiometricItem -> {
                    val multisigsBiometricItem = t as SafeMultisigsBiometricItem
                    if (multisigsBiometricItem.safe != null) {
                        if (multisigsBiometricItem.state == PaymentStatus.paid.name) {
                            TransferType.safeSigned
                        } else if (multisigsBiometricItem.action == SignatureAction.revoke.name) {
                            TransferType.reject
                        } else {
                            TransferType.approve
                        }
                    } else {
                        if (multisigsBiometricItem.state == PaymentStatus.paid.name) {
                            TransferType.signed
                        } else if (multisigsBiometricItem.action == SignatureAction.revoke.name) {
                            TransferType.unMulSign
                        } else {
                            TransferType.mutlSign
                        }
                    }
                }

                is NftBiometricItem -> {
                    if ((t as NftBiometricItem).release) TransferType.nftRelease else TransferType.nft
                }

                else -> {
                    throw IllegalArgumentException("Unknown type")
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
                if (!isStrangerTransferDisable() && transferBiometricItem.users.size == 1 && transferBiometricItem.users.first().relationship !in listOf(UserRelationship.FRIEND.name, UserRelationship.ME.name)) {
                    tips.add(getString(R.string.unfamiliar_person_reminder, transferBiometricItem.users.first().fullName, transferBiometricItem.users.first().identityNumber))
                }
                if (!isDuplicateTransferDisable() && transferBiometricItem.trace != null) {
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
                            R.string.large_amount_reminder,
                            "${t.amount}${asset.symbol}",
                            "${Fiats.getSymbol()}$fiatAmount",
                        ),
                    )
                }
                if (tips.isEmpty()) {
                    binding.transferAlert.isVisible = false
                } else {
                    binding.transferAlert.isVisible = true
                    binding.transferAlert.warning(R.drawable.ic_transfer_warning, tips)
                }
            } else if (t is NftBiometricItem) {
                // check Stranger
                val transferBiometricItem = t as NftBiometricItem
                if (transferBiometricItem.release)
                    {
                        binding.transferAlert.isVisible = false
                        return@launch
                    }
                val tips = mutableListOf<String>()
                if (!isStrangerTransferDisable() && transferBiometricItem.receivers.size == 1 && transferBiometricItem.receivers.first().relationship != UserRelationship.FRIEND.name) {
                    tips.add(getString(R.string.unfamiliar_person_reminder, transferBiometricItem.receivers.first().fullName, transferBiometricItem.receivers.first().identityNumber))
                }
                if (tips.isEmpty()) {
                    binding.transferAlert.isVisible = false
                } else {
                    binding.transferAlert.isVisible = true
                    binding.transferAlert.warning(R.drawable.ic_transfer_warning, tips)
                }
            } else if (t is WithdrawBiometricItem) {
                // check withdraw within 30 days
                val withdrawBiometricItem = t as WithdrawBiometricItem
                val tips = mutableListOf<String>()
                val ethAddress = PropertyHelper.findValueByKey(EVM_ADDRESS, "")
                val solAddress = PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")

                val addressWarning = withdrawBiometricItem.address.destination !in listOf(ethAddress, solAddress) &&
                    withContext(Dispatchers.IO) {
                        val snapshot = transferViewModel.findLastWithdrawalSnapshotByReceiver(formatDestination(withdrawBiometricItem.address.destination, withdrawBiometricItem.address.tag))
                        if (snapshot != null) {
                            !isCreatedAtWithinLast30Days(snapshot.createdAt)
                        } else {
                            false
                        }
                    }

                if (addressWarning) {
                    tips.add(getString(R.string.address_validity_reminder, formatDestination(withdrawBiometricItem.address.destination, withdrawBiometricItem.address.tag), withdrawBiometricItem.address.label, 30))
                }

                if (tips.isEmpty()) {
                    binding.transferAlert.isVisible = false
                } else {
                    binding.transferAlert.isVisible = true
                    binding.transferAlert.warning(R.drawable.ic_transfer_warning, tips)
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
                val tips =
                    listOf(
                        getString(
                            R.string.large_amount_reminder,
                            "${t.amount}${asset.symbol}",
                            "${Fiats.getSymbol()}$fiatAmount",
                        ),
                    )
                binding.transferAlert.isVisible = true
                R.drawable.ic_transfer_fingerprint
                binding.transferAlert.warning(R.drawable.ic_transfer_warning, tips)
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
        val returnTo =
            when (val t = this.t) {
                is TransferBiometricItem -> t.returnTo
                is AddressTransferBiometricItem -> t.returnTo
                else -> null
            }
        if (returnTo.isNullOrBlank()) {
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
        } else {
            binding.transferAlert.isVisible = true
            binding.transferAlert.info(R.drawable.ic_transfer_merchant, getString(R.string.return_to_merchant_description), R.string.Stay_in_Mixin, R.string.Back_To_Merchant, {
                binding.transferAlert.isVisible = false
            }, {
                requireContext().openExternalUrl(returnTo)
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

                        else -> getString(R.string.error_unknown_with_message, throwable.message)
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

                else -> getString(R.string.error_unknown_with_message, throwable.message)
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
                val t = this@TransferBottomSheetDialogFragment.t
                val trace: Trace?
                val asset = requireNotNull(t.asset)
                val response =
                    withContext(Dispatchers.IO) {
                        when (t) {
                            is TransferBiometricItem -> {
                                val opponentId = if (t.users.size == 1) t.users.first().userId else ""
                                trace = Trace(t.traceId, asset.assetId, t.amount, opponentId, null, null, null, nowInUtc())
                                val receiverIds = t.users.map { it.userId }
                                bottomViewModel.kernelTransaction(asset.assetId, receiverIds, t.threshold, t.amount, pin, t.traceId, t.memo, t.reference)
                            }

                            is NftBiometricItem -> {
                                trace = null
                                val amount = t.releaseAmount ?: t.amount
                                bottomViewModel.kernelTransaction(asset.assetId, t.receivers.map { it.userId }, 1.toByte(), amount, pin, t.traceId, t.memo, inscriptionHash = t.inscriptionItem.inscriptionHash, release = t.release)
                            }

                            is AddressTransferBiometricItem -> {
                                trace = Trace(t.traceId, asset.assetId, t.amount, null, t.address, null, null, nowInUtc())
                                bottomViewModel.kernelAddressTransaction(asset.assetId, t.address, t.amount, pin, t.traceId, t.memo, t.reference)
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
                    isSuccess = true

                    val transactionHash = runCatching {
                        val data = response.data as? List<TransactionResponse>
                        if (data?.size == 1) {
                            data.first().transactionHash
                        } else {
                            null
                        }
                    }.getOrNull()
                    binding.content.displayHash(transactionHash)
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

            is NftBiometricItem -> {
                val user = t.receivers.first()
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
                val addressManageBiometricItem = t
                BiometricInfo(
                    getString(
                        if (addressManageBiometricItem.type == ADD) {
                            R.string.Adding_Address
                        } else {
                            R.string.Deleting_Address
                        }
                    ),
                    "",
                    getDescription(),
                )
            }

            is SafeMultisigsBiometricItem -> {
                BiometricInfo(
                    getString(
                        R.string.Multisig_Transaction,
                    ),
                    "",
                    getDescription(),
                )
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
