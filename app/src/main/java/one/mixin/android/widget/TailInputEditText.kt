package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText

class TailInputEditText constructor(
    context: Context,
    attrs: AttributeSet? = null
) : EditText(context, attrs) {

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (selStart != selEnd || selStart != text.length) {
            setSelection(text.length)
        }
    }
}
