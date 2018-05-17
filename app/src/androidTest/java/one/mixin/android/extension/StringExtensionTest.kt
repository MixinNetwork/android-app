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
    fun numberFormat8() {
        val s1 = "12345678901"
        val s2 = "123456789.1234567"
        val s3 = "12345.0600"
        val s4 = "0.00011231"
        val s5 = "1234567.00101"
        val s6 = "123.0001014324"

        assertEquals("12,345,678,901", s1.numberFormat8())
        assertEquals("123,456,789", s2.numberFormat8())
        assertEquals("12,345.06", s3.numberFormat8())
        assertEquals("0.0001123", s4.numberFormat8())
        assertEquals("1,234,567", s5.numberFormat8())
        assertEquals("123.0001", s6.numberFormat8())
    }
}