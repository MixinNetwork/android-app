package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewPercentItemBinding

class PercentItemView(context: Context) : LinearLayout(context) {

    private val binding = ViewPercentItemBinding.inflate(LayoutInflater.from(context), this)

    init {
        val params = LinearLayout.LayoutParams(0, WRAP_CONTENT)
        params.weight = 1f
        gravity = Gravity.CENTER
        layoutParams = params
    }

    @SuppressLint("SetTextI18n")
    fun setPercentItem(item: PercentView.PercentItem, index: Int) {
        binding.color.setImageResource(
            when (index) {
                0 -> R.drawable.ic_rect_percent_first
                1 -> R.drawable.ic_rect_percent_second
                else -> R.drawable.ic_rect_percent_other
            }
        )
        binding.name.text = item.name
        binding.percent.text = "${(item.percent * 100).toInt()}%"
    }
}
