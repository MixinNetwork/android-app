package one.mixin.android.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.whispersystems.curve25519.Curve25519

@RunWith(AndroidJUnit4::class)
class Curve25519NativeTest {
    @Test
    fun nativeProviderSupportsKeyAgreementAndSignatures() {
        val curve = Curve25519.getInstance(Curve25519.BEST)
        val alice = curve.generateKeyPair()
        val bob = curve.generateKeyPair()
        val message = "curve25519-native".toByteArray()

        assertTrue(curve.isNative)
        assertArrayEquals(
            curve.calculateAgreement(bob.publicKey, alice.privateKey),
            curve.calculateAgreement(alice.publicKey, bob.privateKey),
        )

        val signature = curve.calculateSignature(alice.privateKey, message)
        assertTrue(curve.verifySignature(alice.publicKey, message, signature))
    }
}
