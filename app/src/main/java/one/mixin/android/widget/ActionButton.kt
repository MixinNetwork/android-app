package one.mixin.android.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.round
import org.jetbrains.anko.dip

class ActionButton(context: Context) : AppCompatTextView(context) {
    init {
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setBackgroundResource(R.drawable.bg_action_button)
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        foreground = resources.getDrawable(outValue.resourceId, context.theme)
        backgroundTintList = ColorStateList.valueOf(context.colorFromAttribute(R.attr.bg_bubble))
        setPaddingRelative(dip(8), dip(8), dip(8), dip(8))
        round(dip(8f))
    }
}
