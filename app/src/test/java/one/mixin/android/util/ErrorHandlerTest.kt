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
    fun mapsPerpsOrderValueBelowMinimumErrors() {
        assertEquals(R.string.error_perps_order_value_too_small, perpsOrderValueErrorResource(10650))
        assertEquals(R.string.error_perps_order_value_too_small, perpsOrderValueErrorResource(10654))
        assertNull(perpsOrderValueErrorResource(10653))
    }

    @Test
    fun showMinimumOrderValueWhenProvided() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        val resources =
            object : Resources(application.assets, application.resources.displayMetrics, application.resources.configuration) {
                override fun getString(id: Int): String =
                    when (id) {
                        R.string.error_perps_order_value_too_small -> "ERROR %1\$d: Position size is too small."
                        R.string.error_perps_order_value_minimum -> "ERROR %1\$d: Order value must be at least %2\$s USD."
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
                """{"min_order_value":10}""" to "10",
                """{"min_order_value":"0"}""" to null,
                """{"min_order_value":"-10"}""" to null,
                """{"min_order_value":"invalid"}""" to null,
                """{"min_order_value":{}}""" to null,
                """{"min_order_value":null}""" to null,
                "{}" to null,
                "null" to null,
            )) {
                val error =
                    gson.fromJson(
                        """{"status":202,"code":$code,"description":"Perps order value is below the minimum","extra":$extra}""",
                        ResponseError::class.java,
                    )
                val response = MixinResponse<Any>(error)
                val expected =
                    minimum?.let { "ERROR $code: Order value must be at least $it USD." }
                        ?: "ERROR $code: Position size is too small."
                assertEquals(expected, context.getMixinErrorStringByCode(response.errorCode, response.errorDescription))
            }
        }
        assertEquals("ERROR 10654: Position size is too small.", context.getMixinErrorStringByCode(10654, "Invalid extra\n{broken"))
    }
}
