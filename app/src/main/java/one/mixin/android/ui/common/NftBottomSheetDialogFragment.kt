package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.view.View.GONE
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.CollectibleRequest
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.api.response.signature.SignatureAction
import one.mixin.android.api.response.signature.SignatureState
import one.mixin.android.databinding.FragmentNftBottomSheetBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.round
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.session.encryptPin
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.NftBiometricItem
import one.mixin.android.ui.common.biometric.ValuableBiometricBottomSheetDialogFragment.Companion.ARGS_BIOMETRIC_ITEM
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class NftBottomSheetDialogFragment : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "NftBottomSheetDialogFragment"

        fun newInstance(t: NftBiometricItem) =
            NftBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_BIOMETRIC_ITEM, t)
            }
    }

    private var success: Boolean = false

    private val binding by viewBinding(FragmentNftBottomSheetBinding::inflate)
    private val t: NftBiometricItem by lazy {
        requireArguments().getParcelable(ARGS_BIOMETRIC_ITEM)!!
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()
        checkState(t)

        binding.apply {
            title.text = getString(R.string.Transfer)
            arrowIv.setImageResource(R.drawable.ic_multisigs_arrow_right)
            biometricLayout.payTv.setText(R.string.multisig_pay_pin)
            biometricLayout.biometricTv.setText(R.string.multisig_pay_biometric)
            nftIv.round(4.dp)
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

            handleMixinResponse(
                invokeNetwork = { bottomViewModel.getToken(t.tokenId) },
                switchContext = Dispatchers.IO,
                successBlock = { response ->
                    response.data?.let { data ->
                        data.metadata
                        binding.apply {
                            nftIv.loadImage(data.metadata.iconUrl, R.drawable.nft_default)
                            nftGroup.text = data.metadata.groupName
                            nftTokenId.text = "#${data.tokenKey}"
                            nftTokenName.text = data.metadata.tokenName
                        }
                    }
                }
            )
        }
    }

    private fun checkState(t: BiometricItem) {
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
            "${getString(R.string.Senders)} ${t.sendersThreshold}/${t.senders.size}"
        } else {
            getString(R.string.multisig_receivers_threshold, "${t.receiversThreshold}/${t.receivers.size}")
        }
        UserListBottomSheetDialogFragment.newInstance(userList, title)
            .showNow(parentFragmentManager, UserListBottomSheetDialogFragment.TAG)
    }

    override fun getBiometricInfo(): BiometricInfo {
        val t = this.t
        return BiometricInfo(
            requireContext().getString(
                // Todo replace string
                if (t.action == SignatureAction.cancel.name) {
                    R.string.log_pin_multisig_unlock
                } else {
                    R.string.multisig_transaction
                }
            ),
            t.memo ?: "",
            "",
            getString(R.string.wallet_pay_with_pin)
        )
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        val request = CollectibleRequest(t.action, t.rawTransaction, encryptPin(Session.getPinToken()!!, pin)!!)
        return when (t.action) {
            SignatureAction.sign.name -> {
                bottomViewModel.signCollectibleTransfer(t.requestId, request)
            }
            SignatureAction.unlock.name -> {
                bottomViewModel.unlockCollectibleTransfer(t.requestId, request)
            }
            else -> {
                bottomViewModel.cancelCollectibleTransfer(t.requestId)
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
        if (!success &&
            t.state != SignatureState.signed.name &&
            t.state != SignatureState.unlocked.name
        ) {
            MixinApplication.appScope.launch {
                bottomViewModel.cancelCollectibleTransfer(t.requestId)
            }
        }
    }
}
