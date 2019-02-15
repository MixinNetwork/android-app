package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.CheckedTextView
import one.mixin.android.R
import org.jetbrains.anko.dip

class CheckedFlowItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : CheckedTextView(context, attrs, defStyle) {
    init {
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setBackgroundResource(R.drawable.bg_selector_wallet_round_gray)
        setPaddingRelative(dip(8), dip(8), dip(8), dip(8))
    }

    private var onCheckedListener: OnCheckedListener? = null

    fun setOnCheckedListener(onCheckedListener: OnCheckedListener) {
        this.onCheckedListener = onCheckedListener
    }

    interface OnCheckedListener {
        fun onChecked(id: Int)
    }
}