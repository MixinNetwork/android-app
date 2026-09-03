package one.mixin.android.ui.home.web3.trade

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketCategory
import one.mixin.android.vo.market.MarketItem
import java.math.BigDecimal

class SwapRecommendedMarketCardsTest {
    @Test
    fun trendingAndStockPreserveApiOrderAndApplyDisplayLimit() =
        runBlocking {
            val cached = (1..10).map { market("coin-$it", it.toString()) }

            listOf(MarketCategory.TRENDING, MarketCategory.STOCK).forEach { category ->
                val result =
                    recommendedMarketsFromDatabase(
                        markets = flowOf(cached),
                        limit = SWAP_RECOMMENDED_MARKET_LIMIT,
                    ).first()

                assertEquals(
                    cached.take(SWAP_RECOMMENDED_MARKET_LIMIT).map(MarketItem::coinId),
                    result.map(MarketItem::coinId),
                )
            }
        }

    @Test
    fun databaseRefreshUpdatesRecommendedMarkets() =
        runBlocking {
            val result =
                recommendedMarketsFromDatabase(
                    markets = flowOf(emptyList(), listOf(market("3", "3"), market("1", "1"), market("2", "2"))),
                    limit = SWAP_RECOMMENDED_MARKET_LIMIT,
                ).first { it.isNotEmpty() }

            assertEquals(listOf("3", "1", "2"), result.map(MarketItem::coinId))
        }

    @Test
    fun gainersAndLosersPreserveApiOrder() =
        runBlocking {
            val markets = listOf(market("middle", "2"), market("highest", "5"), market("lowest", "-4"))

            val gainers =
                recommendedMarketsFromDatabase(
                    markets = flowOf(markets),
                    limit = SWAP_RECOMMENDED_MARKET_LIMIT,
                ).first()
            val losers =
                recommendedMarketsFromDatabase(
                    markets = flowOf(markets),
                    limit = SWAP_RECOMMENDED_MARKET_LIMIT,
                ).first()

            assertEquals(listOf("middle", "highest", "lowest"), gainers.map(MarketItem::coinId))
            assertEquals(listOf("middle", "highest", "lowest"), losers.map(MarketItem::coinId))
        }

    @Test
    fun recommendedMarketLimitMatchesIOS() {
        assertEquals(8, SWAP_RECOMMENDED_MARKET_LIMIT)
    }

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

    private fun market(
        coinId: String,
        change24H: String,
    ) =
        MarketItem.fromMarket(
            Market(
                coinId = coinId,
                name = coinId,
                symbol = coinId,
                iconUrl = "",
                currentPrice = "1",
                marketCap = "1",
                marketCapRank = "1",
                totalVolume = "1",
                high24h = "1",
                low24h = "1",
                priceChange24h = "0",
                priceChangePercentage1H = "0",
                priceChangePercentage24H = change24H,
                priceChangePercentage7D = "0",
                priceChangePercentage30D = "0",
                marketCapChange24h = "0",
                marketCapChangePercentage24h = "0",
                circulatingSupply = "1",
                totalSupply = "1",
                maxSupply = "1",
                ath = "1",
                athChangePercentage = "0",
                athDate = "",
                atl = "1",
                atlChangePercentage = "0",
                atlDate = "",
                assetIds = emptyList(),
                sparklineIn7d = "",
                sparklineIn24h = "",
                updatedAt = "",
            ),
        )
}
