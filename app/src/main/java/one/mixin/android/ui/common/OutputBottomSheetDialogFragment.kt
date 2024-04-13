package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.TextUtils
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.MIXIN_FEE_USER_ID
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.ResponseError
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.databinding.FragmentOutputBottomSheetBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putStringSet
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.ValuableBiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.displayAddress
import one.mixin.android.ui.common.biometric.hasAddress
import one.mixin.android.ui.wallet.WithdrawalSuspendedBottomSheet
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.BLOCKCHAIN_ERROR
import one.mixin.android.util.ErrorHandler.Companion.INSUFFICIENT_BALANCE
import one.mixin.android.util.ErrorHandler.Companion.INSUFFICIENT_TRANSACTION_FEE
import one.mixin.android.util.ErrorHandler.Companion.INVALID_PIN_FORMAT
import one.mixin.android.util.ErrorHandler.Companion.PIN_INCORRECT
import one.mixin.android.util.ErrorHandler.Companion.TOO_SMALL
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Trace
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.toUser
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

    private val binding by viewBinding(FragmentOutputBottomSheetBinding::inflate)

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
            if (!TextUtils.isEmpty(t.memo)) {
                memo.visibility = VISIBLE
                memo.text = t.memo
            }
            when (t) {
                is TransferBiometricItem -> {
                    (t as TransferBiometricItem).let { t ->
                        if (t.users.size == 1) {
                            val user = t.users.first()
                            title.text =
                                getString(R.string.transfer_to, user.fullName ?: "")
                            subTitle.text = if (user.identityNumber == "0") user.userId else "Mixin ID: ${user.identityNumber}"
                        } else {
                            title.text = getString(R.string.Multisig_Transaction)
                            subTitle.text = t.memo
                            memo.isVisible = false
                            avatarLl.isVisible = true
                            arrowIv.setImageResource(R.drawable.ic_multisigs_arrow_right)
                            val senders = listOf(Session.getAccount()!!.toUser())
                            sendersView.addList(senders)
                            receiversView.addList(t.users)
                            sendersView.setOnClickListener {
                                showUserList(senders, true)
                            }
                            receiversView.setOnClickListener {
                                showUserList(t.users, false)
                            }
                        }
                    }
                    biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
                }
                is AddressTransferBiometricItem -> {
                    (t as AddressTransferBiometricItem).let {
                        title.text = getString(R.string.Transfer)
                        subTitle.text = it.address
                    }
                    biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
                }
                is WithdrawBiometricItem -> {
                    (t as WithdrawBiometricItem).let {
                        title.text =
                            if (it.hasAddress()) {
                                getString(R.string.withdrawal_to, it.address.label)
                            } else {
                                getString(R.string.Withdrawal)
                            }
                        subTitle.text = it.displayAddress()
                    }
                    biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
                }
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

    override fun getBiometricItem() = t

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        val trace: Trace
        val asset = requireNotNull(t.asset) { "required token can not be null" }
        val response =
            when (val t = this.t) {
                is TransferBiometricItem -> {
                    val opponentId = if (t.users.size == 1) t.users.first().userId else ""
                    trace = Trace(t.traceId, asset.assetId, t.amount, opponentId, null, null, null, nowInUtc())
                    val receiverIds = t.users.map { it.userId }
                    bottomViewModel.kernelTransaction(asset.assetId, receiverIds, t.threshold, t.amount, pin, t.traceId, t.memo, t.reference)
                }
                is AddressTransferBiometricItem -> {
                    trace = Trace(t.traceId, asset.assetId, t.amount, null, t.address, null, null, nowInUtc())
                    bottomViewModel.kernelAddressTransaction(asset.assetId, t.address, t.amount, pin, t.traceId, t.memo, t.reference)
                }
                else -> {
                    t as WithdrawBiometricItem
                    trace = Trace(t.traceId, asset.assetId, t.amount, null, t.address.destination, t.address.tag, null, nowInUtc())
                    val fee = requireNotNull(t.fee) { "required fee can not be null" }
                    bottomViewModel.kernelWithdrawalTransaction(MIXIN_FEE_USER_ID, t.traceId, asset.assetId, fee.token.assetId, t.amount, fee.fee, t.address.destination, t.address.tag, t.memo, pin)
                }
            }
        bottomViewModel.insertTrace(trace)
        bottomViewModel.deletePreviousTraces()
        return response
    }

    override suspend fun handleWithErrorCodeAndDesc(
        pin: String,
        error: ResponseError,
    ) {
        if (error.code == ErrorHandler.WITHDRAWAL_SUSPEND) {
            WithdrawalSuspendedBottomSheet.newInstance(t.asset!!).show(parentFragmentManager, WithdrawalSuspendedBottomSheet.TAG)
            dismissNow()
        } else {
            super.handleWithErrorCodeAndDesc(pin, error)
        }
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
            is AddressTransferBiometricItem -> {
                returnTo = t.returnTo
            }
            else -> {
                t as WithdrawBiometricItem
                updateFirstWithdrawalSet(t)
            }
        }
        val data = response.data
        if (data is SafeSnapshot) {
            bottomViewModel.insertSnapshot(data)
        }

        t.traceId?.let { traceId ->
            lifecycleScope.launch {
                val trace = bottomViewModel.suspendFindTraceById(traceId)
                if (trace != null) {
                    if (data is SafeSnapshot) {
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
                    "${item.fee} ${t.asset!!.chainSymbol}",
                )
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
            firsSet = setOf(item.address.addressId)
        } else {
            firsSet.add(item.address.addressId)
        }
        defaultSharedPreferences.putStringSet(Constants.Account.PREF_HAS_WITHDRAWAL_ADDRESS_SET, firsSet)
    }

    private fun showUserList(
        userList: List<User>,
        isSender: Boolean,
    ) {
        val t = t as TransferBiometricItem
        val title =
            if (isSender) {
                getString(R.string.Senders)
            } else {
                getString(R.string.multisig_receivers_threshold, "${t.threshold}/${t.users.size}")
            }
        UserListBottomSheetDialogFragment.newInstance(ArrayList(userList), title)
            .showNow(parentFragmentManager, UserListBottomSheetDialogFragment.TAG)
    }

    interface OnDestroyListener {
        fun onDestroy()
    }
}
