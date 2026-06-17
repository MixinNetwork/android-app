package one.mixin.android.ui.home.web3.trade

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
                inputText = "",
                isSendFocused = false,
                isKeyboardVisible = false,
            )
        )

        assertFalse(
            shouldShowSwapRecommendedMarketCards(
                hasRecommendedCards = true,
                inputText = "",
                isSendFocused = true,
                isKeyboardVisible = false,
            )
        )
        assertFalse(
            shouldShowSwapRecommendedMarketCards(
                hasRecommendedCards = true,
                inputText = "1",
                isSendFocused = false,
                isKeyboardVisible = false,
            )
        )
        assertFalse(
            shouldShowSwapRecommendedMarketCards(
                hasRecommendedCards = true,
                inputText = "",
                isSendFocused = false,
                isKeyboardVisible = true,
            )
        )
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
