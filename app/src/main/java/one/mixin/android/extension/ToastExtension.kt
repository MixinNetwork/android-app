@file:Suppress("NOTHING_TO_INLINE")

package one.mixin.android.extension

import android.content.Context
import androidx.annotation.StringRes
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast

inline fun Context.toast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT): Toast {
    return Toast.makeText(this, text, duration).apply {
        view.findViewById<TextView>(android.R.id.message).apply {
            gravity = Gravity.CENTER
        }
        show()
    }
}

inline fun Context.toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT): Toast {
    return Toast.makeText(this, resId, duration).apply {
        view.findViewById<TextView>(android.R.id.message).apply {
            gravity = Gravity.CENTER
        }
        show()
    }
}