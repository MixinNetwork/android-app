package one.mixin.android.ui.home.web3.market

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.view.isInvisible
import one.mixin.android.R
import one.mixin.android.databinding.MenuTopBinding

class TopPercentageAdapter(private val context: Context, private val items: List<PercentageMenuData>) : BaseAdapter() {

    var checkPosition: Int = 1

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): PercentageMenuData = items[position]

    override fun getItemId(position: Int): Long = items[position].type.ordinal.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding = MenuTopBinding.bind(convertView ?: LayoutInflater.from(context).inflate(R.layout.menu_top, parent, false))
        val item = getItem(position)
        binding.iv.isInvisible = checkPosition != position
        binding.title.text = if (item.type == PercentageMenuType.SEVEN_DAYS) {
            binding.root.context.getString(R.string.change_percent_period_day, 7)
        } else {
            binding.root.context.getString(R.string.change_percent_period_hour, 24)
        }
        return binding.root
    }
}
