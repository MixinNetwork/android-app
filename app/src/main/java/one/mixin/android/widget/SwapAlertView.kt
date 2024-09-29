package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.LinearLayoutCompat
import one.mixin.android.R
import one.mixin.android.databinding.ViewSwapAlertBinding

class SwapAlertView : LinearLayoutCompat {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        binding = ViewSwapAlertBinding.inflate(LayoutInflater.from(context), this)
        weightSum = 2f
        setBackgroundResource(R.drawable.bg_round_light_gray_12dp)
    }

    private val binding: ViewSwapAlertBinding
    val swap get() = binding.swapTv
    val alertVa get() = binding.alertVa

}
