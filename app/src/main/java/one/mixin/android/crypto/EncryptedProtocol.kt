package one.mixin.android.crypto

import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import one.mixin.android.extension.toByteArray
import one.mixin.android.extension.toLeByteArray
import java.util.UUID

class EncryptedProtocol {

    fun encryptMessage(seed: ByteArray, plaintext: ByteArray, otherPublicKey: ByteArray, otherSessionId: String): ByteArray {
        val privateKey = EdDSAPrivateKeySpec(seed, ed25519)
        val aesGcmKey = generateAesKey()
        val encryptedMessageData = aesGcmEncrypt(plaintext, aesGcmKey)
        val messageKey = encryptCipherMessageKey(seed, otherPublicKey, aesGcmKey)
        val messageKeyWithSession = UUID.fromString(otherSessionId).toByteArray().plus(messageKey)
        val pub = EdDSAPublicKey(EdDSAPublicKeySpec(privateKey.a, ed25519))
        val senderPublicKey = publicKeyToCurve25519(pub)
        val version = byteArrayOf(0x01)
        return version.plus(toLeByteArray(1.toUInt())).plus(senderPublicKey).plus(messageKeyWithSession).plus(encryptedMessageData)
    }

    private fun encryptCipherMessageKey(seed: ByteArray, publicKey: ByteArray, aesGcmKey: ByteArray): ByteArray {
        val private = privateKeyToCurve25519(seed)
        val sharedSecret = calculateAgreement(publicKey, private)
        return aesEncrypt(sharedSecret, aesGcmKey)
    }

    private fun decryptCipherMessageKey(seed: ByteArray, publicKey: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val private = privateKeyToCurve25519(seed)
        val sharedSecret = calculateAgreement(publicKey, private)
        return aesDecrypt(sharedSecret, iv, ciphertext)
    }

    fun decryptMessage(seed: ByteArray, ciphertext: ByteArray) {
        val version = ciphertext[0]
        val sessionSize = ciphertext.slice(IntRange(1, 2))
        val senderPublicKey = ciphertext.slice(IntRange(3, 34))
        val sessionId = ciphertext.slice(IntRange(35, 51))
    }
}
