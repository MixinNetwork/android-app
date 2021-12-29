package one.mixin.android.ui.common.biometric

import android.security.keystore.UserNotAuthenticatedException
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.ERROR_CANCELED
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.toast
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.BiometricUtil.CRASHLYTICS_BIOMETRIC
import one.mixin.android.util.reportException
import java.nio.charset.Charset
import java.security.InvalidKeyException
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException

class BiometricInfo(
    val title: String,
    val subTitle: String,
    val description: String,
    val negativeBtnText: String
)

class BiometricDialog(
    private val context: FragmentActivity,
    private val biometricInfo: BiometricInfo
) {
    var callback: Callback? = null

    fun show() {
        val biometricPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(biometricInfo.title)
            .setSubtitle(biometricInfo.subTitle)
            .setDescription(biometricInfo.description)
            .setNegativeButtonText(biometricInfo.negativeBtnText)
            .setConfirmationRequired(true)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
        val cipher = try {
            BiometricUtil.getDecryptCipher(context)
        } catch (e: Exception) {
            when (e) {
                is UserNotAuthenticatedException -> callback?.showAuthenticationScreen()
                is InvalidKeyException, is NullPointerException -> {
                    BiometricUtil.deleteKey(context)
                    toast(R.string.wallet_biometric_invalid)
                    reportException("$CRASHLYTICS_BIOMETRIC-getDecryptCipher", e)
                    callback?.onCancel()
                }
                else ->
                    reportException("$CRASHLYTICS_BIOMETRIC-getDecryptCipher", e)
            }
            return
        }
        val cryptoObject = BiometricPrompt.CryptoObject(cipher)
        val biometricPrompt = BiometricPrompt(context, ContextCompat.getMainExecutor(context), authenticationCallback)
        biometricPrompt.authenticate(biometricPromptInfo, cryptoObject)
    }

    private val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode == ERROR_CANCELED || errorCode == ERROR_USER_CANCELED) {
                callback?.onCancel()
            } else if (errorCode == ERROR_LOCKOUT || errorCode == ERROR_LOCKOUT_PERMANENT) {
                callback?.showPin()
            } else {
                toast(errString)
            }
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            val cipher = result.cryptoObject?.cipher
            if (cipher != null) {
                try {
                    val encrypt = context.defaultSharedPreferences.getString(Constants.BIOMETRICS_PIN, null)
                    val decryptByteArray = cipher.doFinal(Base64.decode(encrypt, Base64.URL_SAFE))
                    val pin = decryptByteArray.toString(Charset.defaultCharset())
                    callback?.onPinComplete(pin)
                } catch (e: Exception) {
                    if (e is IllegalStateException ||
                        e is IllegalBlockSizeException ||
                        e is BadPaddingException ||
                        e is AEADBadTagException
                    ) {
                        BiometricUtil.deleteKey(context)
                        toast(R.string.wallet_biometric_invalid)
                    }
                    reportException("$CRASHLYTICS_BIOMETRIC-onAuthenticationSucceeded", e)
                }
            }
        }
    }

    interface Callback {
        fun onPinComplete(pin: String)

        fun showPin()

        fun showAuthenticationScreen()

        fun onCancel()
    }
}
