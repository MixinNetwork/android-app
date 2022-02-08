package one.mixin.android.util

import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import one.mixin.android.crypto.ed25519
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.crypto.privateKeyToCurve25519
import one.mixin.android.crypto.publicKeyToCurve25519
import kotlin.test.Test

class CryptoUtilTest {

    @Test
    fun `test curve25519 conversion`() {
        val public = byteArrayOf(147.toByte(), 193.toByte(), 19.toByte(), 201.toByte(), 96.toByte(), 200.toByte(), 216.toByte(), 248.toByte(), 19.toByte(), 54.toByte(), 49.toByte(), 150.toByte(), 150.toByte(), 167.toByte(), 41.toByte(), 75.toByte(), 87.toByte(), 242.toByte(), 28.toByte(), 199.toByte(), 153.toByte(), 217.toByte(), 6.toByte(), 224.toByte(), 84.toByte(), 169.toByte(), 210.toByte(), 80.toByte(), 186.toByte(), 202.toByte(), 128.toByte(), 201.toByte())
        val seed = byteArrayOf(126.toByte(), 51.toByte(), 73.toByte(), 128.toByte(), 30.toByte(), 5.toByte(), 236.toByte(), 244.toByte(), 27.toByte(), 127.toByte(), 26.toByte(), 150.toByte(), 49.toByte(), 250.toByte(), 179.toByte(), 252.toByte(), 107.toByte(), 36.toByte(), 94.toByte(), 118.toByte(), 231.toByte(), 79.toByte(), 230.toByte(), 175.toByte(), 74.toByte(), 217.toByte(), 163.toByte(), 61.toByte(), 162.toByte(), 214.toByte(), 235.toByte(), 156.toByte())
        val targetPrivate = byteArrayOf(232.toByte(), 23.toByte(), 164.toByte(), 168.toByte(), 212.toByte(), 159.toByte(), 250.toByte(), 121.toByte(), 48.toByte(), 244.toByte(), 252.toByte(), 13.toByte(), 183.toByte(), 100.toByte(), 82.toByte(), 162.toByte(), 219.toByte(), 106.toByte(), 10.toByte(), 171.toByte(), 30.toByte(), 240.toByte(), 31.toByte(), 208.toByte(), 91.toByte(), 201.toByte(), 15.toByte(), 179.toByte(), 136.toByte(), 192.toByte(), 210.toByte(), 87.toByte())
        val targetPublic = byteArrayOf(159.toByte(), 128.toByte(), 169.toByte(), 96.toByte(), 138.toByte(), 29.toByte(), 242.toByte(), 209.toByte(), 248.toByte(), 250.toByte(), 1.toByte(), 148.toByte(), 133.toByte(), 194.toByte(), 107.toByte(), 237.toByte(), 154.toByte(), 18.toByte(), 40.toByte(), 50.toByte(), 51.toByte(), 58.toByte(), 81.toByte(), 213.toByte(), 200.toByte(), 152.toByte(), 8.toByte(), 126.toByte(), 7.toByte(), 140.toByte(), 6.toByte(), 47.toByte())

        val publicKey = EdDSAPublicKey(EdDSAPublicKeySpec(public, ed25519))

        val curve25519PrivateKey = privateKeyToCurve25519(seed)
        assert(curve25519PrivateKey.contentEquals(targetPrivate))
        val curve25519PublicKey = publicKeyToCurve25519(publicKey)
        assert(curve25519PublicKey.contentEquals(targetPublic))
    }

    @Test
    fun `test curve25519`() {
        val keyPair = generateEd25519KeyPair()
        val publicKey = keyPair.public as EdDSAPublicKey
        val bytes = publicKeyToCurve25519(publicKey)
        assert(verifyPubkey(bytes))
    }
}
