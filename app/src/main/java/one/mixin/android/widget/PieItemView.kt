package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_pie_item.view.*
import one.mixin.android.R

class PieItemView(context: Context) : LinearLayout(context) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_pie_item, this, true)
        val params = LinearLayout.LayoutParams(0, WRAP_CONTENT)
        params.weight = 1f
        gravity = Gravity.CENTER
        layoutParams = params
    }

    @SuppressLint("SetTextI18n")
    fun setPieItem(item: PieView.PieItem, index: Int) {
        color.setImageResource(when (index) {
            0 -> R.drawable.ic_circle_pie_first
            1 -> R.drawable.ic_circle_pie_second
            else -> R.drawable.ic_circle_pie_other
        })
        name.text = item.name
        percent.text = "${(item.percent * 100).toInt()}%"
    }
}
