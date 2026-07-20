package one.mixin.android.ui.tip

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import one.mixin.android.Constants
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.vo.WalletCategory

class PendingMnemonicTipRoutingTest {
    @Test
    fun tipFlowCreatesClassicWalletAfterSafeRegistration() {
        assertTrue(shouldCreateClassicWalletAfterTip(TipType.Create))
    }

    @Test
    fun successfulLocalSaveClearsPendingAndRoutesToWalletHome() = runBlocking {
        var cleared = false
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(matchingImportedWallet("imported-id")),
            pin = "123456",
            pendingWords = pendingMnemonicWords,
            save = { _, _, _ -> true },
            clear = { cleared = true },
        )

        assertEquals(PendingMnemonicResolution.WalletHome("imported-id"), result)
        assertTrue(cleared)
    }

    @Test
    fun matchingPathlessWatchWalletClearsPendingAndRoutesToWatchWallet() = runBlocking {
        var cleared = false
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(matchingWatchWallet("watch-id", index = 7)),
            pin = "123456",
            pendingWords = pendingMnemonicWords,
            save = { _, _, _ -> error("Mnemonic should not be saved for a watch wallet") },
            clear = { cleared = true },
        )

        assertEquals(
            PendingMnemonicResolution.WalletHome(
                "watch-id",
                WalletCategory.WATCH_ADDRESS.value,
            ),
            result,
        )
        assertTrue(cleared)
    }

    @Test
    fun matchingWatchWalletTakesPriorityOverImportedWallet() = runBlocking {
        var cleared = false
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(
                matchingImportedWallet("imported-id"),
                matchingWatchWallet("watch-id"),
            ),
            pin = "123456",
            pendingWords = pendingMnemonicWords,
            save = { _, _, _ -> error("Imported wallet should not be restored when a watch wallet matches") },
            clear = { cleared = true },
        )

        assertEquals(
            PendingMnemonicResolution.WalletHome(
                "watch-id",
                WalletCategory.WATCH_ADDRESS.value,
            ),
            result,
        )
        assertTrue(cleared)
    }

    @Test
    fun failedLocalSaveDoesNotClearPendingOrRouteToWalletHome() = runBlocking {
        var cleared = false
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(matchingImportedWallet("imported-id")),
            pin = "123456",
            pendingWords = pendingMnemonicWords,
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
            wallets = listOf(matchingImportedWallet("imported-id")),
            pin = "123456",
            pendingWords = pendingMnemonicWords,
            save = { _, _, _ -> error("save failed") },
            clear = { cleared = true },
        )

        assertEquals(PendingMnemonicResolution.LocalSaveFailed, result)
        assertFalse(cleared)
    }

    @Test
    fun savesEveryMatchingImportedMnemonicWalletBeforeClearingPending() = runBlocking {
        val savedWalletIds = mutableListOf<String>()
        var cleared = false
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(
                matchingImportedWallet("first-id", index = 0),
                matchingImportedWallet("second-id", index = 1),
            ),
            pin = "123456",
            pendingWords = pendingMnemonicWords,
            save = { walletId, _, _ ->
                savedWalletIds += walletId
                true
            },
            clear = { cleared = true },
        )

        assertEquals(PendingMnemonicResolution.WalletHome("first-id"), result)
        assertEquals(listOf("first-id", "second-id"), savedWalletIds)
        assertTrue(cleared)
    }

    @Test
    fun laterLocalSaveFailureKeepsPendingForEveryMatchingWallet() = runBlocking {
        val savedWalletIds = mutableListOf<String>()
        var cleared = false
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(
                matchingImportedWallet("first-id", index = 0),
                matchingImportedWallet("second-id", index = 1),
            ),
            pin = "123456",
            pendingWords = pendingMnemonicWords,
            save = { walletId, _, _ ->
                savedWalletIds += walletId
                walletId != "second-id"
            },
            clear = { cleared = true },
        )

        assertEquals(PendingMnemonicResolution.LocalSaveFailed, result)
        assertEquals(listOf("first-id", "second-id"), savedWalletIds)
        assertFalse(cleared)
    }

    @Test
    fun missingPinRequestsVerificationWithoutSaving() = runBlocking {
        var saved = false
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(matchingImportedWallet("imported-id")),
            pin = null,
            pendingWords = pendingMnemonicWords,
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

    @Test
    fun unmatchedImportedMnemonicWalletRoutesToImportWithoutSaving() = runBlocking {
        var saved = false
        val wallet = testWallet("imported-id", WalletCategory.IMPORTED_MNEMONIC.value).apply {
            addresses = listOf(
                Web3Address(
                    addressId = "address-id",
                    walletId = id,
                    chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                    destination = "0xnot-the-pending-mnemonic-address",
                    path = "m/44'/60'/0'/0/0",
                    createdAt = "",
                ),
            )
        }
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(wallet),
            pin = "123456",
            pendingWords = pendingMnemonicWords,
            save = { _, _, _ ->
                saved = true
                true
            },
            clear = {},
        )

        assertEquals(PendingMnemonicResolution.ImportMnemonic, result)
        assertFalse(saved)
    }

    @Test
    fun matchingImportedPrivateKeyWalletSavesDerivedPrivateKeyAndRoutesToWalletHome() = runBlocking {
        var savedWalletId: String? = null
        var savedPrivateKey: String? = null
        val derivedPrivateKey = "derived-private-key"
        val result = resolvePendingMnemonicAfterWalletFetch(
            wallets = listOf(matchingImportedPrivateKeyWallet("private-key-id")),
            pin = "123456",
            pendingWords = pendingMnemonicWords,
            privateKeyForAddress = { _, _, _ -> derivedPrivateKey },
            save = { _, _, _ -> error("Mnemonic should not be saved for an imported private key wallet") },
            savePrivateKey = { walletId, _, privateKey ->
                savedWalletId = walletId
                savedPrivateKey = privateKey
                true
            },
            clear = {},
        )

        assertEquals(
            PendingMnemonicResolution.WalletHome(
                "private-key-id",
                WalletCategory.IMPORTED_PRIVATE_KEY.value,
            ),
            result,
        )
        assertEquals("private-key-id", savedWalletId)
        assertEquals(derivedPrivateKey, savedPrivateKey)
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

    private fun matchingImportedWallet(id: String, index: Int = 0): Web3Wallet {
        val wallet = testWallet(id, WalletCategory.IMPORTED_MNEMONIC.value)
        wallet.addresses = listOf(
            Web3Address(
                addressId = "other-address-id",
                walletId = id,
                chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                destination = "0xnot-the-pending-mnemonic-address",
                path = "m/44'/60'/0'/0/1",
                createdAt = "",
            ),
            Web3Address(
                addressId = "address-id",
                walletId = id,
                chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                destination = CryptoWalletHelper.mnemonicToAddress(
                    mnemonic = pendingMnemonicWords.joinToString(" "),
                    chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                    index = index,
                ),
                path = "m/44'/60'/0'/0/$index",
                createdAt = "",
            ),
        )
        return wallet
    }

    private fun matchingImportedPrivateKeyWallet(id: String): Web3Wallet {
        val wallet = testWallet(id, WalletCategory.IMPORTED_PRIVATE_KEY.value)
        wallet.addresses = listOf(
            Web3Address(
                addressId = "address-id",
                walletId = id,
                chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                destination = CryptoWalletHelper.mnemonicToAddress(
                    mnemonic = pendingMnemonicWords.joinToString(" "),
                    chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                ),
                path = "m/44'/60'/0'/0/0",
                createdAt = "",
            ),
        )
        return wallet
    }

    private fun matchingWatchWallet(id: String, index: Int = 0): Web3Wallet {
        val wallet = testWallet(id, WalletCategory.WATCH_ADDRESS.value)
        wallet.addresses = listOf(
            Web3Address(
                addressId = "address-id",
                walletId = id,
                chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                destination = CryptoWalletHelper.mnemonicToAddress(
                    mnemonic = pendingMnemonicWords.joinToString(" "),
                    chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                    index = index,
                ),
                path = null,
                createdAt = "",
            ),
        )
        return wallet
    }

    private companion object {
        val pendingMnemonicWords =
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about".split(" ")
    }
}
