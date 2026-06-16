package one.mixin.android.ui.wallet.home

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletHomeBalanceHandoffTest {
    @After
    fun tearDown() {
        WalletHomeBalanceHandoff.clear()
    }

    @Test
    fun consumePrivacyBalanceReturnsSnapshotOnce() {
        val snapshot = balanceSnapshot("100")

        WalletHomeBalanceHandoff.savePrivacyBalance(snapshot)

        assertEquals(snapshot, WalletHomeBalanceHandoff.consumePrivacyBalance())
        assertNull(WalletHomeBalanceHandoff.consumePrivacyBalance())
    }

    @Test
    fun consumeWeb3BalanceIsScopedByWalletId() {
        val snapshot = balanceSnapshot("100")

        WalletHomeBalanceHandoff.saveWeb3Balance("wallet-a", snapshot)

        assertNull(WalletHomeBalanceHandoff.consumeWeb3Balance("wallet-b"))
        assertEquals(snapshot, WalletHomeBalanceHandoff.consumeWeb3Balance("wallet-a"))
        assertNull(WalletHomeBalanceHandoff.consumeWeb3Balance("wallet-a"))
    }

    private fun balanceSnapshot(fiatTotal: String) =
        WalletHomeBalanceSnapshot(
            fiatTotal = fiatTotal,
            tokenFiatTotal = fiatTotal,
            btcTotal = "0.01",
            fiatSymbol = "$",
            totalTokenCount = 1,
        )
}
