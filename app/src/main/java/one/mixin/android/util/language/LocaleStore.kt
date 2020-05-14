package one.mixin.android.util.language

import java.util.Locale

/**
 *  Interface to be used by [Lingver] for storing a Locale.
 */
interface LocaleStore {
    fun getLocale(): Locale
    fun persistLocale(locale: Locale)

    fun setFollowSystemLocale(value: Boolean)
    fun isFollowingSystemLocale(): Boolean
}
