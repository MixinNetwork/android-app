package one.mixin.android.util

import android.content.Context
import android.os.Build
import android.os.LocaleList
import java.util.Locale
import one.mixin.android.Constants
import one.mixin.android.extension.defaultSharedPreferences

const val SIMPLIFIED_CHINESE = "zh"
const val ENGLISH = "en"

val supportedLanguage = hashMapOf<String, Locale>().apply {
    put(ENGLISH, Locale.ENGLISH)
    put(SIMPLIFIED_CHINESE, Locale.SIMPLIFIED_CHINESE)
}

fun Context.changeLanguage(): Context {
    val language = getAppLanguage()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val locale = getLocaleByLanguage(language)
        val configuration = resources.configuration
        configuration.setLocale(locale)
        configuration.setLocales(LocaleList(locale))
        return createConfigurationContext(configuration)
    } else {
        this
    }
}

@Suppress("DEPRECATION")
private fun getLocaleByLanguage(language: String) = supportedLanguage[language] ?: Locale.ENGLISH

private fun Context.getAppLanguage(): String {
    val defaultLang = Locale.getDefault().language
    return defaultSharedPreferences.getString(Constants.Account.PREF_LANGUAGE, defaultLang) ?: defaultLang
}
