package one.mixin.android.ui.wallet.transfer

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentTransferBottomSheetBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getRelativeTimeSpan
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.SafeMultisigsBiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.wallet.transfer.data.TransferStatus
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.Trace
import one.mixin.android.vo.UserRelationship
import one.mixin.android.vo.safe.formatDestination
import one.mixin.android.widget.BottomSheet
import timber.log.Timber
import java.math.BigDecimal

@AndroidEntryPoint
class TransferBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "TransferBottomSheetDialogFragment"
        const val ARGS_TRANSFER = "args_transfer"

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


        binding.bottom.setOnClickListener({
            dismiss()
        }, {
            showPin()
        }, {
            dismiss()
        })
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

            is SafeMultisigsBiometricItem -> {
                binding.header.setContent(R.string.Transfer_confirmation, R.string.Transfer_confirmation_desc, t.asset!!)
            }
        }


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

        lifecycleScope.launch {
            transferViewModel.status.collect { status ->
                binding.bottom.updateStatus(status)
                when (status) {
                    TransferStatus.AWAITING_CONFIRMATION -> {
                        preCheck()
                    }

                    TransferStatus.FAILED -> {
                        binding.header.filed()
                    }

                    TransferStatus.IN_PROGRESS -> {
                        binding.header.progress()
                    }

                    TransferStatus.SUCCESSFUL -> {
                        binding.header.success()
                        finishCheck()
                    }
                }
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
                Timber.e("${transferBiometricItem.users.map { it.fullName }}")
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
        // Todo
    }

    private fun showPin() {
        PinInputBottomSheetDialogFragment.newInstance().setOnPinComplete { pin ->
            lifecycleScope.launch {
                transferViewModel.updateStatus(TransferStatus.IN_PROGRESS)
                val t = this@TransferBottomSheetDialogFragment.t
                val trace: Trace
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
                bottomViewModel.insertTrace(trace)
                if (response.isSuccess) {
                    transferViewModel.updateStatus(TransferStatus.SUCCESSFUL)
                } else {
                    transferViewModel.updateStatus(TransferStatus.FAILED)
                    // Todo
                }
            }
        }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }
}