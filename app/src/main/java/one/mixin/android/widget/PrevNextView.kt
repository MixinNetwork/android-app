package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import androidx.appcompat.widget.LinearLayoutCompat
import one.mixin.android.databinding.ViewPrevNextBinding

class PrevNextView(context: Context, attrs: AttributeSet) : LinearLayoutCompat(context, attrs) {
    private val binding = ViewPrevNextBinding.inflate(LayoutInflater.from(context), this)
    val prev get() = binding.prevIv
    val next get() = binding.nextIv

    init {
        gravity = Gravity.CENTER_VERTICAL
    }
}
