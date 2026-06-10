package one.mixin.android.util

import one.mixin.android.ui.wallet.fiatmoney.getCurrencyFromPhoneNumber
import org.junit.Test
import kotlin.test.assertEquals

class CurrencyTest {
    @Test
    fun testCurrencyFromPhone() {
        assertEquals(getCurrencyFromPhoneNumber("+971-4-8005111"), "AED")
        assertEquals(getCurrencyFromPhoneNumber("+610434988123"), "AUD")
        assertEquals(getCurrencyFromPhoneNumber("+33-48518116"), "EUR")
        assertEquals(getCurrencyFromPhoneNumber("+34-303072941"), "EUR")
        assertEquals(getCurrencyFromPhoneNumber("+44-6958189548"), "GBP")
        assertEquals(getCurrencyFromPhoneNumber("+852-45027001"), "HKD")
        assertEquals(getCurrencyFromPhoneNumber("+62-430913315"), "IDR")
        assertEquals(getCurrencyFromPhoneNumber("+81-7303686495"), "JPY")
        assertEquals(getCurrencyFromPhoneNumber("+82-1588-8000"), "KRW")
        assertEquals(getCurrencyFromPhoneNumber("+60 3-8776 2000"), "MYR")
        assertEquals(getCurrencyFromPhoneNumber("+63-8649875404"), "PHP")
        assertEquals(getCurrencyFromPhoneNumber("+65 6595 6868"), "SGD")
        assertEquals(getCurrencyFromPhoneNumber("+886-0970852223"), "TWD")
        assertEquals(getCurrencyFromPhoneNumber("+1 818-840-8840"), "USD")
        assertEquals(getCurrencyFromPhoneNumber("+1 604-207-7077"), "CAD")
        assertEquals(getCurrencyFromPhoneNumber("+84 4441442"), "VND")
        assertEquals(getCurrencyFromPhoneNumber("+90 212 4633000"), "TRY")
        assertEquals(getCurrencyFromPhoneNumber("+91 7428 730894"), "INR")
    }
}
