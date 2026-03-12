@file:JvmName("CountryAdapterKt")

package one.mixin.android.widget.countrypicker

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import java.util.*
import kotlin.code
import kotlin.collections.ArrayList

class CountryAdapter(
    private val countries: List<Country>,
    private val onClickListener: (Country) -> Unit
) : HeaderAdapter<RecyclerView.ViewHolder>(), SectionIndexer, StickyRecyclerHeadersAdapter<HeaderViewHolder> {
    private val sectionPositions = ArrayList<Int>(26)

    override fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder {
        return CountryHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_normal, parent, false))
    }

    override fun getHeaderId(position: Int): Long {
        val pos = getPos(position)
        if (pos == -1) return -1L
        return countries[getPos(position)].englishName[0].code.toLong()
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        return HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false))
    }

    override fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int) {
        countries[getPos(position)].let { holder.bind(it) }
    }

    override fun getItemCount() = countries.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CountryHolder) {
            countries[position].let { holder.bind(it, onClickListener) }
        }
    }

    override fun getSections(): Array<String> {
        val sections = ArrayList<String>(26)
        countries.forEachIndexed { i, c ->
            val section = c.englishName[0].uppercaseChar().toString()
            if (!sections.contains(section)) {
                sections.add(section)
                sectionPositions.add(i)
            }
        }
        return sections.toTypedArray()
    }

    override fun getPositionForSection(sectionIndex: Int): Int = sectionPositions[sectionIndex]

    override fun getSectionForPosition(position: Int): Int = 0
}

class CountryHolder(itemView: View) : NormalHolder(itemView) {
    fun bind(country: Country, onClickListener: (Country) -> Unit) {
        val drawableName = "flag_" + country.code.lowercase(Locale.ENGLISH)
        val drawableId = itemView.context.resIdByName(drawableName, "drawable")
        if (drawableId != -1) {
            itemView.findViewById<ImageView>(R.id.icon).setImageResource(drawableId)
        }
        itemView.findViewById<TextView>(R.id.normal).text = country.name
        itemView.findViewById<TextView>(R.id.code).text = country.dialCode
        itemView.setOnClickListener { onClickListener.invoke(country) }
    }
}

class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(country: Country) {
        itemView.findViewById<TextView>(R.id.header).text = country.englishName[0].toString()
    }
}

@SuppressLint("DiscouragedApi")
fun Context.resIdByName(resIdName: String?, resType: String): Int {
    resIdName?.let {
        return resources.getIdentifier(it, resType, packageName)
    }
    return -1
}