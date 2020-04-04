package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class TailInputEditText constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        val str = text ?: return
        if (selStart != selEnd || selStart != str.length) {
            setSelection(str.length)
        }
    }
}
