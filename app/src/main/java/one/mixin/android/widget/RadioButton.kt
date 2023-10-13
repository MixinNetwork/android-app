package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Checkable
import android.widget.FrameLayout
import androidx.annotation.StringRes
import one.mixin.android.R
import one.mixin.android.databinding.ViewRadioBinding

class RadioButton(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs), Checkable {

    private var checked: Boolean = false
    private val binding = ViewRadioBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RadioButton)

        binding.title.text = typedArray.getText(R.styleable.RadioButton_android_text)
        checked = typedArray.getBoolean(R.styleable.RadioButton_android_checked, false)
        typedArray.recycle()
        if (checked) {
            binding.checkIv.setImageResource(R.drawable.ic_check_blue_24dp)
        } else {
            binding.checkIv.setImageDrawable(null)
        }
        setOnClickListener {
            if (!isChecked) {
                toggle()
            }
        }
    }

    fun setText(text: String) {
        binding.title.text = text
    }

    fun setText(@StringRes resId: Int) {
        binding.title.setText(resId)
    }

    override fun isChecked(): Boolean {
        return checked
    }

    override fun toggle() {
        isChecked = !checked
    }

    override fun setChecked(checked: Boolean) {
        if (this.checked != checked) {
            listener?.onCheckedChanged(id, checked)
        }
        this.checked = checked
        if (checked) {
            binding.checkIv.setImageResource(R.drawable.ic_check_blue_24dp)
        } else {
            binding.checkIv.setImageDrawable(null)
        }
    }

    private var listener: OnCheckedChangeListener? = null

    interface OnCheckedChangeListener {
        fun onCheckedChanged(id: Int, checked: Boolean)
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        this.listener = listener
    }
}
