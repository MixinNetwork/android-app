package one.mixin.android.ui.wallet.fiatmoney

import android.content.Context
import com.google.i18n.phonenumbers.PhoneNumberUtil
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.Currency
import timber.log.Timber

fun getDefaultCurrency(
    context: Context,
    supportCurrencies: List<Currency>,
): String {
    val currency =
        context.defaultSharedPreferences.getString(
            CalculateFragment.CURRENT_CURRENCY,
            getCurrencyFromPhoneNumber(Session.getAccount()?.phone),
        ).let {
            if (it == null) {
                val localCurrency = Session.getFiatCurrency()
                val c = supportCurrencies.find { item -> localCurrency == item.name }
                c?.name ?: supportCurrencies.last().name
            } else {
                it
            }
        }
    return currency
}

private val phoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

fun getCountryCodeFromPhoneNumber(phone: String?): String? {
    if (phone == null) {
        return null
    }
    return try {
        val number = phoneNumberUtil.parse(phone, "")
        val countryCode = number.countryCode
        phoneNumberUtil.getRegionCodeForCountryCode(countryCode)
    } catch (e: Exception) {
        null
    }
}

fun getCurrencyFromPhoneNumber(phone: String?): String? {
    if (phone == null) {
        return null
    }
    return try {
        val number = phoneNumberUtil.parse(phone, "")
        val countryCode = number.countryCode
        val country = phoneNumberUtil.getRegionCodeForCountryCode(countryCode)
        if (country == "AE") {
            "AED"
        } else if (country == "AU") {
            "AUD"
        } else if (country == "US") {
            // North America region
            val isValid = phoneNumberUtil.isValidNumberForRegion(number, "CA")
            if (isValid) "CAD" else "USD"
        } else if (country == "CA") {
            "CAD"
        } else if (listOf("IE", "FR", "DE", "AT", "BE", "BG", "CY", "HR", "EE", "FI", "GR", "IT", "LV", "LT", "LU", "MT", "NL", "PT", "SK", "SI", "ES").contains(country)) {
            "EUR"
        } else if (country == "GB") {
            "GBP"
        } else if (country == "HK") {
            "HKD"
        } else if (country == "ID") {
            "IDR"
        } else if (country == "JP") {
            "JPY"
        } else if (country == "KR") {
            "KRW"
        } else if (country == "MY") {
            "MYR"
        } else if (country == "PH") {
            "PHP"
        } else if (country == "SG") {
            "SGD"
        } else if (country == "TW") {
            "TWD"
        } else if (country == "TR") {
            "TRY"
        } else if (country == "VN") {
            "VND"
        } else if (country == "IN") {
            "INR"
        } else {
            null
        }
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}
