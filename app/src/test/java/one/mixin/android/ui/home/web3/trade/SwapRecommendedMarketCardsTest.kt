package one.mixin.android.ui.home.web3.trade

import one.mixin.android.vo.market.MarketItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SwapRecommendedMarketCardsTest {
    @Test
    fun recommendedCardsShowOnlyWhenSendInputIsEmptyAndNotFocused() {
        assertTrue(
            shouldShowSwapRecommendedMarketCards(
                hasRecommendedCards = true,
                inMixin = true,
                inputText = "",
                isSendFocused = false,
                isKeyboardVisible = false,
            )
        )

        assertFalse(
            shouldShowSwapRecommendedMarketCards(
                hasRecommendedCards = true,
                inMixin = false,
                inputText = "",
                isSendFocused = false,
                isKeyboardVisible = false,
            )
        )

        assertFalse(
            shouldShowSwapRecommendedMarketCards(
                hasRecommendedCards = true,
                inMixin = true,
                inputText = "",
                isSendFocused = true,
                isKeyboardVisible = false,
            )
        )
        assertFalse(
            shouldShowSwapRecommendedMarketCards(
                hasRecommendedCards = true,
                inMixin = true,
                inputText = "1",
                isSendFocused = false,
                isKeyboardVisible = false,
            )
        )
        assertFalse(
            shouldShowSwapRecommendedMarketCards(
                hasRecommendedCards = true,
                inMixin = true,
                inputText = "",
                isSendFocused = false,
                isKeyboardVisible = true,
            )
        )
    }

    @Test
    fun systemBackKeyboardDismissShowsRecommendedCardsEvenIfFocusRemains() {
        var isSendFocused = true
        if (shouldResetSwapSendFocusState(inputText = "", isKeyboardVisible = false)) {
            isSendFocused = false
        }

        assertTrue(
            shouldShowSwapRecommendedMarketCards(
                hasRecommendedCards = true,
                inMixin = true,
                inputText = "",
                isSendFocused = isSendFocused,
                isKeyboardVisible = false,
            )
        )
        assertFalse(shouldResetSwapSendFocusState(inputText = "", isKeyboardVisible = true))
        assertFalse(shouldResetSwapSendFocusState(inputText = "1", isKeyboardVisible = false))
    }

    @Test
    fun recommendedMarketPriceUsesCompactFixedDecimals() {
        assertEquals("$1.23", formatRecommendedMarketFiatPrice(BigDecimal("1.234"), "$"))
        assertEquals("$0.1234", formatRecommendedMarketFiatPrice(BigDecimal("0.12345"), "$"))
        assertEquals("$1.23K", formatRecommendedMarketFiatPrice(BigDecimal("1234.56"), "$"))
        assertEquals("<$0.0001", formatRecommendedMarketFiatPrice(BigDecimal("0.00009"), "$"))
        assertEquals(null, formatRecommendedMarketFiatPrice(BigDecimal.ZERO, "$"))
    }

    @Test
    fun recommendedMarketPercentUsesCompactKFormatWhenLarge() {
        assertEquals("+999.99%", formatRecommendedMarketSignedPercent(BigDecimal("999.99")))
        assertEquals("+1K%", formatRecommendedMarketSignedPercent(BigDecimal("1000")))
        assertEquals("+1.2K%", formatRecommendedMarketSignedPercent(BigDecimal("1299.99")))
        assertEquals("-1.5K%", formatRecommendedMarketSignedPercent(BigDecimal("-1500.12")))
    }

    @Test
    fun recommendedTopMoversAreSortedBy24HourChange() {
        val markets =
            listOf(
                market("middle", "5"),
                market("lowest", "-10"),
                market("highest", "20"),
            )

        assertEquals(
            listOf("highest", "middle", "lowest"),
            sortSwapRecommendedMarkets(markets, SwapRecommendedMarketType.TopGainers).map { it.coinId },
        )
        assertEquals(
            listOf("lowest", "middle", "highest"),
            sortSwapRecommendedMarkets(markets, SwapRecommendedMarketType.TopLosers).map { it.coinId },
        )
    }

    private fun market(
        coinId: String,
        change24h: String,
    ) = MarketItem(
        coinId = coinId,
        name = coinId,
        symbol = coinId.uppercase(),
        iconUrl = "",
        currentPrice = "1",
        marketCap = "100",
        marketCapRank = "1",
        totalVolume = "10",
        high24h = "1",
        low24h = "1",
        priceChange24h = "0",
        priceChangePercentage1H = "0",
        priceChangePercentage24H = change24h,
        priceChangePercentage7D = "0",
        priceChangePercentage30D = "0",
        marketCapChange24h = "0",
        marketCapChangePercentage24h = "0",
        circulatingSupply = "0",
        totalSupply = "0",
        maxSupply = "0",
        ath = "0",
        athChangePercentage = "0",
        athDate = "",
        atl = "0",
        atlChangePercentage = "0",
        atlDate = "",
        assetIds = emptyList(),
        sparklineIn7d = "",
        sparklineIn24 = "",
        isFavored = false,
    )
}
