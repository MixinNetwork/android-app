package one.mixin.android.web3.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import one.mixin.android.db.web3.vo.Web3TokenItem

class Web3TokenNavigationTest {
    @Test
    fun keepsKnownTokenForTheSameWallet() {
        val token = token(walletId = "wallet-a", balance = "2")

        val resolved = tokenForTransactionWallet("wallet-a", "asset", token, walletToken = null)

        assertEquals(token, resolved)
    }

    @Test
    fun prefersTheTransactionWalletOverAnotherWalletsToken() {
        val otherWallet = token(walletId = "wallet-b", balance = "9")
        val transactionWallet = token(walletId = "wallet-a", balance = "1")

        val resolved = tokenForTransactionWallet("wallet-a", "asset", otherWallet, transactionWallet)

        assertEquals(transactionWallet, resolved)
    }

    @Test
    fun retagsDisplayFallbackOntoTheTransactionWallet() {
        val otherWallet = token(walletId = "wallet-b", balance = "9", hidden = true)

        val resolved = tokenForTransactionWallet("wallet-a", "asset", otherWallet, walletToken = null)

        assertEquals("wallet-a", resolved?.walletId)
        assertEquals("0", resolved?.balance)
        assertNull(resolved?.hidden)
        assertEquals("asset", resolved?.assetId)
        assertEquals("ETH", resolved?.symbol)
    }

    @Test
    fun ignoresKnownTokenForADifferentAsset() {
        val otherAsset = token(walletId = "wallet-b", assetId = "other")

        val resolved = tokenForTransactionWallet("wallet-a", "asset", otherAsset, walletToken = null)

        assertNull(resolved)
    }

    private fun token(
        walletId: String,
        assetId: String = "asset",
        balance: String = "0",
        hidden: Boolean? = false,
    ) = Web3TokenItem(
        walletId = walletId,
        assetId = assetId,
        chainId = "chain",
        name = "Ether",
        assetKey = "0x0",
        symbol = "ETH",
        iconUrl = "",
        precision = 18,
        balance = balance,
        priceUsd = "1",
        changeUsd = "0",
        chainIcon = null,
        chainName = null,
        chainSymbol = null,
        hidden = hidden,
        level = 0,
    )
}
