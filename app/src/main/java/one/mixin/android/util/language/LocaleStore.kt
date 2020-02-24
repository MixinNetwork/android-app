package one.mixin.android.util.language

import java.util.*

/**
 *  Interface to be used by [Lingver] for storing a Locale.
 */
interface LocaleStore {
    fun getLocale(): Locale
    fun persistLocale(locale: Locale)
}
