package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Checkable
import android.widget.CompoundButton
import android.widget.FrameLayout
import androidx.annotation.StringRes
import one.mixin.android.databinding.ViewCheckBinding
import one.mixin.android.extension.fileSize

class CheckView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs), Checkable {

    private val binding = ViewCheckBinding.inflate(LayoutInflater.from(context), this, true)
    init {
        setOnClickListener {
            toggle()
        }
    }

    override fun isChecked(): Boolean {
        return binding.checkBox.isChecked
    }

    override fun toggle() {
        binding.checkBox.isChecked = !binding.checkBox.isChecked
    }

    override fun setChecked(checked: Boolean) {
        binding.checkBox.isChecked = checked
    }

    fun setName(name: String) {
        binding.nameTv.text = name
    }

    fun setName(@StringRes name: Int) {
        binding.nameTv.setText(name)
    }

    fun setSize(size: Long) {
        binding.storageTv.text = size.fileSize()
    }

    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener) {
        binding.checkBox.setOnCheckedChangeListener(listener)
    }
}
