package one.mixin.android.ui.wallet.home

import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.vo.safe.TokenItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletHomeTokenHandoffTest {
    @After
    fun tearDown() {
        WalletHomeTokenHandoff.clear()
    }

    @Test
    fun consumePrivacyTokensReturnsSnapshotOnce() {
        val tokens = listOf(tokenItem("bitcoin"))

        WalletHomeTokenHandoff.savePrivacyTokens(tokens)

        assertEquals(tokens, WalletHomeTokenHandoff.consumePrivacyTokens())
        assertTrue(WalletHomeTokenHandoff.consumePrivacyTokens().isEmpty())
    }

    @Test
    fun consumeWeb3TokensIsScopedByWalletId() {
        val tokens = listOf(web3TokenItem("wallet-a", "bitcoin"))

        WalletHomeTokenHandoff.saveWeb3Tokens("wallet-a", tokens)

        assertTrue(WalletHomeTokenHandoff.consumeWeb3Tokens("wallet-b").isEmpty())
        assertEquals(tokens, WalletHomeTokenHandoff.consumeWeb3Tokens("wallet-a"))
        assertTrue(WalletHomeTokenHandoff.consumeWeb3Tokens("wallet-a").isEmpty())
    }

    private fun tokenItem(id: String) =
        TokenItem(
            assetId = id,
            symbol = id.uppercase(),
            name = id,
            iconUrl = "",
            balance = "1",
            priceBtc = "0",
            priceUsd = "1",
            chainId = "chain-$id",
            changeUsd = "0",
            changeBtc = "0",
            hidden = false,
            confirmations = 0,
            chainIconUrl = null,
            chainSymbol = null,
            chainName = null,
            assetKey = null,
            dust = null,
            withdrawalMemoPossibility = null,
            collectionHash = null,
            level = null,
            precision = 8,
        )

    private fun web3TokenItem(
        walletId: String,
        id: String,
    ) = Web3TokenItem(
        walletId = walletId,
        assetId = id,
        chainId = "chain-$id",
        name = id,
        assetKey = id,
        symbol = id.uppercase(),
        iconUrl = "",
        precision = 8,
        balance = "1",
        priceUsd = "1",
        changeUsd = "0",
        chainIcon = null,
        chainName = null,
        chainSymbol = null,
        hidden = false,
        level = 0,
    )
}
