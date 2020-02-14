package one.mixin.android.util.markdown

import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance

class RemoveUnderlineSpan : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) {
        tp.isUnderlineText = false
    }
}
