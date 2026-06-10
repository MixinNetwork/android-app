package one.mixin.android.ui.setting.delete

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.DeactivateRequest
import one.mixin.android.databinding.FragmentDeleteAccountPinBottomSheetBinding
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.localDateString
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.oldwallet.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.oldwallet.biometric.BiometricInfo
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class DeleteAccountPinBottomSheetDialogFragment : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DeleteAccountPinBottomSheetDialogFragment"
        private const val VERIFICATION_ID = "verification_id"

        fun newInstance(verificationId: String?) =
            DeleteAccountPinBottomSheetDialogFragment().withArgs {
                putString(VERIFICATION_ID, verificationId)
            }
    }

    private val binding by viewBinding(FragmentDeleteAccountPinBottomSheetBinding::inflate)

    private val verificationId: String? by lazy {
        requireArguments().getString(VERIFICATION_ID, null)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()

        binding.biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
        binding.biometricLayout.measureAllChildren = false

        val info = MixinApplication.get().getString(R.string.setting_delete_account_pin_content, localDateString(System.currentTimeMillis() + 60 * 60 * 1000 * 24 * 30L))
        val learnUrl = MixinApplication.get().getString(R.string.setting_delete_account_url)
        binding.content.highlightStarTag(info, arrayOf(learnUrl))
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        val vid = verificationId
        return if (vid == null) {
            bottomViewModel.deactivate(DeactivateRequest(pin = bottomViewModel.getDeactivateTipBody(Session.getAccountId()!!, pin)))
        } else {
            bottomViewModel.deactivate(pin, vid)
        }
    }

    override fun doWhenInvokeNetworkSuccess(
        response: MixinResponse<*>,
        pin: String,
    ): Boolean {
        MixinApplication.get().closeAndClear()
        return true
    }

    override fun getBiometricInfo() =
        BiometricInfo(
            getString(R.string.Verify_by_Biometric),
            "",
            "",
        )
}
