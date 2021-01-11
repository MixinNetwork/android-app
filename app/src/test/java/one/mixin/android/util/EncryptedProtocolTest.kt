package one.mixin.android.util

import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import one.mixin.android.crypto.EncryptedProtocol
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.crypto.publicKeyToCurve25519
import java.util.UUID
import kotlin.test.Test

class EncryptedProtocolTest {

    @Test
    fun testEncryptAndDecrypt() {
        val content = "L".toByteArray()
        val otherSessionId = UUID.randomUUID().toString()

        val encryptedProtocol = EncryptedProtocol()

        val senderKeyPair = generateEd25519KeyPair()
        val senderPrivateKey = senderKeyPair.private as EdDSAPrivateKey

        val receiverKeyPair = generateEd25519KeyPair()
        val receiverPrivateKey = receiverKeyPair.private as EdDSAPrivateKey
        val receiverPublicKey = receiverKeyPair.public as EdDSAPublicKey
        val receiverCurvePublicKey = publicKeyToCurve25519(receiverPublicKey)

        val encodedContent = encryptedProtocol.encryptMessage(senderPrivateKey, content, receiverCurvePublicKey, otherSessionId)

        val decryptedContent = encryptedProtocol.decryptMessage(receiverPrivateKey, encodedContent)

        assert(decryptedContent.contentEquals(content))
    }
}
