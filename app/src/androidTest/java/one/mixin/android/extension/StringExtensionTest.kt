package one.mixin.android.extension

import org.junit.Assert.assertEquals
import org.junit.Test

class StringExtensionTest {

    @Test
    fun getPattern() {
        val s1 = "12345678901"
        val s2 = "123456789.1234567"
        val s3 = "12345.0600"
        val s4 = "0.00011231"
        val s5 = "1234567.00101"
        val s6 = "123.0001014324"

        assertEquals(",###", s1.getPattern())
        assertEquals(",###", s2.getPattern())
        assertEquals(",###.###", s3.getPattern())
        assertEquals(",###.#######", s4.getPattern())
        assertEquals(",###.#", s5.getPattern())
        assertEquals(",###.#####", s6.getPattern())
    }

    @Test
    fun numberFormat2() {
        val s1 = "12345"
        val s2 = "0.123"
        val s3 = "0.103"
        val s4 = "0.108"
        val s5 = "0.0000000023"

        assertEquals("12,345", s1.numberFormat2())
        assertEquals("0.12", s2.numberFormat2())
        assertEquals("0.1", s3.numberFormat2())
        assertEquals("0.11", s4.numberFormat2())
        assertEquals("0.00", s5.numberFormat2())
    }

    @Test
    fun numberFormat8() {
        val s1 = "12345678901"
        val s2 = "123456789.1234567"
        val s3 = "12345.0600"
        val s4 = "0.000112312"
        val s5 = "1234567.00101"
        val s6 = "123.00011014324"
        val s7 = "123.00011142"
        val s8 = "779.99640892283"
        val s9 = "1.000112312"
        val s10 = "129.99641012"
        val s11 = "-0.00000001"
        val s12 = "-0.000000001"
        val s13 = "0.0000000001"

        assertEquals("12,345,678,901", s1.numberFormat8())
        assertEquals("123,456,789", s2.numberFormat8())
        assertEquals("12,345.06", s3.numberFormat8())
        assertEquals("0.00011231", s4.numberFormat8())
        assertEquals("1,234,567", s5.numberFormat8())
        assertEquals("123.00011", s6.numberFormat8())
        assertEquals("123.00011", s7.numberFormat8())
        assertEquals("779.99641", s8.numberFormat8())
        assertEquals("1.0001123", s9.numberFormat8())
        assertEquals("129.99641", s10.numberFormat8())
        assertEquals("-0.00000001", s11.numberFormat8())
        assertEquals("-0", s12.numberFormat8())
        assertEquals("0.00000000", s13.numberFormat8())
    }

    @Test
    fun numberFormat() {
        val s1 = "12345678901"
        val s2 = "123456789.1234567"
        val s3 = "12345.0600"
        val s4 = "0.000112312000827543897052"
        val s5 = "1234567.0010123049580234598807834658927346001234"

        assertEquals("12,345,678,901", s1.numberFormat())
        assertEquals("123,456,789.1234567", s2.numberFormat())
        assertEquals("12,345.06", s3.numberFormat())
        assertEquals("0.000112312000827543897052", s4.numberFormat())
        assertEquals("1,234,567.0010123049580234598807834658927346001234", s5.numberFormat())
    }
}
