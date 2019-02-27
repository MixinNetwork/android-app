package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import android.widget.ImageView
import one.mixin.android.R

class CheckedImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ImageView(context, attrs, defStyle), Checkable {
    private var checked: Boolean = false

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CheckedImageView)
        checked = typedArray.getBoolean(R.styleable.CheckedImageView_android_checked, false)
        typedArray.recycle()
    }

    override fun isChecked() = checked

    override fun toggle() {
        checked = !checked
    }

    override fun setChecked(checked: Boolean) {
        this.checked = checked
    }
}