package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_web_control.view.*
import one.mixin.android.R

class WebControlView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    var mode = false
        set(value) {
            if (value != field) {
                field = value
                updateByMode(value)
            }
        }

    var callback: Callback? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_web_control, this, true)
        orientation = HORIZONTAL
        weightSum = 2f
        more_iv.setOnClickListener { callback?.onMoreClick() }
        close_iv.setOnClickListener { callback?.onCloseClick() }
        updateByMode(false)
    }

    private fun updateByMode(dark: Boolean) {
        if (dark) {
            setBackgroundResource(R.drawable.bg_view_web_control_black)
            more_iv.setImageResource(R.drawable.ic_more_horiz_white_24dp)
            close_iv.setImageResource(R.drawable.ic_close_white_24dp)
            divide.setBackgroundColor(Color.parseColor("#1Affffff"))
        } else {
            setBackgroundResource(R.drawable.bg_view_web_control_white)
            more_iv.setImageResource(R.drawable.ic_more_horiz_black_24dp)
            close_iv.setImageResource(R.drawable.ic_close_dark_24dp)
            divide.setBackgroundColor(Color.parseColor("#0F000000"))
        }
    }

    interface Callback {
        fun onMoreClick()
        fun onCloseClick()
    }
}
