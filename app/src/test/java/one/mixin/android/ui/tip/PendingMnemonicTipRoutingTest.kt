package one.mixin.android.ui.tip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import one.mixin.android.vo.WalletCategory

class PendingMnemonicTipRoutingTest {
    @Test
    fun pendingMnemonicImportStillCreatesClassicWallet() {
        assertTrue(shouldCreateClassicWalletAfterTip(TipType.Create))
        assertTrue(shouldCreateClassicWalletAfterTip(TipType.Upgrade))
    }

    @Test
    fun nonImportTipStillCreatesClassicWallet() {
        assertTrue(shouldCreateClassicWalletAfterTip(TipType.Create))
        assertTrue(shouldCreateClassicWalletAfterTip(TipType.Upgrade))
        assertFalse(shouldCreateClassicWalletAfterTip(TipType.Change))
    }

    @Test
    fun classicWalletRoutesPendingImportToWalletHome() {
        assertEquals(
            PendingMnemonicRoute.WalletHome,
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
    fun failedWalletFetchKeepsPendingImportForLaterRetry() {
        assertEquals(
            PendingMnemonicRoute.WalletFetchFailed,
            routePendingMnemonicAfterWalletFetch(null),
        )
    }

    @Test
    fun nonClassicWalletsRoutePendingImportToImportPage() {
        assertEquals(
            PendingMnemonicRoute.ImportMnemonic,
            routePendingMnemonicAfterWalletFetch(
                listOf(
                    WalletCategory.IMPORTED_MNEMONIC.value,
                    WalletCategory.WATCH_ADDRESS.value,
                    WalletCategory.MIXIN_SAFE.value,
                ),
            ),
        )
    }
}
