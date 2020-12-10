package one.mixin.android.util

import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.math.GroupElement
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import one.mixin.android.crypto.ed25519
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.crypto.privateKeyToCurve25519
import one.mixin.android.crypto.publicKeyToCurve25519
import org.junit.Test

class CryptoUtilTest {

    @Test
    fun `test Curve25519 conversion`() {
        val keyPair = generateEd25519KeyPair()
        val seed = (keyPair.private as EdDSAPrivateKey).seed
        val privateSpec = EdDSAPrivateKeySpec(seed, ed25519)
        val privateKey = EdDSAPrivateKey(privateSpec)
        val publicKey = EdDSAPublicKey(EdDSAPublicKeySpec(privateSpec.a, ed25519))

        // val curve25519PrivateKey = privateKeyToCurve25519(privateKey.seed)
        // val basePoint = byteArrayOf(9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        // val curve25519Public = ByteArray(32)
        // scalarmult.crypto_scalarmult(curve25519Public, curve25519PrivateKey, basePoint)
        // val curve25519Public1 = publicKeyToCurve25519(publicKey)
        //
        // assert(curve25519Public.contentEquals(curve25519Public1))

        val curve25519PrivateKey = privateKeyToCurve25519(privateKey.seed)
        val ge = ed25519.curve.getZero(GroupElement.Representation.P3)
        val edPublic = ge.scalarMultiply(curve25519PrivateKey)

        // assert(publicKey.abyte.contentEquals(edPublic.toByteArray()))

        val curve25519Public = publicKeyToCurve25519(publicKey)
        assert(curve25519Public.contentEquals(edPublic.toByteArray()))
    }
}