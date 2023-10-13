package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewHeadTailBinding

class HeadTailView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    private val binding: ViewHeadTailBinding = ViewHeadTailBinding.inflate(LayoutInflater.from(context), this)
    val tail = binding.tailTv

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.HeadTailView)
        if (ta.hasValue(R.styleable.HeadTailView_head)) {
            binding.headTv.text = ta.getString(R.styleable.HeadTailView_head)
        }
        if (ta.hasValue(R.styleable.HeadTailView_tail)) {
            binding.tailTv.text = ta.getString(R.styleable.HeadTailView_tail)
        }
        ta.recycle()
    }
}
