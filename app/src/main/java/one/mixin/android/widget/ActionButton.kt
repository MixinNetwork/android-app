package one.mixin.android.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.round

class ActionButton(context: Context) : AppCompatTextView(context) {
    init {
        layoutParams =
            ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        setBackgroundResource(R.drawable.bg_action_button)
        val outValue = TypedValue()
        gravity = Gravity.CENTER
        maxLines = 1
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        foreground = ResourcesCompat.getDrawable(resources, outValue.resourceId, context.theme)
        backgroundTintList = ColorStateList.valueOf(context.colorFromAttribute(R.attr.bg_bubble))
        setPaddingRelative(8.dp, 8.dp, 8.dp, 8.dp)
        round(8.dp)
    }
}
