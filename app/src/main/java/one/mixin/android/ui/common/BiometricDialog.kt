package one.mixin.android.ui.common

import android.content.Context
import android.os.CancellationSignal
import android.security.keystore.UserNotAuthenticatedException
import com.bugsnag.android.Bugsnag
import java.math.BigDecimal
import java.nio.charset.Charset
import java.security.InvalidKeyException
import moe.feng.support.biometricprompt.BiometricPromptCompat
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.numberFormat2
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.util.BiometricUtil
import one.mixin.android.vo.Fiats
import org.jetbrains.anko.getStackTraceString
import org.jetbrains.anko.toast

class BiometricDialog<T : BiometricItem>(
    private val context: Context,
    private val t: T
) {
    var callback: Callback<T>? = null
    private var cancellationSignal: CancellationSignal? = null

    fun show() {
        val biometricPromptBuilder = BiometricPromptCompat.Builder(context)
        when (t) {
            is TransferBiometricItem -> {
                biometricPromptBuilder.setTitle(context.getString(R.string.wallet_bottom_transfer_to, t.user.fullName))
                    .setSubtitle(context.getString(R.string.contact_mixin_id, t.user.identityNumber))
            }
            is WithdrawBiometricItem -> {
                biometricPromptBuilder.setTitle(context.getString(R.string.withdrawal_to, t.label))
                    .setSubtitle(t.destination.formatPublicKey())
            }
        }
        biometricPromptBuilder.setDescription(getDescription())
            .setNegativeButton(context.getString(R.string.wallet_pay_with_pwd)) { _, _ ->
                callback?.showTransferBottom(t)
            }
        val biometricPrompt = biometricPromptBuilder.build()
        val cipher = try {
            BiometricUtil.getDecryptCipher(context)
        } catch (e: Exception) {
            when (e) {
                is UserNotAuthenticatedException -> callback?.showAuthenticationScreen()
                is InvalidKeyException -> {
                    BiometricUtil.deleteKey(context)
                    context.toast(R.string.wallet_biometric_invalid)
                    callback?.onCancel()
                }
                else -> Bugsnag.notify(BiometricException("getDecryptCipher. ${e.getStackTraceString()}"))
            }
            return
        }
        val cryptoObject = BiometricPromptCompat.DefaultCryptoObject(cipher)
        cancellationSignal = CancellationSignal().apply {
            setOnCancelListener { context.toast(R.string.cancel) }
        }
        biometricPrompt.authenticate(cryptoObject, cancellationSignal, biometricCallback)
    }

    private fun getDescription(): String {
        val pre = "${t.amount} ${t.asset.symbol}"
        val post = "≈ ${(BigDecimal(t.amount) * t.asset.priceFiat()).numberFormat2()} ${Fiats.currency}"
        return "$pre ($post)"
    }

    private val biometricCallback = object : BiometricPromptCompat.IAuthenticationCallback {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            if (errorCode == BiometricPromptCompat.BIOMETRIC_ERROR_CANCELED || errorCode == BiometricPromptCompat.BIOMETRIC_ERROR_USER_CANCELED) {
                callback?.onCancel()
            } else if (errorCode == BiometricPromptCompat.BIOMETRIC_ERROR_LOCKOUT ||
                errorCode == BiometricPromptCompat.BIOMETRIC_ERROR_LOCKOUT_PERMANENT) {
                cancellationSignal?.cancel()
                callback?.showTransferBottom(t)
            } else {
                errString?.let { context.toast(it) }
            }
        }

        override fun onAuthenticationSucceeded(result: BiometricPromptCompat.IAuthenticationResult) {
            val cipher = result.cryptoObject?.cipher
            if (cipher != null) {
                try {
                    val encrypt = context.defaultSharedPreferences.getString(Constants.BIOMETRICS_PIN, null)
                    val decryptByteArray = cipher.doFinal(Base64.decode(encrypt, Base64.URL_SAFE))
                    t.pin = decryptByteArray.toString(Charset.defaultCharset())
                    callback?.onStartTransfer(t)
                } catch (e: Exception) {
                    Bugsnag.notify(BiometricException("onAuthenticationSucceeded  ${e.getStackTraceString()}"))
                }
            }
        }

        override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
        }

        override fun onAuthenticationFailed() {
        }
    }

    interface Callback<T : BiometricItem> {
        fun onStartTransfer(t: T)

        fun showTransferBottom(t: T)

        fun showAuthenticationScreen()

        fun onCancel()
    }
}

class BiometricException(message: String) : IllegalStateException(message)
