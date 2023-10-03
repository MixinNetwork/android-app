package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import one.mixin.android.R
import one.mixin.android.databinding.ViewSegmentedBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.translationX

class SegmentedView : ConstraintLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        binding = ViewSegmentedBinding.inflate(LayoutInflater.from(context), this)
        initView()
    }

    private var onSelectListener: OnSelectListener? = null
    private val binding: ViewSegmentedBinding

    private fun initView() {
        binding.root.setPadding(2.dp)
        binding.root.setBackgroundResource(R.drawable.bg_round_window_8dp)
        binding.newTv.setOnClickListener {
            status = 1
        }
        binding.oldTv.setOnClickListener {
            status = 0
        }
    }

    fun setOnSelectListener(onSelectListener: OnSelectListener) {
        this.onSelectListener = onSelectListener
    }

    private var status = 0
        set(value) {
            if (field != value) {
                field = value
                if (value == 0) {
                    binding.thumb.translationX(0f)
                } else {
                    binding.thumb.translationX(binding.oldTv.measuredWidth.toFloat() + 8.dp)
                }
                onSelectListener?.onClick(value)
            }
        }

    interface OnSelectListener {
        fun onClick(status:Int)
    }
}
