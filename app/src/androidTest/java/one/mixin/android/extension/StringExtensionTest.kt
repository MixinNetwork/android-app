package one.mixin.android.extension

import org.junit.Assert.assertEquals
import org.junit.Test

class StringExtensionTest {

    @Test
    fun formatPrice() {
        val price1 = "12345"
        val price2 = "12345.6789"
        val price3 = "12345.0600"
        val price4 = "0.0001"
        val price5 = "0.00101"
        val price6 = "0.000101"

        assertEquals(price1.formatPrice(), "12345")
        assertEquals(price2.formatPrice(), "12345.67")
        assertEquals(price3.formatPrice(), "12345.06")
        assertEquals(price4.formatPrice(), "0.0001")
        assertEquals(price5.formatPrice(), "0.00101")
        assertEquals(price6.formatPrice(), "0.0001")
    }

    @Test
    fun numberFormat() {
        val num1 = "123"
        val num2 = "1234567"
        val num3 = "1234.5678"
        val num4 = "12345678901234567890123456789.1234567890123456789"
        val num5 = "123456789012345"
        val num6 = "2158185835409464150507677.01642"

        assertEquals("123", num1.numberFormat())
        assertEquals("1,234,567", num2.numberFormat())
        assertEquals("1,234.5678", num3.numberFormat())
        assertEquals("12,345,678,901,234,567,890,123,456,789.1234567890123456789", num4.numberFormat())
        assertEquals("123,456,789,012,345", num5.numberFormat())
        assertEquals("2,158,185,835,409,464,150,507,677.01642", num6.numberFormat())
    }
}