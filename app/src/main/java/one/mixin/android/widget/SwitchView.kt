package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Checkable
import android.widget.CompoundButton
import android.widget.FrameLayout
import androidx.annotation.StringRes
import one.mixin.android.databinding.ViewSwitchBinding

class SwitchView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs), Checkable {

    private val binding = ViewSwitchBinding.inflate(LayoutInflater.from(context), this, true)
    init {
        setOnClickListener {
            toggle()
        }
    }

    override fun isChecked(): Boolean {
        return binding.switchCompat.isChecked
    }

    override fun toggle() {
        binding.switchCompat.toggle()
    }

    override fun setChecked(checked: Boolean) {
        binding.switchCompat.isChecked = checked
    }

    fun setContent(name: String) {
        binding.switchContent.text = name
    }

    fun setContent(@StringRes name: Int) {
        binding.switchContent.setText(name)
    }

    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener) {
        binding.switchCompat.setOnCheckedChangeListener(listener)
    }
}
