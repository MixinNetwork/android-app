package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.LinearLayoutCompat
import one.mixin.android.databinding.ViewSendReceiveBinding

class SendReceiveView : LinearLayoutCompat {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        binding = ViewSendReceiveBinding.inflate(LayoutInflater.from(context), this)
        weightSum = 3f
    }

    private val binding: ViewSendReceiveBinding
    val send get() = binding.send
    val receive get() = binding.receive
    val swap get() = binding.swap
    val badge get() = binding.badge

}
