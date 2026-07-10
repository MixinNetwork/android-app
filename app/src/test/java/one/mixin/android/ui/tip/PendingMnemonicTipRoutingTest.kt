package one.mixin.android.ui.tip

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun successfulLocalSaveClearsPendingAndRoutesToWalletHome() = runBlocking {
        var cleared = false
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(testWallet("imported-id", WalletCategory.IMPORTED_MNEMONIC.value)),
            pin = "123456",
            pendingWords = listOf("word"),
            save = { _, _, _ -> true },
            clear = { cleared = true },
        )

        assertEquals(PendingMnemonicResolution.WalletHome("imported-id"), result)
        assertTrue(cleared)
    }

    @Test
    fun failedLocalSaveDoesNotClearPendingOrRouteToWalletHome() = runBlocking {
        var cleared = false
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(testWallet("imported-id", WalletCategory.IMPORTED_MNEMONIC.value)),
            pin = "123456",
            pendingWords = listOf("word"),
            save = { _, _, _ -> false },
            clear = { cleared = true },
        )

        assertEquals(PendingMnemonicResolution.LocalSaveFailed, result)
        assertFalse(cleared)
    }

    @Test
    fun localSaveExceptionDoesNotClearPendingOrRouteToWalletHome() = runBlocking {
        var cleared = false
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(testWallet("imported-id", WalletCategory.IMPORTED_MNEMONIC.value)),
            pin = "123456",
            pendingWords = listOf("word"),
            save = { _, _, _ -> error("save failed") },
            clear = { cleared = true },
        )

        assertEquals(PendingMnemonicResolution.LocalSaveFailed, result)
        assertFalse(cleared)
    }

    @Test
    fun missingPinRequestsVerificationWithoutSaving() = runBlocking {
        var saved = false
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(testWallet("imported-id", WalletCategory.IMPORTED_MNEMONIC.value)),
            pin = null,
            pendingWords = listOf("word"),
            save = { _, _, _ ->
                saved = true
                true
            },
            clear = {},
        )

        assertEquals(PendingMnemonicResolution.NeedPin, result)
        assertFalse(saved)
    }

    @Test
    fun missingImportedWalletRoutesToImportWithoutSaving() = runBlocking {
        var saved = false
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(testWallet("classic-id", WalletCategory.CLASSIC.value)),
            pin = "123456",
            pendingWords = listOf("word"),
            save = { _, _, _ ->
                saved = true
                true
            },
            clear = {},
        )

        assertEquals(PendingMnemonicResolution.ImportMnemonic, result)
        assertFalse(saved)
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
