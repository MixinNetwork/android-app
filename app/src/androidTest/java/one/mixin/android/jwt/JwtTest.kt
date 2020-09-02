package one.mixin.android.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import one.mixin.android.Constants.DELAY_SECOND
import one.mixin.android.crypto.generateRSAKeyPair
import one.mixin.android.crypto.getPrivateKeyPem
import one.mixin.android.crypto.getRSAPrivateKeyFromString
import one.mixin.android.mock.mockAccount
import one.mixin.android.mock.mockRequest
import one.mixin.android.util.Session
import org.junit.Assert
import org.junit.Test

class JwtTest {

    @Test
    fun jwtTest() {
        val rsaKey = generateRSAKeyPair()
        val token = rsaKey.getPrivateKeyPem()
        val account = mockAccount()
        val signToken = Session.signToken(account, mockRequest(), token)
        val isDelay = Session.requestDelay(account, signToken, DELAY_SECOND)
        Assert.assertFalse(isDelay)
    }
}