package one.mixin.android.vo

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import one.mixin.android.Constants.Account.PREF_FIAT_MAP
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.util.Session
import java.util.concurrent.ConcurrentHashMap

data class Fiat(val code: String, val rate: Double)

object Fiats {
    private val gson = Gson()

    private val codeRateMap: ConcurrentHashMap<String, Double>

    private val codeSymbolMap = ConcurrentHashMap<String, String>()

    private val type = object : TypeToken<ConcurrentHashMap<String, Double>>() {}.type

    init {
        MixinApplication.appContext.apply {
            val names = resources.getStringArray(R.array.currency_names)
            val symbols = resources.getStringArray(R.array.currency_symbols)
            names.forEachIndexed { i, s ->
                codeSymbolMap[s] = symbols[i]
            }

            val codeRateMapString = defaultSharedPreferences.getString(PREF_FIAT_MAP, null)
            codeRateMap = if (codeRateMapString == null) {
                ConcurrentHashMap()
            } else gson.fromJson(codeRateMapString, type)
        }
    }

    fun updateFiats(newFiatList: List<Fiat>) {
        newFiatList.forEach { f ->
            codeRateMap[f.code] = f.rate
        }
        val codeRateMapString = gson.toJson(codeRateMap)
        MixinApplication.appContext.defaultSharedPreferences.putString(PREF_FIAT_MAP, codeRateMapString)
    }

    fun getRate(code: String = Session.getFiatCurrency()): Double = codeRateMap[code] ?: 1.0

    fun getSymbol(code: String = Session.getFiatCurrency()): String {
        val rateExists = codeRateMap.keys.contains(code)
        // if there is no rate info, just return '$' as default
        if (!rateExists) {
            return "$"
        }
        return codeSymbolMap[code] ?: "$"
    }
}
