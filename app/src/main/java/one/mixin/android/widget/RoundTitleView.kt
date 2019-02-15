package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.updateLayoutParams
import kotlinx.android.synthetic.main.view_round_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx

class RoundTitleView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_round_title, this, true)
        val ta = context.obtainStyledAttributes(attrs, R.styleable.RoundTitleView)
        if (ta != null) {
            if (ta.hasValue(R.styleable.RoundTitleView_title_text)) {
                title_tv.text = ta.getString(R.styleable.RoundTitleView_title_text)
            }
            if (ta.hasValue(R.styleable.RoundTitleView_right_icon)) {
                right_iv.setImageResource(ta.getResourceId(R.styleable.RoundTitleView_right_icon, 0))
                right_iv.visibility = View.VISIBLE
            }
            if (ta.hasValue(R.styleable.RoundTitleView_left_icon)) {
                left_iv.setImageResource(ta.getResourceId(R.styleable.RoundTitleView_left_icon, 0))
                left_iv.visibility = View.VISIBLE
            } else {
                title_tv.updateLayoutParams<RelativeLayout.LayoutParams> {
                    marginStart = context.dpToPx(20f)
                }
            }
            ta.recycle()
        }
    }
}