package one.mixin.android.util

import one.mixin.android.ui.wallet.fiatmoney.getCurrencyFromPhone
import org.junit.Test
import kotlin.test.assertEquals

class CurrencyTest {

    @Test
    fun testCurrencyFromPhone() {
        assertEquals(getCurrencyFromPhone("+971-4-8005111"), "AED")
        assertEquals(getCurrencyFromPhone("+610434988123"), "AUD")
        assertEquals(getCurrencyFromPhone("+33-48518116"), "EUR")
        assertEquals(getCurrencyFromPhone("+34-303072941"), "EUR")
        assertEquals(getCurrencyFromPhone("+44-6958189548"), "GBP")
        assertEquals(getCurrencyFromPhone("+852-45027001"), "HKD")
        assertEquals(getCurrencyFromPhone("+62-430913315"), "IDR")
        assertEquals(getCurrencyFromPhone("+81-7303686495"), "JPY")
        assertEquals(getCurrencyFromPhone("+82-1588-8000"), "KRW")
        assertEquals(getCurrencyFromPhone("+60 3-8776 2000"), "MYR")
        assertEquals(getCurrencyFromPhone("+63-8649875404"), "PHP")
        assertEquals(getCurrencyFromPhone("+65 6595 6868"), "SGD")
        assertEquals(getCurrencyFromPhone("+886-0970852223"), "TWD")
        assertEquals(getCurrencyFromPhone("+1 818-840-8840"), "USD")
        assertEquals(getCurrencyFromPhone("+1 604-207-7077"),"CAD")
    }
}
