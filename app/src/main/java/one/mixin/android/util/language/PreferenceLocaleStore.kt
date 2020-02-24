package one.mixin.android.util.language

import android.content.Context
import android.content.SharedPreferences
import java.util.*
import org.json.JSONObject

/**
 * Default implementation of [LocaleStore] using [SharedPreferences].
 */
class PreferenceLocaleStore @JvmOverloads constructor(
    context: Context,
    private val defaultLocale: Locale = Locale.getDefault(),
    preferenceName: String = DEFAULT_PREFERENCE_NAME
) : LocaleStore {

    private val prefs = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)

    override fun getLocale(): Locale {
        // there is no predefined way to serialize/deserialize Locale object
        return if (!prefs.getString(LANGUAGE_KEY, null).isNullOrBlank()) {
            val json = JSONObject(prefs.getString(LANGUAGE_KEY, null)!!)
            val language = json.getString(LANGUAGE_JSON_KEY)
            val country = json.getString(COUNTRY_JSON_KEY)
            val variant = json.getString(VARIANT_JSON_KEY)
            Locale(language, country, variant)
        } else {
            defaultLocale
        }
    }

    override fun persistLocale(locale: Locale) {
        val json = JSONObject().apply {
            put(LANGUAGE_JSON_KEY, locale.language)
            put(COUNTRY_JSON_KEY, locale.country)
            put(VARIANT_JSON_KEY, locale.variant)
        }
        prefs.edit().putString(LANGUAGE_KEY, json.toString()).apply()
    }

    companion object {
        private const val LANGUAGE_KEY = "language_key"
        private const val DEFAULT_PREFERENCE_NAME = "lingver_preference"
        private const val LANGUAGE_JSON_KEY = "language"
        private const val COUNTRY_JSON_KEY = "country"
        private const val VARIANT_JSON_KEY = "variant"
    }
}
