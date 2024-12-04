package one.mixin.android.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class Argon2Test {
    @Test
    fun testArgon2() {
        val argon2Kt = Argon2Kt()
        val password = "123456"

        val hashResult = argon2Kt.argon2IHash(password.toByteArray(), "2e613adae4f0167255933a3ec1d97e0acdd38e46d319c348b7a3d709f23bae8f")
        println("Raw hash: ${hashResult.rawHashAsHexadecimal()}")
        println("Encoded string: ${hashResult.encodedOutputAsString()}")

        val verificationResult =
            argon2Kt.verify(
                mode = Argon2Mode.ARGON2_I,
                encoded = hashResult.encodedOutputAsString(),
                password = password.toByteArray(),
            )
        println(verificationResult)
        assert(verificationResult)
    }
}
