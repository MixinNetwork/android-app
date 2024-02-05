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
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BottomSheetViewModel
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
import one.mixin.android.vo.Trace
import one.mixin.android.widget.BottomSheet

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

        binding.content.render(t)
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
        // Todo
        binding.transferAlert.isVisible = false
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