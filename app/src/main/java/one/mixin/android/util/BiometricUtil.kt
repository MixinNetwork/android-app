package one.mixin.android.util

import android.app.KeyguardManager
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import com.bugsnag.android.Bugsnag
import one.mixin.android.R
import one.mixin.android.Constants
import one.mixin.android.Constants.BIOMETRICS_ALIAS
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.extension.remove
import timber.log.Timber
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import java.util.Arrays

object BiometricUtil {

    const val REQUEST_CODE_CREDENTIALS = 101

    private const val FEATURE_IRIS = "android.hardware.iris"
    private const val FEATURE_FACE = "android.hardware.face"
    private val SUPPORTED_BIOMETRIC_FEATURES = arrayOf(PackageManager.FEATURE_FINGERPRINT, FEATURE_IRIS, FEATURE_FACE)

    fun isSupport(ctx: Context): Boolean {
        return isKeyguardSecure(ctx) && isSecureHardware() && isHardwareDetected(ctx) && !RootUtil.isDeviceRooted
    }

    fun showAuthenticationScreen(fragment: Fragment) {
        val intent = fragment.requireContext().getSystemService<KeyguardManager>()?.createConfirmDeviceCredentialIntent(
            fragment.requireContext().getString(R.string.wallet_biometric_screen_lock),
            fragment.requireContext().getString(R.string.wallet_biometric_screen_lock_desc))
        if (intent != null) {
            fragment.startActivityForResult(intent, REQUEST_CODE_CREDENTIALS)
        }
    }

    fun savePin(ctx: Context, pin: String, fragment: Fragment): Boolean {
        val cipher = try {
            getEncryptCipher()
        } catch (e: Exception) {
            if (e is UserNotAuthenticatedException) {
                showAuthenticationScreen(fragment)
            }
            return false
        }
        val iv = Base64.encodeBytes(cipher.iv, Base64.URL_SAFE)
        ctx.defaultSharedPreferences.putString(Constants.BIOMETRICS_IV, iv)
        val encrypt = cipher.doFinal(pin.toByteArray(Charset.defaultCharset()))
        val result = Base64.encodeBytes(encrypt, Base64.URL_SAFE)
        ctx.defaultSharedPreferences.putString(Constants.BIOMETRICS_PIN, result)
        return true
    }

    fun deleteKey(ctx: Context) {
        val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        try {
            ks.deleteEntry(BIOMETRICS_ALIAS)
        } catch (e: Exception) {
            Bugsnag.notify(IllegalStateException("delete entry BIOMETRICS_ALIAS failed."))
            Timber.d("delete entry BIOMETRICS_ALIAS failed.")
        }

        ctx.defaultSharedPreferences.remove(Constants.BIOMETRICS_IV)
        ctx.defaultSharedPreferences.remove(Constants.BIOMETRICS_ALIAS)
        ctx.defaultSharedPreferences.remove(Constants.Account.PREF_BIOMETRICS)
    }

    fun shouldShowBiometric(ctx: Context): Boolean {
        if (!isSupport(ctx)) return false

        val openBiometrics = ctx.defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
        val biometricPinCheck = ctx.defaultSharedPreferences.getLong(Constants.BIOMETRIC_PIN_CHECK, 0)
        val biometricInterval = ctx.defaultSharedPreferences.getLong(Constants.BIOMETRIC_INTERVAL, Constants.BIOMETRIC_INTERVAL_DEFAULT)
        val currTime = System.currentTimeMillis()
        return openBiometrics && currTime - biometricPinCheck <= biometricInterval
    }

    private fun isHardwareDetected(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                val pm = context.packageManager
                Arrays.stream(SUPPORTED_BIOMETRIC_FEATURES).anyMatch { pm.hasSystemFeature(it) }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                FingerprintManagerCompat.from(context).isHardwareDetected
            }
            else -> false
        }
    }

    private fun isKeyguardSecure(ctx: Context): Boolean {
        val keyguardManager = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val isKeyguardSecure = keyguardManager.isKeyguardSecure
        if (!isKeyguardSecure) {
            deleteKey(ctx)
        }
        return isKeyguardSecure
    }

    private fun getKey(): SecretKey? {
        val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        var key: SecretKey? = null
        try {
            key = ks.getKey(BIOMETRICS_ALIAS, null) as? SecretKey
        } catch (e: Exception) {
            Bugsnag.notify(IllegalStateException("getKey BIOMETRICS_ALIAS failed."))
            Timber.d("getKey BIOMETRICS_ALIAS failed.")
        }
        try {
            if (key == null) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(BIOMETRICS_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(
                            KeyProperties.BLOCK_MODE_CBC,
                            KeyProperties.BLOCK_MODE_CTR,
                            KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7,
                            KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setUserAuthenticationRequired(true)
                        .setUserAuthenticationValidityDurationSeconds(2 * 60 * 60)
                        .build())
                key = keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Bugsnag.notify(IllegalStateException("keyGenerator init failed."))
            Timber.d("keyGenerator init failed.")
        }
        return key
    }

    private fun getEncryptCipher(): Cipher {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        return cipher
    }

    fun getDecryptCipher(ctx: Context): Cipher {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val iv = ctx.defaultSharedPreferences.getString(Constants.BIOMETRICS_IV, null)
        val ivSpec = IvParameterSpec(Base64.decode(iv, Base64.URL_SAFE))
        cipher.init(Cipher.DECRYPT_MODE, getKey(), ivSpec)
        return cipher
    }

    private fun isSecureHardware(): Boolean {
        val key = getKey() ?: return false

        val factory = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore")
        val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
        return keyInfo.isInsideSecureHardware && keyInfo.isUserAuthenticationRequirementEnforcedBySecureHardware
    }
}