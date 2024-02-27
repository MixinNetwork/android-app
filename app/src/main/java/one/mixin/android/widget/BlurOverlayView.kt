package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import one.mixin.android.databinding.ViewBlurOverlayBinding

class BlurOverlayView: FrameLayout {

    private val _binding: ViewBlurOverlayBinding
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        _binding = ViewBlurOverlayBinding.inflate(LayoutInflater.from(context), this)
    }

    fun setKey(key: String) {
    }
}