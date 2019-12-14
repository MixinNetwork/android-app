package one.mixin.android.vo

import android.os.Build
import com.google.gson.Gson
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import one.mixin.android.Constants.Account.PREF_FIAT_SET
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putStringSet
import one.mixin.android.util.Session

data class Fiat(val code: String, val rate: Double)

object Fiats {
    private val gson = Gson()

    private val fiatSet = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
        ConcurrentHashMap.newKeySet<Fiat>()
    } else {
        Collections.newSetFromMap(ConcurrentHashMap<Fiat, Boolean>())
    }

    private val codeSymbolMap = ConcurrentHashMap<String, String>()

    init {
        MixinApplication.appContext.apply {
            val names = resources.getStringArray(R.array.currency_names)
            val symbols = resources.getStringArray(R.array.currency_symbols)
            names.forEachIndexed { i, s ->
                codeSymbolMap[s] = symbols[i]
            }

            defaultSharedPreferences.getStringSet(PREF_FIAT_SET, null)
                ?.mapTo(fiatSet) { gson.fromJson(it, Fiat::class.java) }
        }
    }

    fun updateFiats(newFiatSet: Set<Fiat>) {
        fiatSet.clear()
        fiatSet.addAll(newFiatSet)

        val fiatStringSet = mutableSetOf<String>()
        newFiatSet.mapTo(fiatStringSet) { gson.toJson(it) }
        MixinApplication.appContext.defaultSharedPreferences.putStringSet(PREF_FIAT_SET, fiatStringSet)
    }

    fun getRate() = fiatSet.find { it.code == Session.getFiatCurrency() }?.rate.toString() ?: "1.0"

    fun getSymbol(code: String = Session.getFiatCurrency()): String = codeSymbolMap[code] ?: "$"
}
