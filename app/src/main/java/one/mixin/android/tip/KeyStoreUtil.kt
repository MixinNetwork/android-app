package one.mixin.android.tip

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProperties.AUTH_BIOMETRIC_STRONG
import android.security.keystore.KeyProperties.AUTH_DEVICE_CREDENTIAL
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import one.mixin.android.MixinApplication
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.util.reportException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun getEncryptCipher(alias: String): Cipher {
    val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
    cipher.init(Cipher.ENCRYPT_MODE, getKeyByAlias(alias))
    return cipher
}

fun getDecryptCipher(
    alias: String,
    iv: ByteArray,
): Cipher {
    val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
    cipher.init(Cipher.DECRYPT_MODE, getKeyByAlias(alias), IvParameterSpec(iv))
    return cipher
}

fun deleteKeyByAlias(alias: String): Boolean {
    try {
        val ks: KeyStore =
            KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
            }
        ks.deleteEntry(alias)
        return true
    } catch (e: Exception) {
        reportException("deleteKeyByAlias", e)
    }
    return false
}

fun getKeyByAlias(
    alias: String,
    userAuthenticationRequired: Boolean = false,
): SecretKey? {
    var key: SecretKey?
    var keyStoreCrash = false
    try {
        val ks: KeyStore =
            KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
            }
        key = ks.getKey(alias, null) as? SecretKey
        if (key != null) {
            return key
        }
    } catch (e: Exception) {
        keyStoreCrash = true
        key = getAesKeyFromEncryptedPreferences(MixinApplication.appContext, alias)
        if (key != null) {
            return key
        }
        reportException("getKeyByAlias", e)
    }
    try {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder =
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_CBC,
                    KeyProperties.BLOCK_MODE_CTR,
                    KeyProperties.BLOCK_MODE_GCM,
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_PKCS7,
                    KeyProperties.ENCRYPTION_PADDING_NONE,
                )
        if (userAuthenticationRequired) {
            builder.setUserAuthenticationRequired(true).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(2 * 60 * 60, AUTH_DEVICE_CREDENTIAL or AUTH_BIOMETRIC_STRONG)
                } else {
                    @Suppress("DEPRECATION")
                    setUserAuthenticationValidityDurationSeconds(2 * 60 * 60)
                }
            }
        }
        keyGenerator.init(builder.build())
        key = keyGenerator.generateKey()
        if (keyStoreCrash) {
            storeAesKeyInEncryptedPreferences(MixinApplication.appContext, alias, key)
        }
    } catch (e: Exception) {
        reportException("getKeyByAlias", e)
    }
    return key
}

private fun storeAesKeyInEncryptedPreferences(context: Context, alias: String, key: SecretKey) {
    val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "Encrypted-Preferences",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val encodedKey = key.encoded.base64Encode()
    encryptedPrefs.edit().putString(alias, encodedKey).apply()
}

private fun getAesKeyFromEncryptedPreferences(context: Context, alias: String): SecretKey? {
    val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "Encrypted-Preferences",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val encodedKey = encryptedPrefs.getString(alias, null) ?: return null
    val decodedKey = encodedKey.decodeBase64()
    return SecretKeySpec(decodedKey, "AES")
}
