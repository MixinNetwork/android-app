package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.ui.home.web3.trade.limitTradeInputDecimalPlaces
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PerpsMarginTokenTest {
    @Test
    fun marginBalanceUsesTokenUnitsRegardlessOfUsdPrice() {
        val token = token("margin", "10.12345678")
        for (price in listOf("0.99", "1", "1.01", "0", "")) {
            val balance = perpsMarginTokenBalance(token.copy(priceUsd = price))!!
            assertEquals(BigDecimal("10.12345678"), balance)
            assertEquals("10.12345678", limitTradeInputDecimalPlaces(balance.stripTrailingZeros().toPlainString()))
        }
        assertEquals(BigDecimal.ZERO, perpsMarginTokenBalance(token.copy(balance = "0")))
        assertNull(perpsMarginTokenBalance(token.copy(balance = "invalid")))
        assertNull(perpsMarginTokenBalance(null))
    }

    @Test
    fun usesAcceptedAssetIdsAndBreaksBalanceTiesInApiOrder() {
        val tokens = listOf(
            token("unsupported", "1000"),
            token("api-second", "10.00000001"),
            token("api-first", "10.00000001"),
            token("funded", "10.00000002"),
            token("invalid", "invalid"),
            token("zero", "0"),
        )
        val assetIds = listOf("api-first", "api-second", "missing", "zero", "invalid", "funded")

        assertEquals(
            listOf("funded", "api-first", "api-second", "zero", "invalid"),
            sortPerpsMarginTokens(tokens, assetIds).map { it.assetId },
        )
        assertEquals(emptyList(), sortPerpsMarginTokens(tokens, emptyList()))
    }

    @Test
    fun selectsGreatestNumericBalanceInsteadOfLexicographicOrder() {
        val tokens = listOf(token("first", "9"), token("second", "100"), token("third", "20"))
        assertEquals("second", selectPerpsMarginToken(tokens)?.assetId)
    }

    @Test
    fun preservesPrecisionAndAcceptsZeroBalanceToken() {
        assertEquals("second", selectPerpsMarginToken(listOf(token("first", "10.00000001"), token("second", "10.00000002")))?.assetId)
        assertEquals("zero", selectPerpsMarginToken(listOf(token("zero", "0")))?.assetId)
        assertNull(selectPerpsMarginToken(emptyList()))
    }

    @Test
    fun invalidBalanceDoesNotHideAvailableFunds() {
        assertEquals("funded", selectPerpsMarginToken(listOf(token("invalid", "invalid"), token("funded", "1")))?.assetId)
    }

    private fun token(id: String, balance: String) = TokenItem(
        assetId = id,
        symbol = "USD",
        name = "USD",
        iconUrl = "",
        balance = balance,
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
        assetKey = null,
        dust = null,
        withdrawalMemoPossibility = null,
        collectionHash = null,
        level = null,
        precision = 8,
    )
}
