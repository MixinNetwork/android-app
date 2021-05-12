@file:Suppress("NOTHING_TO_INLINE")

package one.mixin.android.extension

import android.content.Context
import android.os.Build
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes

inline fun Context.toast(text: CharSequence, duration: ToastDuration = ToastDuration.Long): Toast {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Toast.makeText(this, text, duration.value()).apply {
            show()
        }
    } else {
        Toast.makeText(this, text, duration.value()).apply {
            @Suppress("DEPRECATION")
            view!!.findViewById<TextView>(android.R.id.message).apply {
                gravity = Gravity.CENTER
            }
            show()
        }
    }
}

inline fun Context.toast(@StringRes resId: Int, duration: ToastDuration = ToastDuration.Long): Toast {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Toast.makeText(this, resId, duration.value()).apply {
            show()
        }
    } else {
        Toast.makeText(this, resId, duration.value()).apply {
            @Suppress("DEPRECATION")
            view!!.findViewById<TextView>(android.R.id.message).apply {
                gravity = Gravity.CENTER
            }
            show()
        }
    }
}

enum class ToastDuration {
    Short { override fun value() = Toast.LENGTH_SHORT },
    Long { override fun value() = Toast.LENGTH_LONG };

    abstract fun value(): Int
}
