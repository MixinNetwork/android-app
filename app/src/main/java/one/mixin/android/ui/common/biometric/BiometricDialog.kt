package one.mixin.android.ui.common.biometric

import android.os.Parcelable
import android.security.keystore.UserNotAuthenticatedException
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.ERROR_CANCELED
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.parcelize.Parcelize
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.toast
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.BiometricUtil.CRASHLYTICS_BIOMETRIC
import one.mixin.android.util.reportException
import timber.log.Timber
import java.nio.charset.Charset
import java.security.InvalidKeyException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException

@Parcelize
class BiometricInfo(
    val title: String,
    val subTitle: String,
    val description: String,
) : Parcelable

class BiometricDialog(
    private val context: FragmentActivity,
    private val biometricInfo: BiometricInfo,
    private val onlyVerify: Boolean = false,
) {
    var callback: Callback? = null

    fun show() {
        val biometricPromptInfo =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(biometricInfo.title)
                .setSubtitle(biometricInfo.subTitle)
                .setDescription(biometricInfo.description)
                .setNegativeButtonText(context.getString(R.string.Verify_PIN))
                .setConfirmationRequired(true)
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .build()
        if (onlyVerify) {
            val biometricPrompt = BiometricPrompt(context, ContextCompat.getMainExecutor(context), authenticationCallback)
            biometricPrompt.authenticate(biometricPromptInfo)
            return
        }

        val cipher: Cipher? =
            try {
                BiometricUtil.getDecryptCipher(context)
            } catch (e: Exception) {
                if (e is UserNotAuthenticatedException) {
                    null
                } else {
                    handleNonUserAuthException(e)
                    return
                }
            }
        val biometricPrompt = BiometricPrompt(context, ContextCompat.getMainExecutor(context), authenticationCallback)
        if (cipher != null) {
            val cryptoObject = BiometricPrompt.CryptoObject(cipher)
            biometricPrompt.authenticate(biometricPromptInfo, cryptoObject)
        } else {
            biometricPrompt.authenticate(biometricPromptInfo)
        }
    }

    private fun handleNonUserAuthException(e: Exception) {
        when (e) {
            is InvalidKeyException, is NullPointerException -> {
                BiometricUtil.deleteKey(context)
                toast(R.string.wallet_biometric_invalid)
                reportException("$CRASHLYTICS_BIOMETRIC-getDecryptCipher", e)
                callback?.onCancel()
            }
            else ->
                reportException("$CRASHLYTICS_BIOMETRIC-getDecryptCipher", e)
        }
    }

    private val authenticationCallback =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence,
            ) {
                when (errorCode) {
                    ERROR_CANCELED, ERROR_USER_CANCELED -> {
                        callback?.onCancel()
                    }
                    ERROR_LOCKOUT, ERROR_LOCKOUT_PERMANENT -> {
                        callback?.showPin()
                    }
                    else -> {
                        toast(errString)
                    }
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (onlyVerify) {
                    callback?.onPinComplete("")
                    return
                }

                var cipher = result.cryptoObject?.cipher
                if (cipher == null) {
                    cipher =
                        try {
                            BiometricUtil.getDecryptCipher(context)
                        } catch (e: Exception) {
                            handleNonUserAuthException(e)
                            null
                        }
                }
                if (cipher == null) return

                try {
                    val encrypt = context.defaultSharedPreferences.getString(Constants.BIOMETRICS_PIN, null)
                    val decryptByteArray = cipher.doFinal(Base64.decode(encrypt, Base64.URL_SAFE))
                    val pin = decryptByteArray.toString(Charset.defaultCharset())
                    callback?.onPinComplete(pin)
                } catch (e: Exception) {
                    if (e is IllegalStateException ||
                        e is IllegalBlockSizeException ||
                        e is BadPaddingException
                    ) {
                        BiometricUtil.deleteKey(context)
                        toast(R.string.wallet_biometric_invalid)
                    }
                    reportException("$CRASHLYTICS_BIOMETRIC-onAuthenticationSucceeded", e)
                }
            }

            override fun onAuthenticationFailed() {
                Timber.e("onAuthenticationFailed")
            }
        }

    interface Callback {
        fun onPinComplete(pin: String)

        fun showPin()

        fun onCancel()
    }
}
