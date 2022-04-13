package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.view.View.GONE
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.OpponentMultisig
import one.mixin.android.api.request.RawTransactionsRequest
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.api.response.signature.SignatureAction
import one.mixin.android.api.response.signature.SignatureState
import one.mixin.android.databinding.FragmentMultisigsBottomSheetBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.Multi2MultiBiometricItem
import one.mixin.android.ui.common.biometric.MultisigsBiometricItem
import one.mixin.android.ui.common.biometric.One2MultiBiometricItem
import one.mixin.android.ui.common.biometric.ValuableBiometricBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class MultisigsBottomSheetDialogFragment :
    ValuableBiometricBottomSheetDialogFragment<MultisigsBiometricItem>() {
    companion object {
        const val TAG = "MultisigsBottomSheetDialogFragment"

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            MultisigsBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_BIOMETRIC_ITEM, t)
            }
    }

    private val t: MultisigsBiometricItem by lazy {
        requireArguments().getParcelable(ARGS_BIOMETRIC_ITEM)!!
    }

    private var success: Boolean = false

    private val binding by viewBinding(FragmentMultisigsBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()
        setBiometricItem()

        val t = this.t
        binding.apply {
            if (t is Multi2MultiBiometricItem) {
                if (t.action == SignatureAction.cancel.name) {
                    title.text = getString(R.string.log_pin_multisig_unlock)
                    arrowIv.setImageResource(R.drawable.ic_multisigs_arrow_ban)
                } else {
                    title.text = getString(R.string.multisig_transaction)
                    arrowIv.setImageResource(R.drawable.ic_multisigs_arrow_right)
                }
            } else {
                title.text = getString(R.string.multisig_transaction)
                arrowIv.setImageResource(R.drawable.ic_multisigs_arrow_right)
            }
            subTitle.text = t.memo
            biometricLayout.payTv.setText(R.string.multisig_pay_pin)
            biometricLayout.biometricTv.setText(R.string.multisig_pay_biometric)
        }

        lifecycleScope.launch {
            val result = bottomViewModel.findMultiUsers(t.senders, t.receivers)
            if (result != null) {
                val senders = result.first
                val receivers = result.second
                binding.apply {
                    sendersView.addList(senders)
                    receiversView.addList(receivers)

                    sendersView.setOnClickListener {
                        showUserList(senders, true)
                    }
                    receiversView.setOnClickListener {
                        showUserList(receivers, false)
                    }
                }
            }
        }
    }

    override fun checkState(t: BiometricItem) {
        when (t.state) {
            SignatureState.signed.name -> {
                binding.biometricLayout.errorBtn.visibility = GONE
                showErrorInfo(getString(R.string.multisig_state_signed))
            }
            SignatureState.unlocked.name -> {
                binding.biometricLayout.errorBtn.visibility = GONE
                showErrorInfo(getString(R.string.multisig_state_unlocked))
            }
            PaymentStatus.paid.name -> {
                binding.biometricLayout.errorBtn.visibility = GONE
                showErrorInfo(getString(R.string.pay_paid))
            }
        }
    }

    private fun showUserList(userList: ArrayList<User>, isSender: Boolean) {
        val title = if (isSender) {
            getString(R.string.multisig_senders)
        } else {
            getString(R.string.multisig_receivers_threshold, "${t.threshold}/${t.receivers.size}")
        }
        UserListBottomSheetDialogFragment.newInstance(userList, title)
            .showNow(parentFragmentManager, UserListBottomSheetDialogFragment.TAG)
    }

    override fun getBiometricInfo(): BiometricInfo {
        val t = this.t
        return BiometricInfo(
            requireContext().getString(
                if (t is Multi2MultiBiometricItem) {
                    if (t.action == SignatureAction.cancel.name) {
                        R.string.log_pin_multisig_unlock
                    } else {
                        R.string.multisig_transaction
                    }
                } else {
                    R.string.multisig_transaction
                }
            ),
            t.memo ?: "",
            getDescription(),
            getString(R.string.multisig_pay_pin)
        )
    }

    override fun getBiometricItem() = t

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return when (val t = this.t) {
            is Multi2MultiBiometricItem -> {
                when (t.action) {
                    SignatureAction.sign.name -> {
                        bottomViewModel.signMultisigs(t.requestId, pin)
                    }
                    else -> {
                        bottomViewModel.unlockMultisigs(t.requestId, pin)
                    }
                }
            }
            is One2MultiBiometricItem -> {
                bottomViewModel.transactions(
                    RawTransactionsRequest(
                        assetId = t.asset.assetId,
                        opponentMultisig = OpponentMultisig(t.receivers, t.threshold),
                        amount = t.amount,
                        pin = "",
                        traceId = t.traceId,
                        memo = t.memo
                    ),
                    pin
                )
            }
            else -> {
                MixinResponse<Void>()
            }
        }
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        success = true

        showDone()
        return false
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val t = this.t
        if (!success &&
            t is Multi2MultiBiometricItem &&
            t.state != SignatureState.signed.name &&
            t.state != SignatureState.unlocked.name
        ) {
            MixinApplication.appScope.launch {
                bottomViewModel.cancelMultisigs(t.requestId)
            }
        }
    }
}
