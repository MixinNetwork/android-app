package one.mixin.android.vo

import android.os.Build
import com.google.gson.Gson
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import one.mixin.android.Constants.Account.PREF_CURRENCY
import one.mixin.android.Constants.Account.PREF_CURRENCY_SYMBOL
import one.mixin.android.Constants.Account.PREF_FIAT_SET
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.extension.putStringSet

data class Fiat(val code: String, val rate: Double)

object Fiats {
    private val gson = Gson()

    private val fiatSet = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
        ConcurrentHashMap.newKeySet<Fiat>()
    } else {
        Collections.newSetFromMap(ConcurrentHashMap<Fiat, Boolean>())
    }

    var currency: String = MixinApplication.appContext.defaultSharedPreferences.getString(PREF_CURRENCY, "USD") ?: "USD"
        set(value) {
            field = value
            MixinApplication.appContext.defaultSharedPreferences.putString(PREF_CURRENCY, value)
        }

    var currencySymbol = MixinApplication.appContext.defaultSharedPreferences.getString(PREF_CURRENCY_SYMBOL, "$") ?: "$"
        set(value) {
            field = value
            MixinApplication.appContext.defaultSharedPreferences.putString(PREF_CURRENCY_SYMBOL, value)
        }

    init {
        MixinApplication.appContext.defaultSharedPreferences.getStringSet(PREF_FIAT_SET, null)
            ?.mapTo(fiatSet) { gson.fromJson(it, Fiat::class.java) }
    }

    fun updateFiats(newFiatSet: Set<Fiat>) {
        fiatSet.clear()
        fiatSet.addAll(newFiatSet)

        val fiatStringSet = mutableSetOf<String>()
        newFiatSet.mapTo(fiatStringSet) { gson.toJson(it) }
        MixinApplication.appContext.defaultSharedPreferences.putStringSet(PREF_FIAT_SET, fiatStringSet)
    }

    fun getRate() = fiatSet.find { it.code == currency }?.rate ?: 1.0
}
