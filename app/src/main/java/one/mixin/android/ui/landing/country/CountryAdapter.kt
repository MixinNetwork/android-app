package one.mixin.android.ui.landing.country

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlinx.android.synthetic.main.item_country.view.*
import kotlinx.android.synthetic.main.item_search_header.view.*
import one.mixin.android.R
import one.mixin.android.extension.inflate
import one.mixin.android.ui.common.recyclerview.HeaderFilterAdapter
import timber.log.Timber
import java.util.Locale

class CountryAdapter : HeaderFilterAdapter<Country>(),
    StickyRecyclerHeadersAdapter<CountryAdapter.HeaderViewHolder> {

    override var data: List<Country>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var allCount : Int? = null

    override fun filtered() = data?.size != allCount

    override fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder =
        CountryHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_country, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CountryHolder) {
            holder.bind(data!![getPos(position)], onItemListener)
        }
    }

    override fun getHeaderId(position: Int): Long {
        if (position == TYPE_HEADER) return -1
        val englishName = data?.get(getPos(position))?.englishName
        return if (englishName.isNullOrEmpty()) -1 else englishName[0].toLong()
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        val view = parent.inflate(R.layout.item_search_header, false)
        return HeaderViewHolder(view)
    }

    override fun onBindHeaderViewHolder(holder: HeaderViewHolder, pos: Int) {
        data?.get(getPos(pos))?.let {
            holder.bind(it)
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(country: Country) {
            country.englishName?.let {
                itemView.search_header_tv.text = "${it[0]}"
            }
        }
    }

    class CountryHolder(itemView: View) : NormalHolder(itemView) {
        fun bind(country: Country, listener: OnItemListener? = null) {
            val drawableName = "flag_" + country.code.toLowerCase(Locale.ENGLISH)
            val drawableId = getResId(drawableName)
            itemView.flag_iv.setImageResource(drawableId)
            itemView.name_tv.text = country.name
            itemView.setOnClickListener { listener?.onNormalItemClick(country) }
        }

        private fun getResId(drawableName: String): Int {
            try {
                val res = R.drawable::class.java
                val field = res.getField(drawableName)
                return field.getInt(null)
            } catch (e: Exception) {
                Timber.w("Failure to get drawable id.")
            }
            return -1
        }
    }
}