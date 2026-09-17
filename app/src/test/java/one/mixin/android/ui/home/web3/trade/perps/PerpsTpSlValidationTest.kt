package one.mixin.android.ui.home.web3.trade.perps

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.MixinApplication
import one.mixin.android.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE)
class PerpsTpSlValidationTest {
    @Before
    @Suppress("DEPRECATION")
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resources = object : Resources(context.assets, context.resources.displayMetrics, context.resources.configuration) {
            override fun getString(id: Int, vararg formatArgs: Any?): String = when (id) {
                R.string.the_price_must_higher_than -> "above ${formatArgs.single()}"
                R.string.the_price_must_lower_than -> "below ${formatArgs.single()}"
                R.string.error_invalid_number -> "invalid number"
                else -> error("Unexpected string resource: $id")
            }
        }
        MixinApplication.appContext = object : ContextWrapper(context) {
            override fun getResources(): Resources = resources
        }
    }

    @Test
    fun higherLeverageInvalidatesPreviouslyValidStopLossForBothSides() {
        for ((isLong, price, boundary) in listOf(Triple(true, "92", "95"), Triple(false, "108", "105"))) {
            assertNull(validate(price, isLong, leverage = 10))
            assertNotNull(validate(price, isLong, leverage = 20))
            assertNull(validate(boundary, isLong, leverage = 20))
            assertNull(validate(price, isLong, leverage = 10))
        }
    }

    @Test
    fun actualLiquidationPriceIsUsedWithoutLeverageScaling() {
        for (leverage in listOf(2, 10, 100)) {
            assertEquals("above $97", validate("96", true, leverage, liquidationPrice = "97"))
            assertEquals("below $103", validate("104", false, leverage, liquidationPrice = "103"))
            assertNull(validate("97", true, leverage, liquidationPrice = "97"))
            assertNull(validate("103", false, leverage, liquidationPrice = "103"))
            assertNull(validate("85", true, leverage, liquidationPrice = "80"))
            assertNull(validate("115", false, leverage, liquidationPrice = "120"))
        }
    }

    @Test
    fun percentInputUsesTheSameActualLiquidationBoundary() {
        for ((isLong, liquidationPrice) in listOf(true to "97", false to "103")) {
            fun validatePercent(percent: String) = validateTpSlPercent(
                rawValue = percent,
                currentPrice = BigDecimal("100"),
                percentBasePrice = BigDecimal("100"),
                liquidationBasePrice = BigDecimal("100"),
                leverage = 10,
                isLong = isLong,
                mode = PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS,
                priceScale = 2,
                liquidationPrice = liquidationPrice.toBigDecimal(),
            )
            assertNotNull(validatePercent("50"))
            assertNull(validatePercent("30"))
            assertNull(validatePercent("20"))
        }
    }

    @Test
    fun currentPriceAndDirectionChangesRevalidateBothPrices() {
        assertNull(validate("110", true, isTakeProfit = true))
        assertNotNull(validate("110", true, isTakeProfit = true, currentPrice = "111"))
        assertNotNull(validate("110", false, isTakeProfit = true))
        assertNull(validate("90", false, isTakeProfit = true))
        assertNotNull(validate("90", false, isTakeProfit = true, currentPrice = "89"))
        assertNotNull(validate("95", false))
        assertNotNull(validate("105", true))
        for (isLong in listOf(true, false)) {
            assertNull(validate("", isLong))
            assertNotNull(validate("invalid", isLong))
            assertNotNull(validate("0", isLong))
        }
    }

    private fun validate(
        price: String,
        isLong: Boolean,
        leverage: Int = 10,
        isTakeProfit: Boolean = false,
        currentPrice: String = "100",
        liquidationPrice: String? = null,
    ) = validateTpSlPrice(
        rawValue = price,
        currentPrice = currentPrice.toBigDecimal(),
        liquidationBasePrice = BigDecimal("100"),
        leverage = leverage,
        isLong = isLong,
        isTakeProfit = isTakeProfit,
        liquidationPrice = liquidationPrice?.toBigDecimal(),
    )
}
