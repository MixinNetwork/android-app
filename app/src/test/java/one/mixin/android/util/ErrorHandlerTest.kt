package one.mixin.android.util

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.ResponseError
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class ErrorHandlerTest {
    @Test
    fun distinguishesPerpsMarginAndPositionMinimumErrors() {
        assertEquals(R.string.error_perps_margin_too_small, perpsMinimumValueErrorResource(10650))
        assertEquals(R.string.error_perps_margin_too_small_value, perpsMinimumValueErrorResource(10650, hasMinimum = true))
        assertEquals(R.string.error_perps_position_size_too_small, perpsMinimumValueErrorResource(10654))
        assertEquals(R.string.error_perps_position_size_too_small_value, perpsMinimumValueErrorResource(10654, hasMinimum = true))
        assertNull(perpsMinimumValueErrorResource(10653))
    }

    @Test
    fun showsMinimumValueWithCurrencyOrFallsBackToTheMatchingError() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        val resources =
            object : Resources(application.assets, application.resources.displayMetrics, application.resources.configuration) {
                override fun getString(id: Int): String =
                    when (id) {
                        R.string.error_perps_margin_too_small -> "ERROR 10650: Margin is too small"
                        R.string.error_perps_margin_too_small_value -> "ERROR 10650: Margin must have minimum value of %1\$s"
                        R.string.error_perps_position_size_too_small -> "ERROR 10654: Position size is too small"
                        R.string.error_perps_position_size_too_small_value -> "ERROR 10654: Position must have minimum size of %1\$s"
                        else -> error("Unexpected string resource: $id")
                    }

                override fun getString(
                    id: Int,
                    vararg formatArgs: Any?,
                ): String = getString(id).format(*formatArgs)
            }
        val context =
            object : ContextWrapper(application) {
                override fun getResources(): Resources = resources
            }
        val gson = Gson()
        for (code in listOf(10650, 10654)) {
            for ((extra, minimum) in listOf(
                """{"field":"amount","min_order_value":"10","reason":"order value must be at least 10"}""" to "10",
                """{"min_order_value":"0.20"}""" to "0.2",
                """{"min_order_value":"0.00000001"}""" to "0.00000001",
                """{"min_order_value":10}""" to "10",
                """{"min_order_value":"0"}""" to null,
                """{"min_order_value":"-10"}""" to null,
                """{"min_order_value":"invalid"}""" to null,
                """{"min_order_value":{}}""" to null,
                """{"min_order_value":null}""" to null,
                """{"min_order_value":true}""" to null,
                """{"min_order_value":"1e10"}""" to null,
                "{}" to null,
                "null" to null,
            )) {
                val error =
                    gson.fromJson(
                        """{"status":202,"code":$code,"description":"Perps order value is below the minimum","extra":$extra}""",
                        ResponseError::class.java,
                    )
                val response = MixinResponse<Any>(error)
                val expected = if (code == 10650) {
                    minimum?.let { "ERROR 10650: Margin must have minimum value of \$$it" }
                        ?: "ERROR 10650: Margin is too small"
                } else {
                    minimum?.let { "ERROR 10654: Position must have minimum size of \$$it" }
                        ?: "ERROR 10654: Position size is too small"
                }
                assertEquals(expected, context.getMixinErrorStringByCode(response.errorCode, response.errorDescription))
            }
        }
        assertEquals("ERROR 10650: Margin is too small", context.getMixinErrorStringByCode(10650, "Invalid extra\n{broken"))
        assertEquals("ERROR 10654: Position size is too small", context.getMixinErrorStringByCode(10654, "Invalid extra\n{broken"))
    }
}
