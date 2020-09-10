package one.mixin.android.jwt

import one.mixin.android.Constants.DELAY_SECOND
import one.mixin.android.crypto.generateRSAKeyPair
import one.mixin.android.crypto.getPrivateKeyPem
import one.mixin.android.mock.mockAccount
import one.mixin.android.mock.mockRequest
import one.mixin.android.util.Session
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JwtTest {

    @Test
    fun jwtTest() {
        val rsaKey = generateRSAKeyPair()
        val token = rsaKey.getPrivateKeyPem()
        val account = mockAccount()
        val signToken = Session.signToken(account, mockRequest(), token)
        val isExpire = Session.requestDelay(account, signToken, DELAY_SECOND, token).isExpire
        assertFalse(isExpire)
        Thread.sleep(2000)
        assertTrue(Session.requestDelay(account, signToken, 1, token).isExpire)
    }
}
