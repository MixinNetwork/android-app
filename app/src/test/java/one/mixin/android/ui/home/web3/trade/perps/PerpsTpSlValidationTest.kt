package one.mixin.android.ui.home.web3.trade.perps

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.MixinApplication
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
class PerpsTpSlValidationTest {
    @Before
    fun setUp() {
        MixinApplication.appContext = ApplicationProvider.getApplicationContext<Context>()
    }

    @Test
    fun takeProfitValidatesCurrentPrice() {
        val basis = resolveTpSlCalculationBasis("100", "100", "100", "10", 10, "0.001")!!
        for ((isLong, currentPrice, invalidPrice) in listOf(
            Triple(true, "90", "89"),
            Triple(true, "90", "90"),
            Triple(true, "110", "105"),
            Triple(true, "110", "110"),
            Triple(false, "110", "111"),
            Triple(false, "110", "110"),
            Triple(false, "90", "95"),
            Triple(false, "90", "90"),
        )) {
            assertNotNull(validateTpSlPrice(invalidPrice, BigDecimal(currentPrice), null, isLong, true, basis))
        }
        assertNull(validateTpSlPrice("95", BigDecimal("90"), null, true, true, basis))
        assertNull(validateTpSlPrice("100", BigDecimal("90"), null, true, true, basis))
        assertNull(validateTpSlPrice("105", BigDecimal("110"), null, false, true, basis))
        assertNull(validateTpSlPrice("100", BigDecimal("110"), null, false, true, basis))
        assertNotNull(validateTpSlPrice("0", BigDecimal("110"), null, false, true, basis))
        assertNull(validateTpSlPrice("100.1", BigDecimal("90"), null, true, true, basis))
        assertNull(validateTpSlPrice("99.9", BigDecimal("110"), null, false, true, basis))
    }

    @Test
    fun openingStopLossUsesServerBoundaryInsteadOfLeverageEstimate() {
        val basis = resolveTpSlCalculationBasis("", "100", "100", null, 10, "0.001")!!
        val longBound = resolveTpSlLiquidationBound("95", "", "100", 10, true)
        val shortBound = resolveTpSlLiquidationBound("105", "", "100", 10, false)
        for (price in listOf("92", "95")) {
            assertNotNull(validateTpSlPrice(price, BigDecimal("100"), longBound, true, false, basis))
        }
        for (price in listOf("108", "105")) {
            assertNotNull(validateTpSlPrice(price, BigDecimal("100"), shortBound, false, false, basis))
        }
        assertNull(validateTpSlPrice("95.01", BigDecimal("100"), longBound, true, false, basis))
        assertNull(validateTpSlPrice("104.99", BigDecimal("100"), shortBound, false, false, basis))
    }
}
