package one.mixin.android.crypto

import one.mixin.android.Constants
import one.mixin.android.extension.hexStringToByteArray
import org.bitcoinj.base.Bech32
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtxoKeyGeneratorTest {
    @Test
    fun derivesDistinctPearlAddressesForSeedIndexes() {
        val seed = ByteArray(32) { index -> (index + 1).toByte() }
        val firstAddress =
            UtxoKeyGenerator.privateKeyToAddress(
                UtxoKeyGenerator.getPrivateKeyFromSeed(
                    seed,
                    Constants.ChainId.PEARL_CHAIN_ID,
                    index = 1,
                ),
                Constants.ChainId.PEARL_CHAIN_ID,
            )
        val secondAddress =
            UtxoKeyGenerator.privateKeyToAddress(
                UtxoKeyGenerator.getPrivateKeyFromSeed(
                    seed,
                    Constants.ChainId.PEARL_CHAIN_ID,
                    index = 2,
                ),
                Constants.ChainId.PEARL_CHAIN_ID,
            )

        assertNotEquals(firstAddress, secondAddress)
    }

    @Test
    fun generatesBitcoinAndPearlAddressesWithOneGenerator() {
        val seed = ByteArray(32) { index -> (index + 1).toByte() }
        val bitcoinPrivateKey = UtxoKeyGenerator.getPrivateKeyFromSeed(
            seed,
            Constants.ChainId.BITCOIN_CHAIN_ID,
        )
        val pearlPrivateKey = UtxoKeyGenerator.getPrivateKeyFromSeed(
            seed,
            Constants.ChainId.PEARL_CHAIN_ID,
        )
        val bitcoinAddress = UtxoKeyGenerator.privateKeyToAddress(
            bitcoinPrivateKey,
            Constants.ChainId.BITCOIN_CHAIN_ID,
        )
        val pearlAddress = UtxoKeyGenerator.privateKeyToAddress(
            pearlPrivateKey,
            Constants.ChainId.PEARL_CHAIN_ID,
        )

        assertEquals(32, bitcoinPrivateKey.size)
        assertEquals(32, pearlPrivateKey.size)
        assertTrue(bitcoinAddress.startsWith("bc1"))
        assertTrue(pearlAddress.startsWith("prl1"))
        assertTrue(UtxoKeyGenerator.isAddressValid(bitcoinAddress, Constants.ChainId.BITCOIN_CHAIN_ID))
        assertTrue(UtxoKeyGenerator.isAddressValid(pearlAddress, Constants.ChainId.PEARL_CHAIN_ID))
        assertNotEquals(bitcoinAddress, pearlAddress)
    }

    @Test
    fun usesStandardBip32ForPearlHardenedDerivation() {
        val seed = (
            "1d00304f6e8daccbea0e2d4c6b8aa9c8e70b2a496887a6c5e40827466584a3c2" +
                "e10524436281a0bfde0221405f7e9dbcdbfa1e3d5c7b9ab9d8f71b3a597897b6"
        ).hexStringToByteArray()
        val privateKey = UtxoKeyGenerator.getPrivateKeyFromSeed(
            seed,
            Constants.ChainId.PEARL_CHAIN_ID,
        )

        assertEquals(
            "prl1p54m4h2agaw92ce58havk6n9l82qgnp8nyyfhad3ycmddsym2rqwqhs07z6",
            UtxoKeyGenerator.privateKeyToAddress(privateKey, Constants.ChainId.PEARL_CHAIN_ID),
        )
    }

    @Test
    fun rejectsTaprootAddressWithNonZeroPadding() {
        val validAddress = "prl1p5rg2k5twnlggzdqhcw994xkgwqfuvvwhjnjrx84xsv9f834r887q4j2j5p"
        assertTrue(UtxoKeyGenerator.isAddressValid(validAddress, Constants.ChainId.PEARL_CHAIN_ID))
        val words = Bech32.decode(validAddress).bytes()
        words[words.lastIndex] = (words.last().toInt() or 1).toByte()
        val invalidAddress = Bech32.encode(
            Bech32.Encoding.BECH32M,
            "prl",
            object : Bech32.Bech32Bytes(words) {},
        )

        assertFalse(UtxoKeyGenerator.isAddressValid(invalidAddress, Constants.ChainId.PEARL_CHAIN_ID))
    }
}
