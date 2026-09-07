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
                chainIds = setOf(btc, pearl),
            )
        )
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
}
