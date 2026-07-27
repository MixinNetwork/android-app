package one.mixin.android.ui.wallet.components

import org.junit.Assert.assertEquals
import org.junit.Test
import one.mixin.android.crypto.CryptoWallet

class FetchWalletStateTest {
    @Test
    fun initialFetchFailureShowsRetryState() {
        assertEquals(FetchWalletState.FETCH_ERROR, fetchWalletFailureState(hasExistingWallets = false))
    }

    @Test
    fun loadMoreFailureKeepsSelectionState() {
        assertEquals(FetchWalletState.SELECT, fetchWalletFailureState(hasExistingWallets = true))
    }

    @Test
    fun missingMnemonicKeepsFetchingState() {
        assertEquals(FetchWalletState.FETCHING, fetchWalletMissingMnemonicState())
    }

    @Test
    fun blankMnemonicDoesNotStartWalletFetch() {
        assertEquals(false, shouldStartWalletFetch(""))
        assertEquals(false, shouldStartWalletFetch("   "))
        assertEquals(true, shouldStartWalletFetch("blur staff nurse happy palm neutral inflict inform soup almost always canal"))
    }

    @Test
    fun initialFetchSelectsEveryAvailableWallet() {
        val firstAvailable = testWallet("first", exists = false)
        val wallets = listOf(
            testWallet("existing", exists = true),
            firstAvailable,
            testWallet("second", exists = false),
        )

        assertEquals(setOf(firstAvailable, wallets[2]), defaultWalletSelection(wallets))
    }

    @Test
    fun initialFetchDoesNotSelectExistingWallets() {
        val wallets = listOf(testWallet("existing", exists = true))

        assertEquals(emptySet<IndexedWallet>(), defaultWalletSelection(wallets))
    }

    private fun testWallet(name: String, exists: Boolean): IndexedWallet {
        val wallet = CryptoWallet(
            mnemonic = "",
            privateKey = "",
            address = name,
            path = "",
        )
        return IndexedWallet(
            name = name,
            ethereumWallet = wallet,
            solanaWallet = wallet,
            btcWallet = wallet,
            exists = exists,
        )
    }
}
