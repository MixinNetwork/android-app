package one.mixin.android.ui.landing.country

import android.content.Context
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.Editable
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import kotlinx.android.synthetic.main.fragment_country.*
import kotlinx.android.synthetic.main.view_country_header.view.*
import one.mixin.android.R
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.landing.country.Country.Companion.ENCODED_COUNTRY_CODE
import one.mixin.android.widget.SearchView
import org.json.JSONArray
import java.util.Locale

class CountryFragment : Fragment() {
    companion object {
        const val TAG = "CountryFragment"

        fun newInstance() = CountryFragment()
    }

    private var selectedCountry: Country? = null
    var locationCountry: Country? = null

    private val countries = arrayListOf<Country>()

    var callback: Callback? = null

    private val adapter: CountryAdapter by lazy {
        CountryAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_country, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        country_rv.layoutManager = LinearLayoutManager(requireContext())
        country_rv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        country_rv.adapter = adapter
        val header = layoutInflater.inflate(R.layout.view_country_header, country_rv, false)
        adapter.headerView = header
        adapter.onItemListener = object : HeaderAdapter.OnItemListener {
            override fun <T> onNormalItemClick(item: T) {
                item as Country
                search_et?.text?.clear()
                selectedCountry = item
                callback?.onSelectCountry(item)
            }
        }

        search_et.listener = object : SearchView.OnSearchViewListener {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    adapter.data = countries
                } else {
                    filter(s.toString())
                }
            }

            override fun onSearch() {
            }
        }

        if (selectedCountry == null) {
            selectedCountry = locationCountry
        }
        selectedCountry?.let {
            header.selected_iv.setImageResource(it.flag)
            header.selected_tv.text = it.name
        }
        locationCountry?.let {
            header.location_iv.setImageResource(it.flag)
            header.location_tv.text = it.name
        }
    }

    fun getUserCountryInfo(context: Context): Country {
        getAllCountries(context)
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (telephonyManager.simState != TelephonyManager.SIM_STATE_ABSENT) {
            return getCountry(telephonyManager.simCountryIso)
        }

        return getCountry(Locale.getDefault().country)
    }

    private fun filter(s: String) {
        val filteredCountries = arrayListOf<Country>()
        countries.forEach {
            if (it.name?.toLowerCase(Locale.ENGLISH)?.contains(s.toLowerCase()) == true) {
                filteredCountries.add(it)
            }
        }
        adapter.data = filteredCountries
    }

    private fun getCountry(countryIsoCode: String): Country {
        return countries.find { it.code == countryIsoCode } ?: afghanistan()
    }

    private fun afghanistan() = Country("AF", dialCode = "+93", flag = R.drawable.flag_af)

    private fun getAllCountries(context: Context) {
        countries.clear()
        val allCountriesCode = readEncodedJsonString()
        val countryArray = JSONArray(allCountriesCode)
        for (i in 0 until countryArray.length()) {
            val jsonObject = countryArray.getJSONObject(i)
            val countryDialCode = jsonObject.getString("dial_code")
            val countryCode = jsonObject.getString("code")
            val country = Country(countryCode, dialCode = countryDialCode, flag = getFlagResId(context, countryCode))
            countries.add(country)
        }
        adapter.data = countries
        adapter.allCount = countries.size
    }

    private fun readEncodedJsonString(): String {
        val data = Base64.decode(ENCODED_COUNTRY_CODE, Base64.DEFAULT)
        return String(data, Charsets.UTF_8)
    }

    private fun getFlagResId(context: Context, drawable: String): Int {
        return try {
            context.resources.getIdentifier("flag_" + drawable.toLowerCase(Locale.ENGLISH),
                "drawable", context.packageName)
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    interface Callback {
        fun onSelectCountry(country: Country)
    }
}