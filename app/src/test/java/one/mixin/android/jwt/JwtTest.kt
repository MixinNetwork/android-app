package one.mixin.android.jwt

import one.mixin.android.Constants.DELAY_SECOND
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.crypto.generateRSAKeyPair
import one.mixin.android.extension.base64Encode
import one.mixin.android.mock.mockAccount
import one.mixin.android.mock.mockRequest
import one.mixin.android.session.Session
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtTest {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun testJwtRSA() {
        val rsaKey = generateRSAKeyPair(1024)
        val account = mockAccount()
        val signToken = Session.signToken(account, mockRequest(), UUID.randomUUID().toString(), rsaKey.private.encoded.toPem(true).toByteArray())
        val isExpire = Session.requestDelay(account, signToken, DELAY_SECOND, rsaKey.public.encoded.toPem(false).toByteArray()).isExpire
        assertFalse(isExpire)
        Thread.sleep(2000)
        assertTrue(Session.requestDelay(account, signToken, 1, rsaKey.public.encoded.toPem(false).toByteArray()).isExpire)
    }

    @Test
    fun testJwtRSADefault() {
        val rsaKey = generateRSAKeyPair()
        val account = mockAccount()
        val signToken = Session.signToken(account, mockRequest(), UUID.randomUUID().toString(), rsaKey.private.encoded.toPem(true).toByteArray())
        val isExpire = Session.requestDelay(account, signToken, DELAY_SECOND, rsaKey.public.encoded.toPem(false).toByteArray()).isExpire
        assertFalse(isExpire)
        Thread.sleep(2000)
        assertTrue(Session.requestDelay(account, signToken, 1, rsaKey.public.encoded.toPem(false).toByteArray()).isExpire)
    }

    @Test
    fun testJwtEdDSA() {
        val keyPair = generateEd25519KeyPair()
        val account = mockAccount()
        val signToken = Session.signToken(account, mockRequest(), UUID.randomUUID().toString(), keyPair.privateKey)
        val isExpire = Session.requestDelay(account, signToken, DELAY_SECOND, keyPair.publicKey).isExpire
        assertFalse(isExpire)
        Thread.sleep(2000)
        assertTrue(Session.requestDelay(account, signToken, 1, keyPair.publicKey).isExpire)
    }

    private fun ByteArray.toPem(private: Boolean): String {
        return if (private) {
            "-----BEGIN RSA PRIVATE KEY-----\n" + this.base64Encode() + "\n-----END RSA PRIVATE KEY-----\n"
        } else {
            "-----BEGIN PUBLIC KEY-----\n" + this.base64Encode() + "\n-----END PUBLIC KEY-----\n"
        }
    }
}
