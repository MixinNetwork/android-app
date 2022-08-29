package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.LinearLayoutCompat
import one.mixin.android.databinding.ViewShadowCircleBinding

class ShadowCircleView : LinearLayoutCompat {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        binding = ViewShadowCircleBinding.inflate(LayoutInflater.from(context), this)
    }

    private val binding: ViewShadowCircleBinding
    val more get() = binding.more
    val firstIv get() = binding.firstIv
    val secondIv get() = binding.secondIv
    val thirdIv get() = binding.thirdIv
}
