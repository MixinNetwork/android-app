package one.mixin.android.ui.home.web3.trade.perps

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class PerpsFormatTest {
    private lateinit var previousDefaultLocale: Locale

    @Before
    fun setUp() {
        previousDefaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousDefaultLocale)
    }

    @Test
    fun exactUsdUsesGroupingAndAtLeastTwoFractionDigits() {
        assertEquals(
            "$1,234.50",
            formatPerpsExactUsdDecimal(BigDecimal("1234.5")),
        )
        assertEquals(
            "$0.80",
            formatPerpsExactUsdDecimal(BigDecimal("0.8")),
        )
    }

    @Test
    fun exactUsdCapsServerPrecision() {
        assertEquals(
            "$1.12345679",
            formatPerpsExactUsdDecimal(BigDecimal("1.123456789")),
        )
    }

    @Test
    fun feeShowsFullAmountInsteadOfLessThanOneCent() {
        assertEquals(
            "-$0.003421",
            formatPerpsSignedExactUsdDecimal(BigDecimal("-0.003421")),
        )
    }

    @Test
    fun tinyPositiveFeeKeepsFullDigits() {
        assertEquals(
            "+$0.0008",
            formatPerpsSignedExactUsdDecimal(BigDecimal("0.000800")),
        )
    }

    @Test
    fun rawUsdStillAbbreviatesTinyPnl() {
        assertEquals(
            "<$0.01",
            formatPerpsRawUsdDecimal(BigDecimal("0.003421")),
        )
    }

    @Test
    fun perpsPriceFormatsAtMarketScale() {
        assertEquals(
            "$50,000.13",
            formatPerpsPrice(BigDecimal("50000.126"), 2),
        )
        assertEquals(
            "$1,234",
            formatPerpsPrice(BigDecimal("1234.4"), 0),
        )
    }

    @Test
    fun perpsPriceKeepsTinyValuesAtHighScale() {
        assertEquals(
            "$0.000123",
            formatPerpsPrice(BigDecimal("0.0001234"), 6),
        )
    }

    @Test
    fun perpsPriceRoundsHalfUp() {
        assertEquals(
            "$1.01",
            formatPerpsPrice(BigDecimal("1.005"), 2),
        )
    }

    @Test
    fun perpsPriceTreatsInvalidBlankAndNullRawInputAsZero() {
        assertEquals(
            "$0.00",
            formatPerpsPrice("not-a-number", 2),
        )
        assertEquals(
            "$0.00",
            formatPerpsPrice("", 2),
        )
        assertEquals(
            "$0.00",
            formatPerpsPrice("   ", 2),
        )
        assertEquals(
            "$0.00",
            formatPerpsPrice(null as String?, 2),
        )
        assertEquals(
            "$0",
            formatPerpsPrice(null as BigDecimal?, 0),
        )
    }

    @Test
    fun perpsPriceCoercesNegativeScaleToZero() {
        assertEquals(
            "$1,235",
            formatPerpsPrice(BigDecimal("1234.5"), -2),
        )
    }
}
