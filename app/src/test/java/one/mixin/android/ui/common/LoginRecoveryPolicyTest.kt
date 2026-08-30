package one.mixin.android.ui.common

import one.mixin.android.vo.WalletCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginRecoveryPolicyTest {
    @Test
    fun failedClassicUtxoBackfillKeepsSyncedWalletsForLogin() {
        val wallets = listOf("classic")

        assertEquals(wallets, classicWalletAfterUtxoBackfill(wallets, backfillSucceeded = false))
    }

    @Test
    fun failedImportedMnemonicDecryptSkipsOnlyThatWallet() {
        assertEquals(
            ImportedMnemonicBackfillAction.SKIP,
            importedMnemonicBackfillAction(
                walletCategory = WalletCategory.IMPORTED_MNEMONIC.value,
                mnemonic = null,
            ),
        )
    }

    @Test
    fun decryptableImportedMnemonicWalletIsProcessed() {
        assertEquals(
            ImportedMnemonicBackfillAction.PROCESS,
            importedMnemonicBackfillAction(
                walletCategory = WalletCategory.IMPORTED_MNEMONIC.value,
                mnemonic = "mnemonic",
            ),
        )
    }
}
