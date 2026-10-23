package one.mixin.android.crypto

import one.mixin.android.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UtxoAddressRequirementTest {
    private val btc = Constants.ChainId.BITCOIN_CHAIN_ID
    private val pearl = Constants.ChainId.PEARL_CHAIN_ID

    @Test
    fun missingPearlRequiresAddressUpdate() {
        assertTrue(
            CryptoWalletHelper.hasMissingUtxoAddress(
                chainIds = setOf(btc),
            )
        )
    }

    @Test
    fun missingPearlTakesPriorityOverMissingBitcoin() {
        assertEquals(
            CryptoWalletHelper.MissingUtxoAddress.PEARL,
            CryptoWalletHelper.missingUtxoAddress(
                chainIds = emptySet(),
            )
        )
    }

    @Test
    fun missingBitcoinUsesTheExistingFlowWhenPearlExists() {
        assertEquals(
            CryptoWalletHelper.MissingUtxoAddress.BITCOIN,
            CryptoWalletHelper.missingUtxoAddress(
                chainIds = setOf(pearl),
            )
        )
    }

    @Test
    fun missingBitcoinAlwaysRequiresAddressUpdate() {
        assertTrue(
            CryptoWalletHelper.hasMissingUtxoAddress(
                chainIds = setOf(pearl),
            )
        )
    }

    @Test
    fun completeUtxoAddressSetDoesNotRequireUpdate() {
        assertFalse(
            CryptoWalletHelper.hasMissingUtxoAddress(
                chainIds = setOf(btc, pearl, Constants.ChainId.Litecoin),
            )
        )
    }

    @Test
    fun missingLitecoinRequiresUnlockLikePearl() {
        assertTrue(CryptoWalletHelper.hasMissingUtxoAddress(setOf(btc, pearl)))
        assertEquals(CryptoWalletHelper.MissingUtxoAddress.LITECOIN, CryptoWalletHelper.missingUtxoAddress(setOf(btc, pearl)))
    }

    @Test
    fun completeAddressSetDoesNotShowUnlockPrompt() {
        assertEquals(null, CryptoWalletHelper.missingUtxoAddress(setOf(btc, pearl, Constants.ChainId.Litecoin)))
    }

    @Test
    fun pearlPathCanRestoreImportedWalletIndex() {
        assertEquals(
            9,
            CryptoWalletHelper.extractIndexFromPaths(
                listOf(null, "m/86'/808276'/0'/0/9"),
            )
        )
    }

    @Test
    fun litecoinPathCanRestoreImportedWalletIndexExactly() {
        assertEquals(7, CryptoWalletHelper.extractIndexFromPath("m/84'/2'/0'/0/7"))
        assertEquals(null, CryptoWalletHelper.extractIndexFromPath("m/84'/2'/1'/0/7"))
        assertEquals(null, CryptoWalletHelper.extractIndexFromPath("m/84'/2'/0'/1/7"))
        assertEquals(null, CryptoWalletHelper.extractIndexFromPath("m/84'/2'/0'/0/-1"))
    }
}
