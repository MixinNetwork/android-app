@file:Suppress("NOTHING_TO_INLINE")

package one.mixin.android.extension

import android.content.Context
import android.os.Build
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes

inline fun Context.toast(text: CharSequence, duration: Int = Toast.LENGTH_LONG): Toast {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Toast.makeText(this, text, duration).apply {
            show()
        }
    } else {
        Toast.makeText(this, text, duration).apply {
            @Suppress("DEPRECATION")
            view!!.findViewById<TextView>(android.R.id.message).apply {
                gravity = Gravity.CENTER
            }
            show()
        }
    }
}

inline fun Context.toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_LONG): Toast {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Toast.makeText(this, resId, duration).apply {
            show()
        }
    } else {
        Toast.makeText(this, resId, duration).apply {
            @Suppress("DEPRECATION")
            view!!.findViewById<TextView>(android.R.id.message).apply {
                gravity = Gravity.CENTER
            }
            show()
        }
    }
}
