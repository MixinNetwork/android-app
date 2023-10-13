package one.mixin.android.jwt

import io.jsonwebtoken.EdDSAPrivateKey
import io.jsonwebtoken.EdDSAPublicKey
import okio.ByteString.Companion.toByteString
import one.mixin.android.Constants.DELAY_SECOND
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.crypto.generateRSAKeyPair
import one.mixin.android.crypto.getPrivateKeyPem
import one.mixin.android.crypto.getRSAPrivateKeyFromString
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
    fun testLegacyJwtRSA() {
        val rsaKey = generateRSAKeyPair(1024)
        val token = rsaKey.getPrivateKeyPem()
        val key = getRSAPrivateKeyFromString(token)
        val account = mockAccount()
        val signToken =
            Session.signLegacyToken(account, mockRequest(), UUID.randomUUID().toString(), key)
        val isExpire = Session.requestLegacyDelay(account, signToken, DELAY_SECOND, key).isExpire
        assertFalse(isExpire)
        Thread.sleep(2000)
        assertTrue(Session.requestLegacyDelay(account, signToken, 1, key).isExpire)
    }

    @Test
    fun testLegacyJwtRSADefault() {
        val rsaKey = generateRSAKeyPair()
        val token = rsaKey.getPrivateKeyPem()
        val key = getRSAPrivateKeyFromString(token)
        val account = mockAccount()
        val signToken =
            Session.signLegacyToken(account, mockRequest(), UUID.randomUUID().toString(), key)
        val isExpire = Session.requestLegacyDelay(account, signToken, DELAY_SECOND, key).isExpire
        assertFalse(isExpire)
        Thread.sleep(2000)
        assertTrue(Session.requestLegacyDelay(account, signToken, 1, key).isExpire)
    }

    @Test
    fun testLegacyJwtEdDSA() {
        val keyPair = generateEd25519KeyPair()
        val privateKey = EdDSAPrivateKey(keyPair.privateKey.toByteString())
        val publicKey = EdDSAPublicKey(keyPair.publicKey.toByteString())
        val account = mockAccount()
        val signToken = Session.signLegacyToken(account, mockRequest(), UUID.randomUUID().toString(), privateKey)
        val isExpire = Session.requestLegacyDelay(account, signToken, DELAY_SECOND, publicKey).isExpire
        assertFalse(isExpire)
        Thread.sleep(2000)
        assertTrue(Session.requestLegacyDelay(account, signToken, 1, publicKey).isExpire)
    }

    @Test
    fun testGoJwtRSA() {
        val rsaKey = generateRSAKeyPair(1024)
        val account = mockAccount()
        val signToken = Session.signGoToken(account, mockRequest(), UUID.randomUUID().toString(), rsaKey.private.encoded.toPem(true).toByteArray())
        val isExpire = Session.requestGoDelay(account, signToken, DELAY_SECOND, rsaKey.public.encoded.toPem(false).toByteArray()).isExpire
        assertFalse(isExpire)
        Thread.sleep(2000)
        assertTrue(Session.requestGoDelay(account, signToken, 1, rsaKey.public.encoded.toPem(false).toByteArray()).isExpire)
    }

    @Test
    fun testGoJwtRSADefault() {
        val rsaKey = generateRSAKeyPair()
        val account = mockAccount()
        val signToken = Session.signGoToken(account, mockRequest(), UUID.randomUUID().toString(), rsaKey.private.encoded.toPem(true).toByteArray())
        val isExpire = Session.requestGoDelay(account, signToken, DELAY_SECOND, rsaKey.public.encoded.toPem(false).toByteArray()).isExpire
        assertFalse(isExpire)
        Thread.sleep(2000)
        assertTrue(Session.requestGoDelay(account, signToken, 1, rsaKey.public.encoded.toPem(false).toByteArray()).isExpire)
    }

    @Test
    fun testGoJwtEdDSA() {
        val keyPair = generateEd25519KeyPair()
        val account = mockAccount()
        val signToken = Session.signGoToken(account, mockRequest(), UUID.randomUUID().toString(), keyPair.privateKey)
        val isExpire = Session.requestGoDelay(account, signToken, DELAY_SECOND, keyPair.publicKey).isExpire
        assertFalse(isExpire)
        Thread.sleep(2000)
        assertTrue(Session.requestGoDelay(account, signToken, 1, keyPair.publicKey).isExpire)
    }

    private fun ByteArray.toPem(private: Boolean): String {
        return if (private) {
            "-----BEGIN RSA PRIVATE KEY-----\n" + this.base64Encode() + "\n-----END RSA PRIVATE KEY-----\n"
        } else {
            "-----BEGIN PUBLIC KEY-----\n" + this.base64Encode() + "\n-----END PUBLIC KEY-----\n"
        }
    }
}
