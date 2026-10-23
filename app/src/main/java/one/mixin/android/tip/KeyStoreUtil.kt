package one.mixin.android.tip

import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.bugsnag.android.Bugsnag
import one.mixin.android.MixinApplication
import one.mixin.android.util.reportException
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

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
    cipher.init(Cipher.DECRYPT_MODE, getKeyByAlias(alias, createIfMissing = false), IvParameterSpec(iv))
    return cipher
}

@Synchronized
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
        Bugsnag.notify(e)
    }
    return false
}

@Synchronized
internal fun getKeyByAlias(
    alias: String,
    createIfMissing: Boolean = true,
): SecretKey {
    val ks: KeyStore =
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
    (ks.getKey(alias, null) as? SecretKey)?.let { return it }
    if (!createIfMissing || ks.containsAlias(alias)) {
        throw KeyStoreException("Keystore key is unavailable")
    }
    try {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder =
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_CBC,
                    KeyProperties.BLOCK_MODE_CTR,
                    KeyProperties.BLOCK_MODE_GCM,
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_PKCS7,
                    KeyProperties.ENCRYPTION_PADDING_NONE,
                )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            MixinApplication.appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        ) {
            try {
                keyGenerator.init(builder.setIsStrongBoxBacked(true).build())
                return keyGenerator.generateKey()
            } catch (e: ProviderException) {
                builder.setIsStrongBoxBacked(false)
            } catch (e: InvalidAlgorithmParameterException) {
                builder.setIsStrongBoxBacked(false)
            }
        }
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    } catch (e: Exception) {
        reportException("getKeyByAlias", e)
        Bugsnag.notify(e)
        throw e
    }
}
