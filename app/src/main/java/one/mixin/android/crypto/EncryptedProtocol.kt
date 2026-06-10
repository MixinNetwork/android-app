package one.mixin.android.crypto

import one.mixin.android.extension.leByteArrayToInt
import one.mixin.android.extension.toByteArray
import one.mixin.android.extension.toLeByteArray
import java.util.UUID

class EncryptedProtocol {
    fun encryptMessage(
        keyPair: EdKeyPair,
        plaintext: ByteArray,
        otherPublicKey: ByteArray,
        otherSessionId: String,
        extensionSessionKey: ByteArray? = null,
        extensionSessionId: String? = null,
    ): ByteArray {
        val aesGcmKey = generateRandomBytes()
        val encryptedMessageData = aesGcmEncrypt(plaintext, aesGcmKey)
        val messageKey = encryptCipherMessageKey(keyPair.privateKey, otherPublicKey, aesGcmKey)
        val messageKeyWithSession = UUID.fromString(otherSessionId).toByteArray().plus(messageKey)
        val senderPublicKey = publicKeyToCurve25519(keyPair.publicKey)
        val version = byteArrayOf(0x01)

        // TODO Should try the encryptCipherMessageKey exception
        return if (extensionSessionId != null && extensionSessionKey != null && extensionSessionKey.isNotEmpty() && extensionSessionKey.size == 32) {
            version.plus(toLeByteArray(2.toUInt())).plus(senderPublicKey).let {
                val emergencyMessageKey =
                    encryptCipherMessageKey(keyPair.privateKey, extensionSessionKey, aesGcmKey)
                it.plus(UUID.fromString(extensionSessionId).toByteArray().plus(emergencyMessageKey))
            }.plus(messageKeyWithSession).plus(encryptedMessageData)
        } else {
            version.plus(toLeByteArray(1.toUInt())).plus(senderPublicKey)
                .plus(messageKeyWithSession)
                .plus(encryptedMessageData)
        }
    }

    private fun encryptCipherMessageKey(
        privateKey: ByteArray,
        publicKey: ByteArray,
        aesGcmKey: ByteArray,
    ): ByteArray {
        val private = privateKeyToCurve25519(privateKey)
        val sharedSecret = calculateAgreement(publicKey, private)
        return aesEncrypt(sharedSecret, aesGcmKey)
    }

    private fun decryptCipherMessageKey(
        privateKey: ByteArray,
        publicKey: ByteArray,
        iv: ByteArray,
        ciphertext: ByteArray,
    ): ByteArray {
        val private = privateKeyToCurve25519(privateKey)
        val sharedSecret = calculateAgreement(publicKey, private)
        return aesDecrypt(sharedSecret, iv, ciphertext)
    }

    fun decryptMessage(
        keyPair: EdKeyPair,
        sessionId: ByteArray,
        ciphertext: ByteArray,
    ): ByteArray {
        val sessionSize = leByteArrayToInt(ciphertext.slice(IntRange(1, 2)).toByteArray()).toInt()
        val senderPublicKey = ciphertext.slice(IntRange(3, 34)).toByteArray()
        var key: ByteArray? = null
        repeat(sessionSize) {
            val offset = it * 64
            val sid = ciphertext.slice(IntRange(35 + offset, 50 + offset)).toByteArray()
            if (sessionId.contentEquals(sid)) {
                key = ciphertext.slice(IntRange(51 + offset, 98 + offset)).toByteArray()
            }
        }
        val messageKey = requireNotNull(key)
        val message = ciphertext.slice(IntRange(35 + 64 * sessionSize, ciphertext.size - 1)).toByteArray()
        val iv = messageKey.slice(IntRange(0, 15)).toByteArray()
        val content = messageKey.slice(IntRange(16, messageKey.size - 1)).toByteArray()
        val decodedMessageKey = decryptCipherMessageKey(keyPair.privateKey, senderPublicKey, iv, content)

        return aesGcmDecrypt(message, decodedMessageKey)
    }
}
