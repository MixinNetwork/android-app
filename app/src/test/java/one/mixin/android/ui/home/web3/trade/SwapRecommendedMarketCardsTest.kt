package one.mixin.android.ui.home.web3.trade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import one.mixin.android.ui.home.web3.market.MarketPriceChangePeriod
import one.mixin.android.ui.home.web3.market.formatMarketPercent
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketItem
import java.math.BigDecimal

class SwapRecommendedMarketCardsTest {
    @Test
    fun trendingAndStockPreserveApiOrderWithoutEightItemCap() {
        val trending = (1..12).map { market("coin-$it", it.toString()) }
        val stocks = (1..10).map { market("stock-$it", it.toString()) }

        val sections =
            buildSwapRecommendedMarketSections(
                trendingMarkets = trending,
                stockMarkets = stocks,
                topGainerMarkets = emptyList(),
                topLoserMarkets = emptyList(),
            )

        assertEquals(trending.map(MarketItem::coinId), sections[0].markets.map(MarketItem::coinId))
        assertEquals(stocks.map(MarketItem::coinId), sections[1].markets.map(MarketItem::coinId))
    }

    @Test
    fun gainersAndLosersKeepAllItemsSortedBySelectedPeriod() {
        val gainers = (1..12).map { market("gainer-$it", change24H = it.toString(), change7D = (20 - it).toString()) }
        val losers = (1..12).map { market("loser-$it", change24H = (-it).toString(), change7D = (-20 + it).toString()) }

        val sections7d =
            buildSwapRecommendedMarketSections(
                trendingMarkets = emptyList(),
                stockMarkets = emptyList(),
                topGainerMarkets = gainers,
                topLoserMarkets = losers,
                priceChangePeriod = MarketPriceChangePeriod.SEVEN_DAYS,
            )

        assertEquals(12, sections7d[0].markets.size)
        assertEquals(12, sections7d[1].markets.size)
        assertEquals("gainer-1", sections7d[0].markets.first().coinId)
        assertEquals("gainer-12", sections7d[0].markets.last().coinId)
        assertEquals("loser-1", sections7d[1].markets.first().coinId)
        assertEquals("loser-12", sections7d[1].markets.last().coinId)
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

    @Test
    fun buildSectionsOrdersCategoriesFiltersStocksAndUsesFallbackAndSortsMovers() {
        val stockBtc = market("stock-nvda", change24H = "10", change7D = "25")
        val btc = market("btc", change24H = "2", change7D = "8")
        val eth = market("eth", change24H = "5", change7D = "3")
        val sol = market("sol", change24H = "-4", change7D = "-12")
        val xrp = market("xrp", change24H = "-1", change7D = "-20")

        val sections7d =
            buildSwapRecommendedMarketSections(
                trendingMarkets = listOf(stockBtc, btc, eth),
                stockMarkets = listOf(stockBtc),
                topGainerMarkets = emptyList(),
                topLoserMarkets = listOf(sol, stockBtc, xrp),
                allMarkets = listOf(btc, eth, stockBtc),
                priceChangePeriod = MarketPriceChangePeriod.SEVEN_DAYS,
            )

        assertEquals(
            listOf(
                SwapRecommendedCategory.TRENDING,
                SwapRecommendedCategory.STOCKS,
                SwapRecommendedCategory.TOP_GAINERS,
                SwapRecommendedCategory.TOP_LOSERS,
            ),
            sections7d.map { it.category },
        )
        assertEquals(listOf("btc", "eth"), sections7d[0].markets.map(MarketItem::coinId))
        assertEquals(listOf("stock-nvda"), sections7d[1].markets.map(MarketItem::coinId))
        assertEquals(listOf("btc", "eth"), sections7d[2].markets.map(MarketItem::coinId))
        assertEquals(listOf("xrp", "sol"), sections7d[3].markets.map(MarketItem::coinId))

        val sections24h =
            buildSwapRecommendedMarketSections(
                trendingMarkets = emptyList(),
                stockMarkets = emptyList(),
                topGainerMarkets = listOf(btc, eth),
                topLoserMarkets = listOf(xrp, sol),
                allMarkets = emptyList(),
                priceChangePeriod = MarketPriceChangePeriod.TWENTY_FOUR_HOURS,
            )

        assertEquals(
            listOf(
                SwapRecommendedCategory.TOP_GAINERS,
                SwapRecommendedCategory.TOP_LOSERS,
            ),
            sections24h.map { it.category },
        )
        assertEquals(listOf("eth", "btc"), sections24h[0].markets.map(MarketItem::coinId))
        assertEquals(listOf("sol", "xrp"), sections24h[1].markets.map(MarketItem::coinId))
    }

    @Test
    fun marketPercentMatchesMarketPageFormat() {
        assertEquals("+2.35%", formatMarketPercent(BigDecimal("2.35")))
        assertEquals("-0.04%", formatMarketPercent(BigDecimal("-0.04")))
        assertEquals("0%", formatMarketPercent(BigDecimal.ZERO))
    }

    private fun market(
        coinId: String,
        change24H: String,
        change7D: String = "0",
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
                priceChangePercentage7D = change7D,
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
