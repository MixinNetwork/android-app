package one.mixin.android.crypto

import one.mixin.android.Constants
import one.mixin.android.vo.WalletCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UtxoAddressRequirementTest {
    private val btc = Constants.ChainId.BITCOIN_CHAIN_ID
    private val pearl = Constants.ChainId.PEARL_CHAIN_ID

    @Test
    fun classicDefaultWalletRequiresPearl() {
        assertTrue(
            CryptoWalletHelper.hasMissingUtxoAddress(
                chainIds = setOf(btc),
                walletCategory = WalletCategory.CLASSIC.value,
                derivationIndex = 0,
            )
        )
    }

    @Test
    fun missingPearlTakesPriorityOverMissingBitcoin() {
        assertEquals(
            CryptoWalletHelper.MissingUtxoAddress.PEARL,
            CryptoWalletHelper.missingUtxoAddress(
                chainIds = emptySet(),
                walletCategory = WalletCategory.CLASSIC.value,
                derivationIndex = 0,
            )
        )
    }

    @Test
    fun missingBitcoinUsesTheExistingFlowWhenPearlExists() {
        assertEquals(
            CryptoWalletHelper.MissingUtxoAddress.BITCOIN,
            CryptoWalletHelper.missingUtxoAddress(
                chainIds = setOf(pearl),
                walletCategory = WalletCategory.CLASSIC.value,
                derivationIndex = 0,
            )
        )
    }

    @Test
    fun importedMnemonicWalletRequiresPearlAtAnyIndex() {
        assertTrue(
            CryptoWalletHelper.hasMissingUtxoAddress(
                chainIds = setOf(btc),
                walletCategory = WalletCategory.IMPORTED_MNEMONIC.value,
                derivationIndex = 9,
            )
        )
    }

    @Test
    fun additionalClassicWalletDoesNotRequirePearl() {
        assertFalse(
            CryptoWalletHelper.hasMissingUtxoAddress(
                chainIds = setOf(btc),
                walletCategory = WalletCategory.CLASSIC.value,
                derivationIndex = 1,
            )
        )
    }

    @Test
    fun missingBitcoinAlwaysRequiresAddressUpdate() {
        assertTrue(
            CryptoWalletHelper.hasMissingUtxoAddress(
                chainIds = setOf(pearl),
                walletCategory = WalletCategory.CLASSIC.value,
                derivationIndex = 0,
            )
        )
    }

    @Test
    fun completeUtxoAddressSetDoesNotRequireUpdate() {
        assertFalse(
            CryptoWalletHelper.hasMissingUtxoAddress(
                chainIds = setOf(btc, pearl),
                walletCategory = WalletCategory.CLASSIC.value,
                derivationIndex = 0,
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
