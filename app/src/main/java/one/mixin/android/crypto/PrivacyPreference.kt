package one.mixin.android.crypto

import android.content.Context
import android.content.SharedPreferences
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.extension.remove
import one.mixin.android.extension.sharedPreferences

object PrivacyPreference {
    private const val PREF_PIN_INTERVAL = "pref_pin_interval"
    private const val IS_LOADED = "is_loaded"
    private const val IS_SYNC_SESSION = "is_sync_session"

    private fun getPrivacyPreference(context: Context): SharedPreferences = context.sharedPreferences("privacy_preferences")

    fun clearPrivacyPreferences(context: Context) {
        context.defaultSharedPreferences.remove(PREF_PIN_INTERVAL)
        getPrivacyPreference(context).remove(PREF_PIN_INTERVAL)
        context.defaultSharedPreferences.remove(IS_LOADED)
        getPrivacyPreference(context).remove(IS_LOADED)
        context.defaultSharedPreferences.remove(IS_SYNC_SESSION)
        getPrivacyPreference(context).remove(IS_SYNC_SESSION)
    }

    private fun getPreference(context: Context, key: String): SharedPreferences {
        val privacyPreference = getPrivacyPreference(context)
        return if (privacyPreference.contains(key)) {
            privacyPreference
        } else {
            context.defaultSharedPreferences
        }
    }

    fun getPrefPinInterval(context: Context, defaultValue: Long): Long = getPreference(context, PREF_PIN_INTERVAL).getLong(PREF_PIN_INTERVAL, defaultValue)
    fun putPrefPinInterval(context: Context, value: Long) {
        getPrivacyPreference(context).putLong(PREF_PIN_INTERVAL, value)
    }

    fun getIsLoaded(context: Context, defaultValue: Boolean): Boolean = getPreference(context, IS_LOADED).getBoolean(IS_LOADED, defaultValue)
    fun putIsLoaded(context: Context, value: Boolean) = getPrivacyPreference(context).putBoolean(IS_LOADED, value)

    fun getIsSyncSession(context: Context, defaultValue: Boolean): Boolean = getPreference(context, IS_SYNC_SESSION).getBoolean(IS_SYNC_SESSION, defaultValue)
    fun putIsSyncSession(context: Context, value: Boolean) = getPrivacyPreference(context).putBoolean(IS_SYNC_SESSION, value)
}
