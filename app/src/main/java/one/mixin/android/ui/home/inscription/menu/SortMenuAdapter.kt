package one.mixin.android.ui.home.inscription.menu

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.view.isInvisible
import one.mixin.android.R
import one.mixin.android.databinding.MenuSortBinding

class SortMenuAdapter(private val context: Context, private val items: List<SortMenuData>) : BaseAdapter() {

    var checkPosition: Int = 1

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): SortMenuData = items[position]

    override fun getItemId(position: Int): Long = items[position].menu.ordinal.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding = MenuSortBinding.bind(convertView ?: LayoutInflater.from(context).inflate(R.layout.menu_sort, parent, false))
        val item = getItem(position)

        binding.iv.isInvisible = checkPosition != position

        binding.icon.setImageResource(item.iconResId)
        binding.title.setText(item.title)

        return binding.root
    }
}
