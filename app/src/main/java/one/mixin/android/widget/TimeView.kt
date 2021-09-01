package one.mixin.android.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import one.mixin.android.databinding.ViewTimeBinding
import one.mixin.android.extension.timeAgo

class TimeView constructor(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private val binding = ViewTimeBinding.inflate(LayoutInflater.from(context), this)
    val chatTime = binding.chatTime
    fun timeAgoClock(str: String) {
        binding.chatTime.timeAgo(str)
    }

    fun setIcon(secretIcon: Drawable?, representativeIcon: Drawable?, statusIcon: Drawable?) {
        binding.secretIcon.isVisible = secretIcon != null
        binding.secretIcon.setImageDrawable(secretIcon)
        binding.representativeIcon.isVisible = representativeIcon != null
        binding.representativeIcon.setImageDrawable(representativeIcon)
        TextViewCompat.setCompoundDrawablesRelative(binding.chatTime, null, null, statusIcon, null)
    }
}
