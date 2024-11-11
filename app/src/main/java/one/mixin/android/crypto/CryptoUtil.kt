@file:Suppress("NOTHING_TO_INLINE")

package one.mixin.android.crypto

import android.content.Context
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
import com.lambdapioneer.argon2kt.Argon2Mode
import ed25519.Ed25519
import okhttp3.tls.HeldCertificate
import okio.ByteString.Companion.toByteString
import one.mixin.android.Constants.Tip.ENCRYPTED_MNEMONIC
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toHex
import one.mixin.android.util.InvalidEd25519Exception
import one.mixin.eddsa.Ed25519Sign
import one.mixin.eddsa.Ed25519Verify
import one.mixin.eddsa.Field25519
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation.createMasterPrivateKey
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

val secureRandom: SecureRandom = SecureRandom()
private const val GCM_IV_LENGTH = 12

// https://github.com/google/tink/issues/403
fun useGoEd() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1

fun generateRSAKeyPair(keyLength: Int = 2048): KeyPair {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(keyLength)
    return kpg.genKeyPair()
}

fun generateEd25519KeyPair(): EdKeyPair {
    return if (useGoEd()) {
        val priv = Ed25519.generateKey()
        EdKeyPair(priv.sliceArray(32..63), priv.sliceArray(0..31))
    } else {
        val keyPair = one.mixin.eddsa.KeyPair.newKeyPair(true)
        val ktEdKeyPair = EdKeyPair(keyPair.publicKey.toByteArray(), keyPair.privateKey.toByteArray())

        val priv = Ed25519.newKeyFromSeed(ktEdKeyPair.privateKey)
        val goEdKeyPair = EdKeyPair(priv.sliceArray(32..63), priv.sliceArray(0..31))
        if (!goEdKeyPair.privateKey.contentEquals(ktEdKeyPair.privateKey) || !goEdKeyPair.publicKey.contentEquals(ktEdKeyPair.publicKey)) {
            throw InvalidEd25519Exception()
        }
        ktEdKeyPair
    }
}

fun newKeyPairFromSeed(seed: ByteArray): EdKeyPair {
    val priv = Ed25519.newKeyFromSeed(seed)
    val goEdKeyPair = EdKeyPair(priv.sliceArray(32..63), priv.sliceArray(0..31))
    return if (useGoEd()) {
        goEdKeyPair
    } else {
        val keyPair = one.mixin.eddsa.KeyPair.newKeyPairFromSeed(seed.toByteString(), checkOnCurve = true)
        val ktEdKeyPair = EdKeyPair(keyPair.publicKey.toByteArray(), keyPair.privateKey.toByteArray())
        if (!goEdKeyPair.privateKey.contentEquals(ktEdKeyPair.privateKey) || !goEdKeyPair.publicKey.contentEquals(ktEdKeyPair.publicKey)) {
            throw InvalidEd25519Exception()
        }
        ktEdKeyPair
    }
}

fun newKeyPairFromMnemonic(mnemonic: String): EdKeyPair {
    val masterKey = newMasterPrivateKeyFromMnemonic(mnemonic)
    return newKeyPairFromSeed(masterKey.privKeyBytes)
}

fun newMasterPrivateKeyFromMnemonic(mnemonic: String): DeterministicKey {
    val seed = toSeed(mnemonic.split(" ").let { list ->
        when (list.size) {
            25 -> {
                list.subList(0, 24)
            }
            13 -> {
                list.subList(0, 12)
            }
            else -> {
                list
            }
        }
    }, "")
    val masterKeyPrivateKey = createMasterPrivateKey(seed)
    return masterKeyPrivateKey
}

fun calculateAgreement(
    publicKey: ByteArray,
    privateKey: ByteArray,
): ByteArray {
    return Curve25519.getInstance(BEST).calculateAgreement(publicKey, privateKey)
}

fun calculateSignature(
    privateKey: ByteArray,
    message: ByteArray,
): ByteArray {
    return Curve25519.getInstance(BEST).calculateSignature(privateKey, message)
}

fun initFromSeedAndSign(
    seed: ByteArray,
    signTarget: ByteArray,
): ByteArray {
    return if (useGoEd()) {
        Ed25519.sign(signTarget, seed)
    } else {
        val keyPair = newKeyPairFromSeed(seed)
        val signer = Ed25519Sign(keyPair.privateKey.toByteString(), checkOnCurve = true)
        signer.sign(signTarget.toByteString(), checkOnCurve = true).toByteArray()
    }
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
    return if (useGoEd()) {
        Ed25519.publicKeyToCurve25519(publicKey)
    } else {
        val p = publicKey.map { it.toInt().toByte() }.toByteArray()
        val x = edwardsToMontgomeryX(Field25519.expand(p))
        Field25519.contract(x)
    }
}

fun verifyCurve25519Signature(
    message: ByteArray,
    sig: ByteArray,
    pub: ByteArray,
): Boolean {
    return if (useGoEd()) {
        Ed25519.verify(message, sig, pub)
    } else {
        Ed25519Verify(pub.toByteString()).verify(sig.toByteString(), message.toByteString())
    }
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

fun String.sha3Sum256(): ByteArray {
    return digestKeccak(KeccakParameter.SHA3_256)
}

fun ByteArray.sha3Sum256(): ByteArray {
    return digestKeccak(KeccakParameter.SHA3_256)
}

fun Argon2Kt.argon2IHash(
    pin: String,
    seed: String,
): Argon2KtResult =
    argon2IHash(pin, seed.toByteArray())

fun Argon2Kt.argon2IHash(
    pin: String,
    seed: ByteArray,
): Argon2KtResult =
    argon2IHash(pin.toByteArray(), seed)

fun Argon2Kt.argon2IHash(
    pin: ByteArray,
    seed: ByteArray,
): Argon2KtResult {
    return hash(
        mode = Argon2Mode.ARGON2_I,
        password = pin,
        salt = seed,
        tCostInIterations = 4,
        mCostInKibibyte = 1024,
        hashLengthInBytes = 32,
    )
}

fun generateEphemeralSeed(): ByteArray = generateRandomBytes(32)

fun generateRandomBytes(len: Int = 16): ByteArray {
    val key = ByteArray(len)
    secureRandom.nextBytes(key)
    return key
}

fun aesGcmEncrypt(
    plain: ByteArray,
    key: ByteArray,
): ByteArray {
    val iv = ByteArray(GCM_IV_LENGTH)
    secureRandom.nextBytes(iv)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val parameterSpec = GCMParameterSpec(128, iv) // 128 bit auth tag length
    val secretKey = SecretKeySpec(key, "AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
    val result = cipher.doFinal(plain)
    return iv.plus(result)
}

fun aesGcmDecrypt(
    cipherMessage: ByteArray,
    key: ByteArray,
): ByteArray {
    val secretKey = SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val gcmIv = GCMParameterSpec(128, cipherMessage, 0, GCM_IV_LENGTH)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmIv)
    return cipher.doFinal(cipherMessage, GCM_IV_LENGTH, cipherMessage.size - GCM_IV_LENGTH)
}

fun aesEncrypt(
    key: ByteArray,
    plain: ByteArray,
): ByteArray {
    val keySpec = SecretKeySpec(key, "AES")
    val iv = ByteArray(16)
    secureRandom.nextBytes(iv)
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
    val result = cipher.doFinal(plain)
    return iv.plus(result)
}

fun aesDecrypt(
    key: ByteArray,
    iv: ByteArray,
    ciphertext: ByteArray,
): ByteArray {
    val keySpec = SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
    return cipher.doFinal(ciphertext)
}

fun aesDecrypt(
    key: ByteArray,
    ciphertext: ByteArray,
): ByteArray {
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

fun rsaDecrypt(
    privateKey: PrivateKey,
    iv: String,
    pinToken: String,
): String {
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
    val kf =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

fun storeMnemonicInEncryptedPreferences(context: Context, alias: String, entropy: ByteArray) {
    val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        ENCRYPTED_MNEMONIC,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val encodedKey = entropy.toHex()
    encryptedPrefs.edit().putString(alias, encodedKey).apply()
}

fun clearMnemonic(context: Context, alias: String) {
    val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        ENCRYPTED_MNEMONIC,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    encryptedPrefs.edit().remove(alias).apply()
}

fun getMnemonicFromEncryptedPreferences(context: Context, alias: String): ByteArray? {
    val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        ENCRYPTED_MNEMONIC,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val encodedText = encryptedPrefs.getString(alias, null) ?: return null
    return encodedText.hexStringToByteArray()
}