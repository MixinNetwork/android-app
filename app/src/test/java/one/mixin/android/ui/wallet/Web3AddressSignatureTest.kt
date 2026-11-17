package one.mixin.android.ui.wallet

import one.mixin.android.Constants
import org.bitcoinj.base.Sha256Hash
import org.bitcoinj.crypto.ECKey
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import java.math.BigInteger
import org.junit.Assert.assertTrue
import org.junit.Test
import org.web3j.utils.Numeric

class Web3AddressSignatureTest {
    private val privateKey = ByteArray(32).apply { this[lastIndex] = 2 }
    private val message = "address\nuser-id\n1700000000"

    @Test
    fun pearlUsesRecoverableP2pkhFlagForTaprootValidation() {
        val signature = signUtxoAddressMessage(
            privateKey = privateKey,
            message = message,
            chainId = Constants.ChainId.PEARL_CHAIN_ID,
        )

        assertTrue(recoveryFlag(signature) in 31..34)
    }

    @Test
    fun bitcoinKeepsBip137P2wpkhFlag() {
        val signature = signUtxoAddressMessage(
            privateKey = privateKey,
            message = message,
            chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
        )

        assertTrue(recoveryFlag(signature) in 39..42)
    }

    @Test
    fun litecoinSignatureUsesItsOwnMessagePrefixAndRecoversTheSigningKey() {
        val signed = Numeric.hexStringToByteArray(signUtxoAddressMessage(privateKey, message, Constants.ChainId.Litecoin))
        // Both strings are shorter than 253 bytes, so their compact lengths occupy one byte.
        val payload = byteArrayOf(25) + "Litecoin Signed Message:\n".toByteArray() +
            byteArrayOf(message.toByteArray().size.toByte()) + message.toByteArray()
        val hash = Sha256Hash.twiceOf(payload)
        val signature = ECKey.ECDSASignature(BigInteger(1, signed.copyOfRange(1, 33)), BigInteger(1, signed.copyOfRange(33, 65)))
        val recoveryId = (signed[0].toInt() and 0xff) - 39
        assertEquals(65, signed.size)
        assertTrue(recoveryId in 0..3)
        val recovered = requireNotNull(ECKey.recoverFromSignature(recoveryId, signature, hash, true))
        assertArrayEquals(ECKey.fromPrivate(privateKey, true).pubKey, recovered.pubKey)
        assertTrue(recovered.verify(hash, signature))
    }

    private fun recoveryFlag(signature: String): Int =
        Numeric.hexStringToByteArray(signature).first().toInt() and 0xff
}
