package one.mixin.android.vo.safe

import one.mixin.android.api.response.web3.groupSwapTokens
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class TokenGroupTest {
    @Test
    fun swapGroupsKeepWalletsAndUnresolvedContractsSeparate() {
        val ethereum = token("ethereum", "usd-coin", "0.1").toSwapToken()
        val base = token("base", "usd-coin", "0.2").toSwapToken()
        val groups = listOf(
            ethereum, base, ethereum,
            base.copy(walletId = "another-wallet"),
            base.copy(assetId = "", address = "contract-1", coinId = null),
            base.copy(assetId = "", address = "contract-2", coinId = null),
        ).groupSwapTokens()
        assertEquals(4, groups.size)
        assertEquals(listOf(ethereum, base), groups.first())
        assertEquals("0.1", groups.first().first().balance)
    }

    @Test
    fun groupByCoinWithoutMergingUnknownAssetsOrInscriptions() {
        val ethereum = token("ethereum", "usd-coin", "0.1")
        val base = token("base", "usd-coin", "0.2")
        val groups = listOf(
            ethereum, base, ethereum,
            token("unknown-1", null, "1"),
            token("unknown-2", "", "2"),
            token("inscription", "usd-coin", "3").copy(collectionHash = "collection"),
        ).groupTokens()

        assertEquals(4, groups.size)
        assertEquals(listOf(ethereum, base), groups.first().tokens)
        assertEquals(BigDecimal("0.3"), groups.first().balance)
        assertEquals("0.1", ethereum.balance)
        assertEquals("0.2", base.balance)
        assertEquals(emptyList<TokenGroup>(), emptyList<TokenItem>().groupTokens())
    }

    private fun token(id: String, coin: String?, amount: String) = TokenItem(
        assetId = id,
        symbol = "USDC",
        name = "USD Coin",
        iconUrl = "",
        balance = amount,
        priceBtc = "0",
        priceUsd = "1",
        chainId = id,
        changeUsd = "0",
        changeBtc = "0",
        hidden = false,
        confirmations = 0,
        chainIconUrl = null,
        chainSymbol = null,
        chainName = null,
        assetKey = id,
        dust = null,
        withdrawalMemoPossibility = null,
        collectionHash = null,
        level = 0,
        precision = 6,
        coinId = coin,
    )
}
