@file:Suppress("DEPRECATION")

package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.os.CancellationSignal
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import com.mattprecious.swirl.SwirlView
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ActivityAppAuthBinding
import one.mixin.android.event.AppAuthEvent
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.isLandscape
import one.mixin.android.extension.putLong
import one.mixin.android.ui.auth.showAppAuthPrompt
import one.mixin.android.util.viewBinding

class AppAuthDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "AppAuthDialogFragment"
    }

    private val binding by viewBinding(ActivityAppAuthBinding::inflate)

    private var fingerprintManager: FingerprintManagerCompat? = null
    private var biometricManager: BiometricManager? = null

    private var hasEnrolledFingerprints = false

    private var cancellationSignal: CancellationSignal? = null

    override fun getTheme() = R.style.AppTheme_AppAuthDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also { dialog ->
            (dialog as ComponentDialog).onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    failureDismiss()
                }
            })
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.setContentView(binding.root)
        updateLayout()
        fingerprintManager = FingerprintManagerCompat.from(requireContext()).apply {
            this@AppAuthDialogFragment.hasEnrolledFingerprints = hasEnrolledFingerprints()
        }
        if (!hasEnrolledFingerprints) {
            biometricManager = BiometricManager.from(requireContext())
        }

        binding.swirl.setState(SwirlView.State.ON)
        binding.swirl.setOnClickListener {
            showVerifyBottomSheet()
        }

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }
    }

    override fun onStart() {
        super.onStart()
        binding.swirl.postDelayed(showPromptRunnable, 100)
    }

    override fun onStop() {
        super.onStop()
        binding.swirl.removeCallbacks(resetSwirlRunnable)
        binding.swirl.removeCallbacks(showPromptRunnable)
        cancellationSignal?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        biometricManager = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!success) {
            failureDismiss()
        } else {
            defaultSharedPreferences.putLong(Constants.Account.PREF_APP_ENTER_BACKGROUND, System.currentTimeMillis())
        }
        MixinApplication.get().appAuthShown = false
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLayout()
    }

    private fun showPrompt() {
        cancellationSignal?.cancel()
        cancellationSignal = CancellationSignal()
        if (hasEnrolledFingerprints) {
            fingerprintManager?.authenticate(null, 0, cancellationSignal, fingerprintCallback, null)
        } else {
            showAppAuthPrompt(
                requireActivity(),
                getString(R.string.Confirm_fingerprint),
                getString(R.string.Cancel),
                biometricCallback,
            )
        }
    }

    private fun refreshSwirl(errString: CharSequence, show: Boolean) {
        showError(errString)
        binding.swirl.postDelayed(resetSwirlRunnable, 1000)
        if (show) {
            binding.swirl.postDelayed(showPromptRunnable, 1000)
        }
    }

    private fun showError(errString: CharSequence) {
        binding.info.text = errString
        binding.info.setTextColor(requireContext().getColor(R.color.colorRed))
        binding.swirl.setState(SwirlView.State.ERROR)
    }

    private fun updateLayout() {
        binding.title.updateLayoutParams<RelativeLayout.LayoutParams> {
            if (requireContext().isLandscape()) {
                removeRule(RelativeLayout.BELOW)
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                setMargins(0, 0, 0, 32.dp)
            } else {
                removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.BELOW, binding.icon.id)
                setMargins(0, 12.dp, 0, 0)
            }
        }
    }

    private var success = false

    private fun successDismiss() {
        success = true
        RxBus.publish(AppAuthEvent())
        dismiss()
    }

    private fun failureDismiss() {
        activity?.moveTaskToBack(true)
    }

    private fun showVerifyBottomSheet() {
        VerifyBottomSheetDialogFragment.newInstance(disableBiometric = true, systemAlertLevel = true)
            .setOnPinSuccess {
                successDismiss()
            }.showNow(parentFragmentManager, VerifyBottomSheetDialogFragment.TAG)
    }

    private val resetSwirlRunnable = Runnable {
        if (isAdded) {
            binding.info.text = getString(R.string.Confirm_fingerprint)
            binding.info.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
            binding.swirl.setState(SwirlView.State.ON)
        }
    }

    private val showPromptRunnable = Runnable {
        if (isAdded) {
            showPrompt()
        }
    }

    private val biometricCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (!isAdded) return

            when (errorCode) {
                BiometricPrompt.ERROR_CANCELED, BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                    failureDismiss()
                }
                BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                    showError(errString)
                }
                BiometricPrompt.ERROR_NO_BIOMETRICS, BiometricPrompt.ERROR_HW_NOT_PRESENT, BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                    showVerifyBottomSheet()
                }
                else -> {
                    refreshSwirl(errString, true)
                }
            }
        }

        override fun onAuthenticationFailed() {
            refreshSwirl(getString(R.string.Not_recognized), false)
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            successDismiss()
        }
    }

    private val fingerprintCallback = object : FingerprintManagerCompat.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (!isAdded) return

            when (errorCode) {
                FingerprintManager.FINGERPRINT_ERROR_CANCELED, FingerprintManager.FINGERPRINT_ERROR_USER_CANCELED, 1010 -> {
                    failureDismiss()
                }
                FingerprintManager.FINGERPRINT_ERROR_LOCKOUT, FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT -> {
                    showError(errString)
                }
                FingerprintManager.FINGERPRINT_ERROR_NO_FINGERPRINTS, FingerprintManager.FINGERPRINT_ERROR_HW_NOT_PRESENT, FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE -> {
                    showVerifyBottomSheet()
                }
                else -> {
                    refreshSwirl(errString, true)
                }
            }
        }

        override fun onAuthenticationFailed() {
            refreshSwirl(getString(R.string.Not_recognized), false)
        }

        override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
            successDismiss()
        }

        override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
            refreshSwirl(helpString.toString(), true)
        }
    }
}
