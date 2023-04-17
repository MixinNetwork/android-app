@file:Suppress("NOTHING_TO_INLINE")

package one.mixin.android.crypto

import android.os.Build
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
import com.lambdapioneer.argon2kt.Argon2Mode
import io.jsonwebtoken.EdDSAPrivateKey
import okhttp3.tls.HeldCertificate
import okio.ByteString
import okio.ByteString.Companion.toByteString
import one.mixin.android.extension.base64Encode
import one.mixin.eddsa.Ed25519Sign
import one.mixin.eddsa.Field25519
import org.komputing.khash.keccak.KeccakParameter
import org.komputing.khash.keccak.extensions.digestKeccak
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
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.experimental.or

private val secureRandom: SecureRandom = SecureRandom()
private const val GCM_IV_LENGTH = 12

fun generateRSAKeyPair(keyLength: Int = 2048): KeyPair {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(keyLength)
    return kpg.genKeyPair()
}

fun generateEd25519KeyPair(): one.mixin.eddsa.KeyPair {
    return one.mixin.eddsa.KeyPair.newKeyPair()
}

fun calculateAgreement(publicKey: ByteArray, privateKey: ByteArray): ByteArray {
    return Curve25519.getInstance(BEST).calculateAgreement(publicKey, privateKey)
}

fun initFromSeedAndSign(seed: ByteArray, signTarget: ByteArray): ByteArray {
    val keyPair = one.mixin.eddsa.KeyPair.newKeyPairFromSeed(seed.toByteString())
    return signWithSk(EdDSAPrivateKey(keyPair.privateKey), signTarget)
}

private fun signWithSk(privateKey: EdDSAPrivateKey, signTarget: ByteArray): ByteArray {
    val signer = Ed25519Sign(privateKey.privateKey)
    return signer.sign(signTarget.toByteString()).toByteArray()
}

fun privateKeyToCurve25519(privateKey: ByteArray): ByteArray {
    val md = MessageDigest.getInstance("SHA-512")
    val h = md.digest(privateKey).sliceArray(IntRange(0, 31))
    h[0] = h[0] and 248.toByte()
    h[31] = h[31] and 127
    h[31] = h[31] or 64
    return h
}

fun publicKeyToCurve25519(publicKey: ByteArray): ByteArray {
    val p = publicKey.map { it.toInt().toByte() }.toByteArray()
    val x = edwardsToMontgomeryX(Field25519.expand(p))
    return Field25519.contract(x)
}
private fun edwardsToMontgomeryX(y: LongArray): LongArray {
    val oneMinusY = LongArray(Field25519.LIMB_CNT)
    oneMinusY[0] = 1
    Field25519.sub(oneMinusY, oneMinusY, y)
    Field25519.inverse(oneMinusY, oneMinusY)

    val outX = LongArray(Field25519.LIMB_CNT)
    outX[0] = 1
    Field25519.sum(outX, y)

    Field25519.mult(outX, outX, oneMinusY)
    return outX
}

fun seedFromPrivateKey(privateKey: ByteString): ByteArray = privateKey.substring(0, 32).toByteArray()

fun ByteArray.sha3Sum256(): ByteArray {
    return digestKeccak(KeccakParameter.SHA3_256)
}

fun Argon2Kt.argon2IdHash(pin: String, seed: String): Argon2KtResult =
    argon2IdHash(pin, seed.toByteArray())

fun Argon2Kt.argon2IdHash(pin: String, seed: ByteArray): Argon2KtResult {
    return hash(
        mode = Argon2Mode.ARGON2_I,
        password = pin.toByteArray(),
        salt = seed,
        tCostInIterations = 4,
        mCostInKibibyte = 1024,
        hashLengthInBytes = 32,
    )
}

fun generateEphemeralSeed(): ByteArray {
    val key = ByteArray(32)
    secureRandom.nextBytes(key)
    return key
}

fun generateAesKey(len: Int = 16): ByteArray {
    val key = ByteArray(len)
    secureRandom.nextBytes(key)
    return key
}

fun aesGcmEncrypt(plain: ByteArray, key: ByteArray): ByteArray {
    val iv = ByteArray(GCM_IV_LENGTH)
    secureRandom.nextBytes(iv)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val parameterSpec = GCMParameterSpec(128, iv) // 128 bit auth tag length
    val secretKey = SecretKeySpec(key, "AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
    val result = cipher.doFinal(plain)
    return iv.plus(result)
}

fun aesGcmDecrypt(cipherMessage: ByteArray, key: ByteArray): ByteArray {
    val secretKey = SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val gcmIv = GCMParameterSpec(128, cipherMessage, 0, GCM_IV_LENGTH)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmIv)
    return cipher.doFinal(cipherMessage, GCM_IV_LENGTH, cipherMessage.size - GCM_IV_LENGTH)
}

fun aesEncrypt(key: ByteArray, plain: ByteArray): ByteArray {
    val keySpec = SecretKeySpec(key, "AES")
    val iv = ByteArray(16)
    secureRandom.nextBytes(iv)
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
    val result = cipher.doFinal(plain)
    return iv.plus(result)
}

fun aesDecrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray {
    val keySpec = SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
    return cipher.doFinal(ciphertext)
}

fun aesDecrypt(key: ByteArray, ciphertext: ByteArray): ByteArray {
    val iv = ciphertext.slice(0..15).toByteArray()
    val cipherContent = ciphertext.slice(16 until ciphertext.size).toByteArray()
    return aesDecrypt(key, iv, cipherContent)
}

inline fun KeyPair.getPublicKey(): ByteArray {
    return public.encoded
}

inline fun KeyPair.getPrivateKeyPem(): String {
    val heldCertificate = HeldCertificate.Builder().keyPair(this).build()
    return heldCertificate.privateKeyPkcs1Pem()
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
            PSource.PSpecified(iv.toByteArray()),
        ),
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
