package one.mixin.android.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun showAppAuthPrompt(
    context: FragmentActivity,
    title: String,
    negativeBtnText: String,
    callback: BiometricPrompt.AuthenticationCallback,
    subtitle: String = " ",
    description: String = " ",
) {
    val biometricPromptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply {
                if (subtitle.isNotEmpty()) {
                    setSubtitle(subtitle)
                }
                if (description.isNotEmpty()) {
                    setDescription(description)
                }
            }
            .setNegativeButtonText(negativeBtnText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
    val biometricPrompt = BiometricPrompt(context, ContextCompat.getMainExecutor(context), callback)
    biometricPrompt.authenticate(biometricPromptInfo)
}
