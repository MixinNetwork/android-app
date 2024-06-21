package one.mixin.android.ui.home.inscription.menu

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import one.mixin.android.R

class SortMenuAdapter(private val context: Context, private val items: List<SortMenuData>) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): SortMenuData = items[position]

    override fun getItemId(position: Int): Long = items[position].menu.ordinal.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.menu_sort, parent, false)
        val item = getItem(position)

        val icon = view.findViewById<ImageView>(R.id.icon)
        val title = view.findViewById<TextView>(R.id.title)

        icon.setImageResource(item.iconResId)
        title.setText(item.title)

        return view
    }
}
