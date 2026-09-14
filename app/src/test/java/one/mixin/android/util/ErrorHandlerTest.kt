package one.mixin.android.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import one.mixin.android.R

class ErrorHandlerTest {
    @Test
    fun mapsPerpsOrderValueBelowMinimumErrors() {
        assertEquals(R.string.error_perps_order_value_too_small, perpsOrderValueErrorResource(10650))
        assertEquals(R.string.error_perps_order_value_too_small, perpsOrderValueErrorResource(10654))
        assertNull(perpsOrderValueErrorResource(10653))
    }
}
