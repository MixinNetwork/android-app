package one.mixin.android.widget

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import one.mixin.android.R
import one.mixin.android.extension.round
import org.jetbrains.anko.dip

class ActionButton(context: Context) : AppCompatTextView(context) {
    init {
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setBackgroundResource(R.drawable.bg_action_button)
        setPaddingRelative(dip(8), dip(8), dip(8), dip(8))
        round(dip(8f))
    }
}
