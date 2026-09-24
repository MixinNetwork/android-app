package one.mixin.android.ui.home.web3.trade.perps

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import one.mixin.android.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class LiquidationPriceLimitTest {
    @Test
    fun showsOnlyMaximumAmountAndFallsBackWhenItIsInvalid() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        val resources = object : Resources(application.assets, application.resources.displayMetrics, application.resources.configuration) {
            override fun getString(id: Int): String = when (id) {
                R.string.error_perps_position_size_exceeds_leverage_limit_value -> "错误 10655：开仓金额超出最大限额 %1\$s，请减少金额或降低杠杆。"
                R.string.error_perps_position_size_exceeds_leverage_limit_add -> "错误 10655：开仓金额超出最大限额 %1\$s，请减少金额。"
                R.string.error_perps_position_size_exceeds_leverage_limit_cannot_add -> "错误 10655：无法加仓。"
                R.string.error_perps_position_size_exceeds_leverage_limit -> "ERROR 10655"
                else -> error("Unexpected string resource: $id")
            }

            override fun getString(id: Int, vararg formatArgs: Any?): String = getString(id).format(*formatArgs)
        }
        val context = object : ContextWrapper(application) {
            override fun getResources(): Resources = resources
        }
        for ((extra, expected) in listOf(
            """{"max_amount":"24.7500","max_leverage":5}""" to "错误 10655：开仓金额超出最大限额 24.75 USDT，请减少金额或降低杠杆。",
            """{"max_amount":0.25}""" to "错误 10655：开仓金额超出最大限额 0.25 USDT，请减少金额或降低杠杆。",
            """{"max_leverage":"3"}""" to "ERROR 10655",
            """{"max_amount":"invalid","max_leverage":2}""" to "ERROR 10655",
            """{"max_amount":"0","max_leverage":0}""" to "ERROR 10655",
            """{"max_amount":"-1","max_leverage":-1}""" to "ERROR 10655",
            """{"max_amount":{},"max_leverage":null}""" to "ERROR 10655",
            "null" to "ERROR 10655",
        )) {
            assertEquals(expected, parseLiquidationPriceLimit(JsonParser.parseString(extra)).errorMessage(context, "USDT"))
        }
        assertEquals("错误 10655：开仓金额超出最大限额 100，请减少金额或降低杠杆。", LiquidationPriceLimit("100", null).errorMessage(context, ""))
        assertEquals(
            "错误 10655：开仓金额超出最大限额 24.75 USDT，请减少金额。",
            LiquidationPriceLimit("24.7500", 5).errorMessage(context, "USDT", isAddingPosition = true),
        )
        for (extra in listOf("""{"max_amount":0}""", """{"max_amount":"0.0000"}""")) {
            assertEquals(
                "错误 10655：无法加仓。",
                parseLiquidationPriceLimit(JsonParser.parseString(extra)).errorMessage(context, "USDT", isAddingPosition = true),
            )
        }
        for (amount in listOf(null, "invalid", "-1")) {
            assertEquals("错误 10655：无法加仓。", LiquidationPriceLimit(amount, null).errorMessage(context, "USDT", isAddingPosition = true))
        }
    }
}
