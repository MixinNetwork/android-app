package one.mixin.android.ui.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

internal fun resolveAppAuthAuthenticators(context: Context): Int {
    val biometricManager = BiometricManager.from(context)
    return if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    } else {
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    }
}

fun showAppAuthPrompt(
    context: FragmentActivity,
    title: String,
    negativeBtnText: String,
    callback: BiometricPrompt.AuthenticationCallback,
    subtitle: String = " ",
    description: String = " ",
): BiometricPrompt {
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
            .setAllowedAuthenticators(resolveAppAuthAuthenticators(context))
            .build()
    val biometricPrompt = BiometricPrompt(context, ContextCompat.getMainExecutor(context), callback)
    biometricPrompt.authenticate(biometricPromptInfo)
    return biometricPrompt
}
