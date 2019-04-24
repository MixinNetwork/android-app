package one.mixin.android.ui.common

import android.content.Context
import android.os.CancellationSignal
import android.security.keystore.UserNotAuthenticatedException
import com.bugsnag.android.Bugsnag
import moe.feng.support.biometricprompt.BiometricPromptCompat
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.numberFormat2
import one.mixin.android.util.BiometricUtil
import one.mixin.android.vo.Asset
import one.mixin.android.vo.User
import org.jetbrains.anko.getStackTraceString
import org.jetbrains.anko.toast
import java.math.BigDecimal
import java.nio.charset.Charset
import java.security.InvalidKeyException

class BiometricDialog(
    private val context: Context,
    private val user: User,
    private val amount: String,
    private val asset: Asset,
    private val trace: String?,
    private val memo: String?
) {
    var callback: Callback? = null
    private var cancellationSignal: CancellationSignal? = null

    fun show() {
        val biometricPrompt = BiometricPromptCompat.Builder(context)
            .setTitle(context.getString(R.string.wallet_bottom_transfer_to, user.fullName))
            .setSubtitle(context.getString(R.string.contact_mixin_id, user.identityNumber))
            .setDescription(getDescription())
            .setNegativeButton(context.getString(R.string.wallet_pay_with_pwd)) { _, _ ->
                callback?.showTransferBottom(user, amount, asset, trace, memo)
            }
            .build()
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
        val pre = "$amount ${asset.symbol}"
        val post = context.getString(R.string.wallet_unit_usd,
            "≈ ${(BigDecimal(amount) * BigDecimal(asset.priceUsd)).numberFormat2()}")
        return "$pre ($post)"
    }

    private val biometricCallback = object : BiometricPromptCompat.IAuthenticationCallback {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            if (errorCode == BiometricPromptCompat.BIOMETRIC_ERROR_CANCELED || errorCode == BiometricPromptCompat.BIOMETRIC_ERROR_USER_CANCELED) {
                callback?.onCancel()
            } else if (errorCode == BiometricPromptCompat.BIOMETRIC_ERROR_LOCKOUT ||
                errorCode == BiometricPromptCompat.BIOMETRIC_ERROR_LOCKOUT_PERMANENT) {
                cancellationSignal?.cancel()
                callback?.showTransferBottom(user, amount, asset, trace, memo)
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
                    callback?.onStartTransfer(asset.assetId, user.userId, amount,
                        decryptByteArray.toString(Charset.defaultCharset()), trace, memo)
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

    interface Callback {
        fun onStartTransfer(
            assetId: String,
            userId: String,
            amount: String,
            pin: String,
            trace: String?,
            memo: String?
        )

        fun showTransferBottom(user: User, amount: String, asset: Asset, trace: String?, memo: String?)

        fun showAuthenticationScreen()

        fun onCancel()
    }
}

class BiometricException(message: String) : IllegalStateException(message)
