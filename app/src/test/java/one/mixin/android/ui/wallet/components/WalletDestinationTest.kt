package one.mixin.android.ui.wallet.components

import one.mixin.android.vo.WalletCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletDestinationTest {
    @Test
    fun classicWalletCategoryCreatesClassicDestination() {
        assertEquals(
            WalletDestination.Classic("wallet-id"),
            walletDestinationForWallet("wallet-id", WalletCategory.CLASSIC.value),
        )
    }

    @Test
    fun importedWalletCategoryCreatesImportDestination() {
        assertEquals(
            WalletDestination.Import("wallet-id", WalletCategory.IMPORTED_MNEMONIC.value),
            walletDestinationForWallet("wallet-id", WalletCategory.IMPORTED_MNEMONIC.value),
        )
    }

    @Test
    fun watchWalletCategoryCreatesWatchDestination() {
        assertEquals(
            WalletDestination.Watch("wallet-id", WalletCategory.WATCH_ADDRESS.value),
            walletDestinationForWallet("wallet-id", WalletCategory.WATCH_ADDRESS.value),
        )
    }

    @Test
    fun walletDestinationJsonRoundTripsImportDestination() {
        val destination = WalletDestination.Import("wallet-id", WalletCategory.IMPORTED_MNEMONIC.value)

        assertEquals(
            destination,
            walletDestinationFromJson(walletDestinationToJson(destination)),
        )
    }
}
