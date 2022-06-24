package one.mixin.android.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

fun getEncryptCipher(alias: String): Cipher {
    val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
    cipher.init(Cipher.ENCRYPT_MODE, getKeyByAlias(alias))
    return cipher
}

fun getDecryptCipher(alias: String, iv: ByteArray): Cipher {
    val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
    cipher.init(Cipher.DECRYPT_MODE, getKeyByAlias(alias), IvParameterSpec(iv))
    return cipher
}

fun deleteKeyByAlias(alias: String): Boolean {
    try {
        val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        ks.deleteEntry(alias)
        return true
    } catch (e: Exception) {
        reportException("deleteKeyByAlias", e)
    }
    return false
}

private fun getKeyByAlias(
    alias: String,
    userAuthenticationRequired: Boolean = false,
): SecretKey? {
    val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    var key: SecretKey? = null
    try {
        key = ks.getKey(alias, null) as? SecretKey
    } catch (e: Exception) {
        reportException("getKeyByAlias", e)
    }
    try {
        if (key == null) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_CBC,
                    KeyProperties.BLOCK_MODE_CTR,
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_PKCS7,
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
            if (userAuthenticationRequired) {
                builder.setUserAuthenticationRequired(true)
                    .setUserAuthenticationValidityDurationSeconds(2 * 60 * 60)
            }
            keyGenerator.init(builder.build())
            key = keyGenerator.generateKey()
        }
    } catch (e: Exception) {
        reportException("getKeyByAlias", e)
    }
    return key
}