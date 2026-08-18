package one.mixin.android.crypto

import org.bitcoinj.base.Bech32
import org.bitcoinj.crypto.ECKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class PearlKeyGeneratorTest {
    @Test
    fun derivesDistinctAddressesForSeedIndexes() {
        val seed = ByteArray(32) { index -> (index + 1).toByte() }
        val firstAddress =
            PearlKeyGenerator.privateKeyToAddress(
                PearlKeyGenerator.getPrivateKeyFromSeed(seed, index = 1),
            )
        val secondAddress =
            PearlKeyGenerator.privateKeyToAddress(
                PearlKeyGenerator.getPrivateKeyFromSeed(seed, index = 2),
            )

        assertNotEquals(firstAddress, secondAddress)
    }

    @Test
    fun rejectsDerivedZeroPrivateKey() {
        val curveOrder = ECKey.ecDomainParameters().n

        assertNull(
            PearlKeyGenerator.deriveChildPrivateKeyValue(
                parentValue = BigInteger.ONE,
                leftValue = curveOrder.subtract(BigInteger.ONE),
            )
        )
    }

    @Test
    fun rejectsTaprootAddressWithNonZeroPadding() {
        val validAddress = "prl1p5rg2k5twnlggzdqhcw994xkgwqfuvvwhjnjrx84xsv9f834r887q4j2j5p"
        assertTrue(PearlKeyGenerator.isAddressValid(validAddress))
        val words = Bech32.decode(validAddress).bytes()
        words[words.lastIndex] = (words.last().toInt() or 1).toByte()
        val invalidAddress = Bech32.encode(
            Bech32.Encoding.BECH32M,
            "prl",
            object : Bech32.Bech32Bytes(words) {},
        )

        assertFalse(PearlKeyGenerator.isAddressValid(invalidAddress))
    }
}
