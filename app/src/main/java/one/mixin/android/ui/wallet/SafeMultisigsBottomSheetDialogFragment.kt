package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.view.View.GONE
import androidx.compose.ui.res.booleanResource
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.api.response.signature.SignatureAction
import one.mixin.android.api.response.signature.SignatureState
import one.mixin.android.databinding.FragmentSafeMultisigsBottomSheetBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.UserListBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.SafeMultisigsBiometricItem
import one.mixin.android.ui.common.biometric.ValuableBiometricBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class SafeMultisigsBottomSheetDialogFragment :
    ValuableBiometricBottomSheetDialogFragment<SafeMultisigsBiometricItem>() {
    companion object {
        const val TAG = "MultisigsBottomSheetDialogFragment"

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            SafeMultisigsBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_BIOMETRIC_ITEM, t)
            }
    }

    private val t: SafeMultisigsBiometricItem by lazy {
        requireArguments().getParcelableCompat(ARGS_BIOMETRIC_ITEM, SafeMultisigsBiometricItem::class.java)!!
    }

    private var success: Boolean = false

    private val binding by viewBinding(FragmentSafeMultisigsBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()
        setBiometricItem()

        val t = this.t
        binding.apply {
            if (t.action == SignatureAction.unlock.name) {
                title.text = getString(R.string.Revoke_multisig_transaction)
                arrowIv.setImageResource(R.drawable.ic_multisigs_arrow_ban)
            } else {
                title.text = getString(R.string.Multisig_Transaction)
                arrowIv.setImageResource(R.drawable.ic_multisigs_arrow_right)
            }
            subTitle.text = t.memo
            biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
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
        }
    }

    private fun showUserList(
        userList: ArrayList<User>,
        isSender: Boolean,
    ) {
        val title =
            if (isSender) {
                getString(R.string.Senders)
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
                if (t.action == SignatureAction.unlock.name) {
                    R.string.Revoke_multisig_transaction
                } else {
                    R.string.Multisig_Transaction
                }
            ),
            t.memo ?: "",
            getDescription(),
        )
    }

    override fun getBiometricItem() = t

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return bottomViewModel.transactionMultisigs(t, pin)
    }

    override fun doWhenInvokeNetworkSuccess(
        response: MixinResponse<*>,
        pin: String,
    ): Boolean {
        success = true
        showDone()
        return false
    }

}
