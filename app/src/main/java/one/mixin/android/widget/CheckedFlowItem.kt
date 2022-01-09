package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatCheckedTextView
import one.mixin.android.R
import one.mixin.android.extension.dp

class CheckedFlowItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatCheckedTextView(context, attrs, defStyle) {
    init {
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setBackgroundResource(R.drawable.bg_selector_wallet_round_gray)
        setPaddingRelative(16.dp, 8.dp, 16.dp, 8.dp)
        setOnClickListener {
            if (!isChecked) {
                toggle()
            }
        }
    }

    override fun setChecked(checked: Boolean) {
        if (isChecked != checked) {
            listener?.onCheckedChanged(id, checked)
        }
        super.setChecked(checked)
    }

    private var listener: OnCheckedChangeListener? = null

    interface OnCheckedChangeListener {
        fun onCheckedChanged(id: Int, checked: Boolean)
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        this.listener = listener
    }
}
