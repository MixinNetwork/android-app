package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.view.View
import android.view.View.GONE
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_multisigs_bottom_sheet.view.*
import kotlinx.android.synthetic.main.layout_pin_biometric.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.OpponentMultisig
import one.mixin.android.api.request.RawTransactionsRequest
import one.mixin.android.api.response.MultisigsAction
import one.mixin.android.api.response.MultisigsState
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.Multi2MultiBiometricItem
import one.mixin.android.ui.common.biometric.MultisigsBiometricItem
import one.mixin.android.ui.common.biometric.One2MultiBiometricItem
import one.mixin.android.ui.common.biometric.ValuableBiometricBottomSheetDialogFragment
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

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_multisigs_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()
        setBiometricItem()

        val t = this.t
        if (t is Multi2MultiBiometricItem) {
            if (t.action == MultisigsAction.cancel.name) {
                contentView.title.text = getString(R.string.multisig_revoke_transaction)
                contentView.arrow_iv.setImageResource(R.drawable.ic_multisigs_arrow_ban)
            } else {
                contentView.title.text = getString(R.string.multisig_transaction)
                contentView.arrow_iv.setImageResource(R.drawable.ic_multisigs_arrow_right)
            }
        } else {
            contentView.title.text = getString(R.string.multisig_transaction)
            contentView.arrow_iv.setImageResource(R.drawable.ic_multisigs_arrow_right)
        }
        contentView.sub_title.text = t.memo
        contentView.pay_tv.setText(R.string.multisig_pay_pin)
        contentView.biometric_tv.setText(R.string.multisig_pay_biometric)

        lifecycleScope.launch {
            val users = withContext(Dispatchers.IO) {
                bottomViewModel.findMultiUsers(t.senders, t.receivers)
            }
            if (users.isNotEmpty()) {
                val senders = arrayListOf<User>()
                val receivers = arrayListOf<User>()
                users.forEach { u ->
                    if (u.userId in t.senders) {
                        senders.add(u)
                    }
                    if (u.userId in t.receivers) {
                        receivers.add(u)
                    }
                }
                contentView.senders_view.addUserList(senders)
                contentView.receivers_view.addUserList(receivers)

                contentView.senders_view.setOnClickListener {
                    showUserList(senders, true)
                }
                contentView.receivers_view.setOnClickListener {
                    showUserList(receivers, false)
                }
            }
        }
    }

    override fun checkState(t: BiometricItem) {
        when (t.state) {
            MultisigsState.signed.name -> {
                contentView.error_btn.visibility = GONE
                showErrorInfo(getString(R.string.multisig_state_signed))
            }
            MultisigsState.unlocked.name -> {
                contentView.error_btn.visibility = GONE
                showErrorInfo(getString(R.string.multisig_state_unlocked))
            }
            PaymentStatus.paid.name -> {
                contentView.error_btn.visibility = GONE
                showErrorInfo(getString(R.string.pay_paid))
            }
        }
    }

    private fun showUserList(userList: ArrayList<User>, isSender: Boolean) {
        val title = if (isSender) {
            getString(R.string.multisig_senders)
        } else {
            getString(R.string.multisig_receivers, "${t.threshold}/${t.receivers.size}")
        }
        UserListBottomSheetDialogFragment.newInstance(userList, title)
            .showNow(parentFragmentManager, UserListBottomSheetDialogFragment.TAG)
    }

    override fun getBiometricInfo(): BiometricInfo {
        val t = this.t
        return BiometricInfo(
            requireContext().getString(
                if (t is Multi2MultiBiometricItem) {
                    if (t.action == MultisigsAction.cancel.name) {
                        R.string.multisig_revoke_transaction
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
                    MultisigsAction.sign.name -> {
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
            t.state != MultisigsState.signed.name &&
            t.state != MultisigsState.unlocked.name
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                bottomViewModel.cancelMultisigs(t.requestId)
            }
        }
    }
}
