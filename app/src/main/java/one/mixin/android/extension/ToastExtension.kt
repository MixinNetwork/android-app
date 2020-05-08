@file:Suppress("NOTHING_TO_INLINE")

package one.mixin.android.extension

import android.content.Context
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes

inline fun Context.toast(text: CharSequence, duration: Int = Toast.LENGTH_LONG): Toast {
    return Toast.makeText(this, text, duration).apply {
        view.findViewById<TextView>(android.R.id.message).apply {
            gravity = Gravity.CENTER
        }
        show()
    }
}

inline fun Context.toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_LONG): Toast {
    return Toast.makeText(this, resId, duration).apply {
        view.findViewById<TextView>(android.R.id.message).apply {
            gravity = Gravity.CENTER
        }
        show()
    }
}
