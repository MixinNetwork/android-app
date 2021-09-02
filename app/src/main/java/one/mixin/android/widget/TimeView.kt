package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import one.mixin.android.R
import one.mixin.android.databinding.ViewTimeBinding
import one.mixin.android.extension.timeAgoClock
import org.jetbrains.anko.textColor

class TimeView constructor(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private val binding = ViewTimeBinding.inflate(LayoutInflater.from(context), this)
    val chatTime = binding.chatTime

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.TimeView)
        if (ta.hasValue(R.styleable.TimeView_text_color)) {
            binding.chatTime.textColor = ta.getColor(
                R.styleable.TimeView_text_color,
                Color.WHITE
            )
        }
        ta.recycle()
    }

    fun timeAgoClock(str: String) {
        binding.chatTime.timeAgoClock(str)
    }

    fun setIcon(secretIcon: Drawable?, representativeIcon: Drawable?, statusIcon: Drawable?) {
        binding.secretIcon.isVisible = secretIcon != null
        binding.secretIcon.setImageDrawable(secretIcon)
        binding.representativeIcon.isVisible = representativeIcon != null
        binding.representativeIcon.setImageDrawable(representativeIcon)
        TextViewCompat.setCompoundDrawablesRelative(binding.chatTime, null, null, statusIcon, null)
    }
}
