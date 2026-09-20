package one.mixin.android.ui.common

import kotlinx.coroutines.runBlocking
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.ResponseError
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.WalletCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginRecoveryPolicyTest {
    @Test
    fun missingWalletIsDeletedAndItsErrorIsHandledDuringLogin() = runBlocking {
        val wallets = mutableListOf("missing", "active")
        var defaultErrorHandled = false
        val result = requestRouteAPI<Unit, Boolean>(
            invokeNetwork = { MixinResponse(ResponseError(404, ErrorHandler.NOT_FOUND, "Not found")) },
            failureBlock = { response ->
                removeMissingWalletForLogin(response.errorCode) { wallets.remove("missing") }
            },
            defaultErrorHandle = { defaultErrorHandled = true },
            requestSession = { error("No session refresh expected") },
        )

        assertEquals(listOf("active"), wallets)
        assertFalse(defaultErrorHandled)
        assertEquals(listOf("active"), classicWalletAfterUtxoBackfill(wallets, result == true))
        assertTrue(removeMissingWalletForLogin(ErrorHandler.NOT_FOUND) {})
        for (code in listOf(0, ErrorHandler.AUTHENTICATION, ErrorHandler.PIN_INCORRECT, 500)) {
            assertFalse(removeMissingWalletForLogin(code) { error("Must not delete a wallet for error $code") })
        }
    }

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
