package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import one.mixin.android.R
import one.mixin.android.extension.dp

@SuppressLint("ViewConstructor")
class ActionButton(context: Context, externalLink: Boolean = false, sendLink: Boolean = false) : FrameLayout(context) {

    val textView = TextView(context).apply {
        background = ContextCompat.getDrawable(context, R.drawable.ripple_button)
        setPaddingRelative(14.dp, 8.dp, 14.dp, 8.dp)
    }

    init {
        layoutParams =
            MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )

        textView.gravity = Gravity.CENTER
        textView.maxLines = 1
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        textView.ellipsize = TextUtils.TruncateAt.END
        addView(textView, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = 0
            gravity = Gravity.CENTER
        })

        if (externalLink || sendLink) {
            val icon = AppCompatImageView(context)
            icon.setImageResource(if (sendLink) R.drawable.ic_send_link else R.drawable.ic_external_link)
            addView(icon, LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 4.dp
                marginEnd = 4.dp
                gravity = Gravity.END or Gravity.TOP
            })
        }
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
