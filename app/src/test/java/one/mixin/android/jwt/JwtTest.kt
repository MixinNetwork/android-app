package one.mixin.android.jwt

import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import one.mixin.android.Constants.DELAY_SECOND
import one.mixin.android.crypto.ed25519
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.crypto.generateRSAKeyPair
import one.mixin.android.crypto.getPrivateKeyPem
import one.mixin.android.crypto.getRSAPrivateKeyFromString
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
        val token = rsaKey.getPrivateKeyPem()
        val key = getRSAPrivateKeyFromString(token)
        val account = mockAccount()
        val signToken = Session.signToken(account, mockRequest(), UUID.randomUUID().toString(), key)
        val isExpire = Session.requestDelay(account, signToken, DELAY_SECOND, key).isExpire
        assertFalse(isExpire)
        Thread.sleep(2000)
        assertTrue(Session.requestDelay(account, signToken, 1, key).isExpire)
    }

    @Test
    fun testJwtRSADefault() {
        val rsaKey = generateRSAKeyPair()
        val token = rsaKey.getPrivateKeyPem()
        val key = getRSAPrivateKeyFromString(token)
        val account = mockAccount()
        val signToken = Session.signToken(account, mockRequest(), UUID.randomUUID().toString(), key)
        val isExpire = Session.requestDelay(account, signToken, DELAY_SECOND, key).isExpire
        assertFalse(isExpire)
        Thread.sleep(2000)
        assertTrue(Session.requestDelay(account, signToken, 1, key).isExpire)
    }

    @Test
    fun testJwtEdDSA() {
        val keyPair = generateEd25519KeyPair()
        val seed = (keyPair.private as EdDSAPrivateKey).seed
        val privateSpec = EdDSAPrivateKeySpec(seed, ed25519)
        val privateKey = EdDSAPrivateKey(privateSpec)
        val publicKey = EdDSAPublicKey(EdDSAPublicKeySpec(privateSpec.a, ed25519))
        val account = mockAccount()
        val signToken = Session.signToken(account, mockRequest(), UUID.randomUUID().toString(), privateKey)
        val isExpire = Session.requestDelay(account, signToken, DELAY_SECOND, publicKey).isExpire
        assertFalse(isExpire)
        Thread.sleep(2000)
        assertTrue(Session.requestDelay(account, signToken, 1, publicKey).isExpire)
    }
}
