package one.mixin.android.ui.tip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.vo.WalletCategory

class PendingMnemonicTipRoutingTest {
    @Test
    fun tipFlowCreatesClassicWalletAfterSafeRegistration() {
        assertTrue(shouldCreateClassicWalletAfterTip(TipType.Create))
    }

    @Test
    fun classicWalletDoesNotCompletePendingImport() {
        assertEquals(
            PendingMnemonicRoute.ImportMnemonic,
            routePendingMnemonicAfterWalletFetch(listOf(WalletCategory.CLASSIC.value)),
        )
    }

    @Test
    fun emptyWalletFetchRoutesPendingImportToImportPage() {
        assertEquals(
            PendingMnemonicRoute.ImportMnemonic,
            routePendingMnemonicAfterWalletFetch(emptyList()),
        )
    }

    @Test
    fun failedWalletFetchRoutesPendingImportToFetchPage() {
        assertEquals(
            PendingMnemonicRoute.ImportMnemonic,
            routePendingMnemonicAfterWalletFetch(null),
        )
    }

    @Test
    fun importedMnemonicWalletRoutesPendingImportToWalletHome() {
        assertEquals(
            PendingMnemonicRoute.WalletHome,
            routePendingMnemonicAfterWalletFetch(listOf(WalletCategory.IMPORTED_MNEMONIC.value)),
        )
    }

    @Test
    fun nonImportedMnemonicWalletsRoutePendingImportToImportPage() {
        assertEquals(
            PendingMnemonicRoute.ImportMnemonic,
            routePendingMnemonicAfterWalletFetch(
                listOf(
                    WalletCategory.CLASSIC.value,
                    WalletCategory.WATCH_ADDRESS.value,
                    WalletCategory.MIXIN_SAFE.value,
                ),
            ),
        )
    }

    @Test
    fun importedMnemonicWalletIdForPendingImportReturnsFirstImportedMnemonicWallet() {
        assertEquals(
            "imported-id",
            importedMnemonicWalletIdForPendingImport(
                listOf(
                    testWallet("classic-id", WalletCategory.CLASSIC.value),
                    testWallet("imported-id", WalletCategory.IMPORTED_MNEMONIC.value),
                ),
            ),
        )
    }

    @Test
    fun importedMnemonicWalletIdForPendingImportReturnsNullWhenNoImportedMnemonicWallet() {
        assertNull(
            importedMnemonicWalletIdForPendingImport(
                listOf(
                    testWallet("classic-id", WalletCategory.CLASSIC.value),
                    testWallet("watch-id", WalletCategory.WATCH_ADDRESS.value),
                ),
            ),
        )
        assertNull(importedMnemonicWalletIdForPendingImport(null))
    }

    private fun testWallet(
        id: String,
        category: String,
    ) = Web3Wallet(
        id = id,
        name = id,
        category = category,
        createdAt = "",
        updatedAt = "",
    )
}
