package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_percent_item.view.*
import one.mixin.android.R

class PercentItemView(context: Context) : LinearLayout(context) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_percent_item, this, true)
        val params = LinearLayout.LayoutParams(0, WRAP_CONTENT)
        params.weight = 1f
        gravity = Gravity.CENTER
        layoutParams = params
    }

    @SuppressLint("SetTextI18n")
    fun setPercentItem(item: PercentView.PercentItem, index: Int) {
        color.setImageResource(
            when (index) {
                0 -> R.drawable.ic_rect_percent_first
                1 -> R.drawable.ic_rect_percent_second
                else -> R.drawable.ic_rect_percent_other
            }
        )
        name.text = item.name
        percent.text = "${(item.percent * 100).toInt()}%"
    }
}
