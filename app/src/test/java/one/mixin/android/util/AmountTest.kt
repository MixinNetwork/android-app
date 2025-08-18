package one.mixin.android.util

import one.mixin.android.ui.wallet.fiatmoney.AmountUtil
import org.junit.Test
import kotlin.test.assertEquals

class AmountTest {
    @Test
    fun testAmount() {
        assertEquals(AmountUtil.illegal("1.23", "JPY"), true)
        assertEquals(AmountUtil.illegal("1.23", "KRW"), true)

        assertEquals(AmountUtil.illegal("1.23", "USD"), false)

        assertEquals(AmountUtil.illegal("123456789", "JPY"), false)

        assertEquals(AmountUtil.illegal("1234567890", "JPY"), true)

        assertEquals(AmountUtil.illegal("1234567890", "USD"), true)

        assertEquals(AmountUtil.illegal("1234567", "USD"), false)
        assertEquals(AmountUtil.illegal("1234567.23", "USD"), false)

        assertEquals(AmountUtil.illegal("1234567.234", "USD"), true)
        assertEquals(AmountUtil.illegal("12345678.234", "USD"), true)
        assertEquals(AmountUtil.illegal("123.234", "USD"), true)

        assertEquals(AmountUtil.illegal("123.24", "USD"), false)
        assertEquals(AmountUtil.illegal("123.4", "USD"), false)
        assertEquals(AmountUtil.illegal("123.", "USD"), false)
        assertEquals(AmountUtil.illegal("123", "USD"), false)
    }
}
