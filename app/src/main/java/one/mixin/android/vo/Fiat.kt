package one.mixin.android.vo

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import one.mixin.android.Constants.Account.PREF_FIAT_MAP
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter
import one.mixin.android.session.Session
import java.util.concurrent.ConcurrentHashMap

@JsonClass(generateAdapter = true)
data class Fiat(val code: String, val rate: Double)

object Fiats {

    private val codeRateMap: MutableMap<String, Double>

    private val codeSymbolMap = ConcurrentHashMap<String, String>()

    private val jsonAdapter by lazy {
        getTypeAdapter<Map<String, Double>>(Types.newParameterizedType(Map::class.java, String::class.java, Double::class.javaObjectType))
    }

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
            } else {
                ConcurrentHashMap(jsonAdapter.fromJson(codeRateMapString)!!)
            }
        }
    }

    fun updateFiats(newFiatList: List<Fiat>) {
        newFiatList.forEach { f ->
            codeRateMap[f.code] = f.rate
        }
        val codeRateMapString = jsonAdapter.toJson(codeRateMap)
        MixinApplication.appContext.defaultSharedPreferences.putString(PREF_FIAT_MAP, codeRateMapString)
    }

    fun isRateEmpty() = codeRateMap.isEmpty()

    fun getRate(code: String = Session.getFiatCurrency()): Double = codeRateMap[code] ?: 1.0

    fun getSymbol(code: String = Session.getFiatCurrency()): String {
        val rateExists = codeRateMap.keys.contains(code)
        // if there is no rate info, just return '$' as default
        if (!rateExists) {
            return "$"
        }
        return codeSymbolMap[code] ?: "$"
    }

    fun getAccountCurrencyAppearance(): String {
        val code = Session.getFiatCurrency()
        val rateExists = codeRateMap.keys.contains(code)
        if (!rateExists) {
            return "USD"
        }
        return code
    }
}
