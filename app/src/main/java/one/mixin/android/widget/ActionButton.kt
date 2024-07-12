package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.setMargins
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.round

@SuppressLint("ViewConstructor")
class ActionButton(context: Context, externalLink: Boolean = false, sendLink: Boolean = false) : FrameLayout(context) {

    val textView = AppCompatTextView(context)

    init {
        layoutParams =
            MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        setBackgroundResource(R.drawable.bg_action_button)

        textView.gravity = Gravity.CENTER
        textView.maxLines = 1
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        textView.ellipsize = TextUtils.TruncateAt.END
        val outValue = TypedValue()
        addView(textView, LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = 4.dp
            bottomMargin = 4.dp
            gravity = Gravity.CENTER
        })

        if (externalLink) {
            val icon = AppCompatImageView(context)
            icon.setImageResource(R.drawable.ic_external_link)
            addView(icon, LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.END or Gravity.TOP
            })
        } else if (sendLink) {
            val icon = AppCompatImageView(context)
            icon.setImageResource(R.drawable.ic_send_link)
            addView(icon, LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.END or Gravity.TOP
            })
        }
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        foreground = ResourcesCompat.getDrawable(resources, outValue.resourceId, context.theme)
        backgroundTintList = ColorStateList.valueOf(context.colorFromAttribute(R.attr.bg_bubble))
        setPaddingRelative(4.dp, 4.dp, 4.dp, 4.dp)
        round(8.dp)
    }


    fun setTypeface(tf: Typeface?, style: Int) {
        textView.setTypeface(tf, style)
    }

    fun setText(text: CharSequence?) {
        textView.text = text?.trim()
    }

    fun setTextColor(@ColorInt color: Int) {
        textView.setTextColor(color)
    }

}
