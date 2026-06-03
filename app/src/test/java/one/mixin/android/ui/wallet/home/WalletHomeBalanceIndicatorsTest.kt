package one.mixin.android.ui.wallet.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import one.mixin.android.vo.PendingDisplay

class WalletHomeBalanceIndicatorsTest {
    @Test
    fun `pending deposit indicator uses amount and symbol for single pending deposit`() {
        val indicator = listOf(
            PendingDisplay(
                symbol = "BTC",
                iconUrl = "https://example.com/btc.png",
                amount = "0.01",
                assetId = "btc",
            ),
        ).toWalletHomePendingIndicator()

        assertEquals(WalletHomePendingKind.SINGLE_DEPOSIT, indicator?.kind)
        assertEquals("0.01 BTC", indicator?.value)
        assertEquals(listOf("https://example.com/btc.png"), indicator?.iconUrls)
        assertEquals("btc", indicator?.singleAssetId)
    }

    @Test
    fun `pending deposit indicator uses count for multiple pending deposits`() {
        val indicator = listOf(
            PendingDisplay(symbol = "BTC", iconUrl = "btc.png", amount = "0.01", assetId = "btc"),
            PendingDisplay(symbol = "ETH", iconUrl = "eth.png", amount = "1", assetId = "eth"),
            PendingDisplay(symbol = "SOL", iconUrl = "sol.png", amount = "2", assetId = "sol"),
        ).toWalletHomePendingIndicator()

        assertEquals(WalletHomePendingKind.MULTIPLE_DEPOSITS, indicator?.kind)
        assertEquals("3", indicator?.value)
        assertEquals(listOf("btc.png", "eth.png"), indicator?.iconUrls)
        assertNull(indicator?.singleAssetId)
    }

    @Test
    fun `pending deposit indicator is absent when empty`() {
        assertNull(emptyList<PendingDisplay>().toWalletHomePendingIndicator())
    }

    @Test
    fun `pending transaction indicator formats single and multiple counts`() {
        val single = walletHomePendingTransactionIndicator(1)
        val multiple = walletHomePendingTransactionIndicator(3)

        assertEquals(WalletHomePendingKind.SINGLE_TRANSACTION, single?.kind)
        assertEquals("1", single?.value)
        assertEquals(WalletHomePendingKind.MULTIPLE_TRANSACTIONS, multiple?.kind)
        assertEquals("3", multiple?.value)
    }

    @Test
    fun `pending transaction indicator is absent without pending transactions`() {
        assertNull(walletHomePendingTransactionIndicator(0))
    }

    @Test
    fun `watch address indicator formats one address`() {
        val indicator = walletHomeWatchIndicator(listOf("0x1234567890abcdef"))

        assertEquals(WalletHomeWatchKind.SINGLE_ADDRESS, indicator?.kind)
        assertEquals("0x1234..cdef", indicator?.value)
    }

    @Test
    fun `watch address indicator formats multiple addresses`() {
        val indicator = walletHomeWatchIndicator(listOf("a", "b"))

        assertEquals(WalletHomeWatchKind.MULTIPLE_ADDRESSES, indicator?.kind)
        assertEquals("2", indicator?.value)
    }

    @Test
    fun `watch address indicator is absent without addresses`() {
        assertNull(walletHomeWatchIndicator(emptyList()))
    }
}
