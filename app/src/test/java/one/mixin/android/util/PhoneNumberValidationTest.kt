package one.mixin.android.util

import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlin.test.Test

class PhoneNumberValidationTest {
    private val phoneUtil = PhoneNumberUtil.getInstance()
    private val countryCode = "CI"
    private val countryDialCode = "+225"

    @Test
    fun `test Ivory Coast phone number is valid`() {
        val num1 = "+22548801211"
        val num2 = "+2250748801211"
        val num3 = "+2250548801211"
        val num4 = "+2250148801211"
        val num5 = "+2252548801211"
        val num6 = "48801211"
        val num7 = "0748801211"
        val num8 = "0548801211"
        val num9 = "0148801211"
        val num10 = "2548801211"

        assert(isValid(num1))
        assert(isValid(num2))
        assert(isValid(num3))
        assert(isValid(num4))
        assert(isValid(num5))
        assert(isValid(num6))
        assert(isValid(num7))
        assert(isValid(num8))
        assert(isValid(num9))
        assert(isValid(num10))
        assert(isValidWithoutDialCode(num6))
        assert(isValidWithoutDialCode(num7))
        assert(isValidWithoutDialCode(num8))
        assert(isValidWithoutDialCode(num9))
        assert(isValidWithoutDialCode(num10))
    }

    private fun isValid(num: String) = isValidNumber(phoneUtil, num, countryCode, countryDialCode).first
    private fun isValidWithoutDialCode(num: String) = isValidNumber(phoneUtil, num, countryCode).first
}
