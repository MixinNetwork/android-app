package one.mixin.android.ui.setting.delete

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.databinding.FragmentDeleteAccountPinBottomSheetBinding
import one.mixin.android.extension.highlightLinkText
import one.mixin.android.extension.localDateString
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class DeleteAccountPinBottomSheetDialogFragment : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DeleteAccountPinBottomSheetDialogFragment"
        private const val VERIFICATION_ID = "verification_id"
        fun newInstance(verificationId: String) =
            DeleteAccountPinBottomSheetDialogFragment().withArgs {
                putString(VERIFICATION_ID, verificationId)
            }
    }

    private val binding by viewBinding(FragmentDeleteAccountPinBottomSheetBinding::inflate)

    private val verificationId: String by lazy {
        requireNotNull(requireArguments().getString(VERIFICATION_ID))
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()

        binding.biometricLayout.biometricTv.setText(R.string.verify_by_biometric)
        binding.biometricLayout.measureAllChildren = false

        val learn: String =
            MixinApplication.get().getString(R.string.action_learn_more)
        val info = MixinApplication.get().getString(R.string.setting_delete_account_pin_content, localDateString(System.currentTimeMillis() + 60 * 60 * 1000 * 24 * 30L))
        val learnUrl = MixinApplication.get().getString(R.string.setting_delete_account_url)
        binding.content.highlightLinkText(info, arrayOf(learn), arrayOf(learnUrl))
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return bottomViewModel.deactivate(pin, verificationId)
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        MixinApplication.get().closeAndClear()
        return true
    }

    override fun getBiometricInfo() = BiometricInfo(
        getString(R.string.verify_by_biometric),
        "",
        "",
        getString(R.string.verify_by_PIN)
    )
}
