package one.mixin.android.ui.setting

import android.content.res.Resources
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.parcelize.Parcelize
import one.mixin.android.R
import one.mixin.android.databinding.ItemCurrencyBinding
import one.mixin.android.session.Session

class CurrencyAdapter : ListAdapter<Currency, CurrencyHolder>(Currency.DIFF_CALLBACK) {
    var currencyListener: OnCurrencyListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CurrencyHolder(ItemCurrencyBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: CurrencyHolder, position: Int) {
        getItem(position)?.let { holder.bind(it, currencyListener) }
    }
}

class CurrencyHolder(private val itemBinding: ItemCurrencyBinding) : RecyclerView.ViewHolder(itemBinding.root) {
    fun bind(currency: Currency, listener: OnCurrencyListener?) {
        itemBinding.apply {
            if (currency.name == Session.getFiatCurrency()) {
                checkIv.isVisible = true
            } else {
                checkIv.isInvisible = true
            }
            flagIv.setImageResource(currency.flag)
            name.text = itemView.context.getString(R.string.wallet_setting_currency_desc, currency.name, currency.symbol)
        }
        itemView.setOnClickListener { listener?.onClick(currency) }
    }
}

interface OnCurrencyListener {
    fun onClick(currency: Currency)
}

@Parcelize
data class Currency(
    val name: String,
    val symbol: String,
    val flag: Int,
) : Parcelable {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Currency>() {
            override fun areItemsTheSame(oldItem: Currency, newItem: Currency) =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: Currency, newItem: Currency) =
                oldItem == newItem
        }
    }
}

fun getCurrencyData(resources: Resources): List<Currency> {
    val names = resources.getStringArray(R.array.currency_names)
    val symbols = resources.getStringArray(R.array.currency_symbols).map { it.trim() }
    val flagArray = resources.obtainTypedArray(R.array.currency_flags)
    val flags = arrayListOf<Int>()
    for (i in names.indices) {
        flags.add(flagArray.getResourceId(i, 0))
    }
    flagArray.recycle()

    if (flags.size != names.size || flags.size != symbols.size) {
        return emptyList()
    }
    val currencies = arrayListOf<Currency>()
    for (i in 0 until flags.size) {
        currencies.add(Currency(names[i], symbols[i], flags[i]))
    }
    return currencies
}
