package one.mixin.android.util

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber

fun isValidNumber(phoneUtil: PhoneNumberUtil, number: String, countryCode: String, countryDialCode: String? = null): Pair<Boolean, Phonenumber.PhoneNumber?> {
    return try {
        val phoneNumber = phoneUtil.parse(number, countryCode)
        var isValid = phoneUtil.isValidNumber(phoneNumber)

        // workaround for old registered Ivory Coast user
        // https://issuetracker.google.com/issues/190630271
        if (!isValid && countryCode == "CI") {
            isValid = addPrefixAndTry(phoneUtil, number, countryCode, countryDialCode)
        }

        Pair(isValid, phoneNumber)
    } catch (e: NumberParseException) {
        Pair(false, null)
    }
}

// +225 01 xx xx xx xx / +225 05 xx xx xx xx / +225 07 xx xx xx xx / +225 25 xx xx xx xx
// calls from outside Ivory Coast to a mobile number
private val prefixList = listOf("07", "05", "01", "25")

private fun addPrefixAndTry(phoneUtil: PhoneNumberUtil, phoneNumber: String, countryCode: String, countryDialCode: String? = null): Boolean {
    val num = if (countryDialCode != null) {
        phoneNumber.removePrefix(countryDialCode)
    } else phoneNumber
    prefixList.forEach { p ->
        val phone = phoneUtil.parse("${countryDialCode ?: ""}$p$num", countryCode)
        if (phoneUtil.isValidNumber(phone)) {
            return true
        }
    }
    return false
}
