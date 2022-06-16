package one.mixin.android.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
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
        val password = "223388"

        val hashResult: Argon2KtResult = argon2Kt.hash(
            mode = Argon2Mode.ARGON2_I,
            password = password.toByteArray(),
            salt = "somesalt".toByteArray(),
            tCostInIterations = 1,
            mCostInKibibyte = 1024,
            hashLengthInBytes = 32
        )

        println("Raw hash: ${hashResult.rawHashAsHexadecimal()}")
        println("Encoded string: ${hashResult.encodedOutputAsString()}")

        val verificationResult = argon2Kt.verify(
            mode = Argon2Mode.ARGON2_I,
            encoded = hashResult.encodedOutputAsString(),
            password = password.toByteArray()
        )
        println(verificationResult)
        assert(verificationResult)
    }
}
