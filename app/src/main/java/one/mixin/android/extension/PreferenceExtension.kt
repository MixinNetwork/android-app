@file:Suppress("NOTHING_TO_INLINE")
package one.mixin.android.extension

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.preference.PreferenceManager

inline val Context.defaultSharedPreferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)

inline fun Context.sharedPreferences(name: String): SharedPreferences =
        this.getSharedPreferences(name, MODE_PRIVATE)

inline fun SharedPreferences.clear() {
    this.edit().clear().apply()
}

inline fun SharedPreferences.putBoolean(key: String, value: Boolean) {
    this.edit().putBoolean(key, value).apply()
}

inline fun SharedPreferences.putFloat(key: String, value: Float) {
    this.edit().putFloat(key, value).apply()
}

inline fun SharedPreferences.putInt(key: String, value: Int) {
    this.edit().putInt(key, value).apply()
}

inline fun SharedPreferences.putLong(key: String, value: Long) {
    this.edit().putLong(key, value).apply()
}

inline fun SharedPreferences.putString(key: String, value: String?) {
    this.edit().putString(key, value).apply()
}

inline fun SharedPreferences.putStringSet(key: String, values: Set<String>?) {
    this.edit().putStringSet(key, values).apply()
}

inline fun SharedPreferences.remove(key: String) {
    this.edit().remove(key).apply()
}
