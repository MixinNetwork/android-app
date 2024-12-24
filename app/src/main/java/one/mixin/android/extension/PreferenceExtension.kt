@file:Suppress("NOTHING_TO_INLINE")

package one.mixin.android.extension

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.github.salomonbrys.kotson.gsonTypeToken
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import one.mixin.android.Constants
import one.mixin.android.Constants.INTERVAL_10_MINS
import one.mixin.android.crypto.PrivacyPreference.getPrefPinInterval
import one.mixin.android.crypto.PrivacyPreference.putPrefPinInterval
import one.mixin.android.util.GsonHelper
import java.lang.reflect.Type

inline val Fragment.defaultSharedPreferences: SharedPreferences
    get() = requireContext().defaultSharedPreferences

inline val Context.defaultSharedPreferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)

inline val Context.scamPreferences: SharedPreferences
    get() = this.sharedPreferences("scam_preferences")

inline val Fragment.scamPreferences: SharedPreferences
    get() = requireContext().scamPreferences

inline fun Context.sharedPreferences(name: String): SharedPreferences =
    this.getSharedPreferences(name, MODE_PRIVATE)

inline fun SharedPreferences.clear() {
    this.edit().clear().apply()
}

inline fun SharedPreferences.putBoolean(
    key: String,
    value: Boolean,
) {
    this.edit().putBoolean(key, value).apply()
}

inline fun SharedPreferences.putFloat(
    key: String,
    value: Float,
) {
    this.edit().putFloat(key, value).apply()
}

inline fun SharedPreferences.putInt(
    key: String,
    value: Int,
) {
    this.edit().putInt(key, value).apply()
}

inline fun SharedPreferences.putLong(
    key: String,
    value: Long,
) {
    this.edit().putLong(key, value).apply()
}

inline fun SharedPreferences.putString(
    key: String,
    value: String?,
) {
    this.edit().putString(key, value).apply()
}

inline fun SharedPreferences.putStringSet(
    key: String,
    values: Set<String>?,
) {
    this.edit().putStringSet(key, values).apply()
}

inline fun SharedPreferences.remove(key: String) {
    this.edit().remove(key).apply()
}

fun Context.updatePinCheck() {
    val cur = System.currentTimeMillis()
    defaultSharedPreferences.putLong(Constants.Account.PREF_PIN_CHECK, cur)
    val interval = getPrefPinInterval(this, INTERVAL_10_MINS)
    if (interval < Constants.INTERVAL_24_HOURS) {
        var tmp = interval * 2
        if (interval * 2 > Constants.INTERVAL_24_HOURS) {
            tmp = Constants.INTERVAL_24_HOURS
        }
        putPrefPinInterval(this, tmp)
    }
}

object TypeTokenCache {
    @Suppress("UNCHECKED_CAST")
    fun <T> getListType(clazz: Class<T>): Type {
        return TypeToken.getParameterized(List::class.java, clazz).type
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getMutableListType(clazz: Class<T>): Type {
        return TypeToken.getParameterized(MutableList::class.java, clazz).type
    }
}

fun <T> SharedPreferences.addToList(key: String, item: T, clazz: Class<T>, limit: Int = 6) {
    val gson = GsonHelper.customGson
    val jsonString = getString(key, null)
    val currentList: MutableList<T> = jsonString?.let {
        gson.fromJson(it, TypeTokenCache.getMutableListType(clazz))
    } ?: mutableListOf()

    currentList.remove(item)
    currentList.add(0, item)

    if (currentList.size > limit) {
        currentList.removeAll(currentList.subList(limit, currentList.size))
    }

    edit().putString(key, gson.toJson(currentList)).apply()
}

fun <T> SharedPreferences.getList(key: String, clazz: Class<T>): List<T> {
    val gson = GsonHelper.customGson
    val jsonString = getString(key, null)
    return jsonString?.let {
        gson.fromJson(it, TypeTokenCache.getListType(clazz))
    } ?: emptyList()
}