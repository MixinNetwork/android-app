package one.mixin.android.api.response.perps

import com.google.gson.Gson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PerpsPositionFieldsTest {
    @Test
    fun positionResponsePreservesMarginReductionLimitThroughConversion() {
        val json = """{"position_id":"position","market_id":"market","side":"long","quantity":"1","entry_price":"100","leverage":10,"max_decrease_margin":"12.34567891"}"""
        val gson = Gson()
        val response = gson.fromJson(json, PerpsPosition::class.java)
        assertEquals("12.34567891", response.maxDecreaseMargin)
        val item = gson.fromJson(json, PerpsPositionItem::class.java)
        assertEquals(response.maxDecreaseMargin, item.toPosition().maxDecreaseMargin)
    }

    @Test
    fun missingMarginReductionLimitRemainsNull() {
        val gson = Gson()
        val response = gson.fromJson("{}", PerpsPosition::class.java)
        val item = gson.fromJson("{}", PerpsPositionItem::class.java)
        assertNull(response.maxDecreaseMargin)
        assertNull(item.maxDecreaseMargin)
    }
}
