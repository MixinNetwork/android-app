package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewWebControlBinding

class WebControlView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    var mode = false
        set(value) {
            if (value != field) {
                field = value
                updateByMode(value)
            }
        }

    var callback: Callback? = null
    val binding = ViewWebControlBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL
        weightSum = 2f
        binding.moreFl.setOnClickListener { callback?.onMoreClick() }
        binding.closeFl.setOnClickListener { callback?.onCloseClick() }
        updateByMode(false)
    }

    private fun updateByMode(dark: Boolean) {
        if (dark) {
            setBackgroundResource(R.drawable.bg_view_web_control_black)
            binding.moreIv.setImageResource(R.drawable.ic_more_horiz_white_24dp)
            binding.closeIv.setImageResource(R.drawable.ic_close_white_24dp)
            binding.divide.setBackgroundColor(context.getColor(R.color.bgWhiteNight))
        } else {
            setBackgroundResource(R.drawable.bg_view_web_control_white)
            binding.moreIv.setImageResource(R.drawable.ic_more_horiz_black_24dp)
            binding.closeIv.setImageResource(R.drawable.ic_close_dark_24dp)
            binding.divide.setBackgroundColor(context.getColor(R.color.bgWhite))
        }
    }

    interface Callback {
        fun onMoreClick()
        fun onCloseClick()
    }
}
