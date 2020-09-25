package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.view_fail_load.view.*
import one.mixin.android.R

class FailLoadView(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {

    var listener: FailLoadListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_fail_load, this, true)

        reload_tv.setOnClickListener {
            listener?.onReloadClick()
        }
        contact_tv.setOnClickListener {
            listener?.onContactClick()
        }
    }

    interface FailLoadListener {
        fun onReloadClick()
        fun onContactClick()
    }
}
