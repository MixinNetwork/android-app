package one.mixin.android.ui.home.web3.trade

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SwapRecommendedMarketCardsTest {
    @Test
    fun cachedMarketsFillInitialFailedResponse() =
        runBlocking {
            val cached = (1..10).toList()

            val result =
                recommendedMarketsWithCacheFallback(
                    cachedMarkets = flowOf(cached),
                    fetchedMarkets = flowOf(null),
                    limit = SWAP_RECOMMENDED_MARKET_LIMIT,
                ).first()

            assertEquals(cached.take(SWAP_RECOMMENDED_MARKET_LIMIT), result)
        }

    @Test
    fun cacheUpdatesRecoverInitialFailedResponse() =
        runBlocking {
            val result =
                recommendedMarketsWithCacheFallback(
                    cachedMarkets = flowOf(emptyList(), listOf(1, 2, 3)),
                    fetchedMarkets = flowOf(null),
                    limit = SWAP_RECOMMENDED_MARKET_LIMIT,
                ).first { it.isNotEmpty() }

            assertEquals(listOf(1, 2, 3), result)
        }

    @Test
    fun successfulResponsePreservesOrderAndOverridesCache() =
        runBlocking {
            val result =
                recommendedMarketsWithCacheFallback(
                    cachedMarkets = flowOf(listOf(1, 2, 3)),
                    fetchedMarkets = flowOf(listOf(3, 1)),
                    limit = SWAP_RECOMMENDED_MARKET_LIMIT,
                ).first()

            assertEquals(listOf(3, 1), result)
        }

    @Test
    fun successfulResponseDoesNotWaitForCache() =
        runBlocking {
            val result =
                recommendedMarketsWithCacheFallback(
                    cachedMarkets = flow { error("cache should not be collected") },
                    fetchedMarkets = flowOf(listOf(3, 1)),
                    limit = SWAP_RECOMMENDED_MARKET_LIMIT,
                ).first()

            assertEquals(listOf(3, 1), result)
        }

    @Test
    fun successfulEmptyResponseOverridesCache() =
        runBlocking {
            val result =
                recommendedMarketsWithCacheFallback(
                    cachedMarkets = flowOf(listOf(1, 2, 3)),
                    fetchedMarkets = flowOf(emptyList()),
                    limit = SWAP_RECOMMENDED_MARKET_LIMIT,
                ).first()

            assertEquals(emptyList<Int>(), result)
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
}
