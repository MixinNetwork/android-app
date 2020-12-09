@file:Suppress("NOTHING_TO_INLINE")
package one.mixin.android.crypto

import android.os.Build
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import okhttp3.tls.HeldCertificate
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.toLeByteArray
import one.mixin.android.util.reportException
import org.whispersystems.curve25519.Curve25519
import org.whispersystems.curve25519.Curve25519.BEST
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.experimental.or

fun generateRSAKeyPair(keyLength: Int = 2048): KeyPair {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(keyLength)
    return kpg.genKeyPair()
}

fun generateEd25519KeyPair(): KeyPair {
    return net.i2p.crypto.eddsa.KeyPairGenerator().generateKeyPair()
}

fun calculateAgreement(publicKey: ByteArray, privateKey: ByteArray): ByteArray? {
    try {
        return Curve25519.getInstance(BEST).calculateAgreement(publicKey, privateKey)
    } catch (t: Throwable) {
        reportException("Calculates an ECDH agreement exception", t)
    }
    return null
}

fun privateKeyToCurve25519(edSeed: ByteArray): ByteArray {
    val md = MessageDigest.getInstance("SHA-512")
    val h = md.digest(edSeed).sliceArray(IntRange(0, 31))
    h[0] = h[0] and 248.toByte()
    h[31] = h[31] and 127
    h[31] = h[31] or 64
    return h
}

inline fun KeyPair.getPublicKey(): ByteArray {
    return public.encoded
}

inline fun KeyPair.getPrivateKeyPem(): String {
    val heldCertificate = HeldCertificate.Builder().keyPair(this).build()
    return heldCertificate.privateKeyPkcs1Pem()
}

fun aesEncrypt(key: String, iterator: Long, code: String): String? {
    val keySpec = SecretKeySpec(Base64.decode(key), "AES")
    val iv = ByteArray(16)
    SecureRandom().nextBytes(iv)

    val pinByte = code.toByteArray() + (System.currentTimeMillis() / 1000).toLeByteArray() + iterator.toLeByteArray()
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
    val result = cipher.doFinal(pinByte)
    return iv.plus(result).base64Encode()
}

fun rsaDecrypt(privateKey: PrivateKey, iv: String, pinToken: String): String {
    val deCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
    deCipher.init(
        Cipher.DECRYPT_MODE,
        privateKey,
        OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified(iv.toByteArray())
        )
    )
    return deCipher.doFinal(Base64.decode(pinToken)).base64Encode()
}

fun getRSAPrivateKeyFromString(privateKeyPEM: String): PrivateKey {
    val striped = stripRsaPrivateKeyHeaders(privateKeyPEM)
    val keySpec = PKCS8EncodedKeySpec(Base64.decode(striped))
    val kf = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        KeyFactory.getInstance("RSA")
    } else {
        KeyFactory.getInstance("RSA", "BC")
    }
    return kf.generatePrivate(keySpec)
}

private fun stripRsaPrivateKeyHeaders(privatePem: String): String {
    val strippedKey = StringBuilder()
    val lines = privatePem.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    lines.filter { line ->
        !line.contains("BEGIN RSA PRIVATE KEY") &&
            !line.contains("END RSA PRIVATE KEY") && !line.trim { it <= ' ' }.isEmpty()
    }
        .forEach { line -> strippedKey.append(line.trim { it <= ' ' }) }
    return strippedKey.toString().trim { it <= ' ' }
}
