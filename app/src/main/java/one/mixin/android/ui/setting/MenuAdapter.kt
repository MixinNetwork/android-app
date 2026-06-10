package one.mixin.android.ui.setting

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.MenuTopBinding

class MenuAdapter(private val context: Context, private val items: List<Int>) : BaseAdapter() {

    var checkPosition: Int = 1

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Int = items[position]

    override fun getItemId(position: Int): Long = items[position].toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding = MenuTopBinding.bind(convertView ?: LayoutInflater.from(context).inflate(R.layout.menu_top, parent, false))
        val item = getItem(position)
        binding.iv.isInvisible = checkPosition != position
        binding.title.text = binding.root.context.getString(item)
        binding.tail.isVisible = true
        binding.tail.setImageResource(if (position == 0) R.drawable.ic_queto_color_green else R.drawable.ic_queto_color_red)
        return binding.root
    }
}
