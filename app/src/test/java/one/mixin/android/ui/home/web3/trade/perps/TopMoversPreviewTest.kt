package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.api.response.perps.PerpsMarket
import java.math.BigDecimal
import kotlin.test.assertEquals
import org.junit.Test

class TopMoversPreviewTest {
    @Test
    fun topMoversPreviewUsesTopFourAndBottomFourMarkets() {
        val markets = listOf(
            market("a", "0.01"),
            market("b", "0.08"),
            market("c", "-0.04"),
            market("d", "0.03"),
            market("e", "-0.09"),
            market("f", "0.05"),
            market("g", "-0.02"),
            market("h", "0.13"),
            market("i", "-0.12"),
            market("j", "0.07"),
        )

        val result = markets.topMoversPreview().map { it.marketId }

        assertEquals(
            listOf("h", "b", "j", "f", "i", "e", "c", "g"),
            result,
        )
    }

    @Test
    fun formatPerpsSignedPercent_formatsLargeValuesWithK() {
        assertEquals("+1.5K%", formatPerpsSignedPercent(BigDecimal(1500)))
        assertEquals("+1K%", formatPerpsSignedPercent(BigDecimal(1000)))
        assertEquals("-2.3K%", formatPerpsSignedPercent(BigDecimal(-2300)))
        assertEquals("+500%", formatPerpsSignedPercent(BigDecimal(500)))
        assertEquals("+999.99%", formatPerpsSignedPercent(BigDecimal(999.99)))
        assertEquals("+10.5K%", formatPerpsSignedPercent(BigDecimal(10500)))
    }

    private fun market(
        marketId: String,
        change: String,
    ) = PerpsMarket(
        marketId = marketId,
        displaySymbol = marketId,
        tokenSymbol = marketId,
        quoteSymbol = "USDT",
        markPrice = "1",
        priceScale = 2,
        leverage = 10,
        iconUrl = "",
        category = "",
        tags = emptyList(),
        fundingRate = "0",
        minAmount = "0",
        maxAmount = "0",
        last = "1",
        volume = "0",
        high = "1",
        low = "1",
        open = "1",
        change = change,
        bidPrice = "1",
        askPrice = "1",
        createdAt = "",
        updatedAt = "",
    )
}
