@file:Suppress("NOTHING_TO_INLINE")

package one.mixin.android.extension

import android.os.Build
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import one.mixin.android.MixinApplication
import one.mixin.android.util.getLocalString

inline fun toast(text: CharSequence, duration: ToastDuration = ToastDuration.Long): Toast {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Toast.makeText(MixinApplication.appContext, text, duration.value()).apply {
            show()
        }
    } else {
        Toast.makeText(MixinApplication.appContext, text, duration.value()).apply {
            @Suppress("DEPRECATION")
            view!!.findViewById<TextView>(android.R.id.message).apply {
                gravity = Gravity.CENTER
            }
            show()
        }
    }
}

inline fun toast(@StringRes resId: Int, duration: ToastDuration = ToastDuration.Long): Toast {
    val text = getLocalString(MixinApplication.appContext,resId)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Toast.makeText(MixinApplication.appContext, text, duration.value()).apply {
            show()
        }
    } else {
        Toast.makeText(MixinApplication.appContext, text, duration.value()).apply {
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
    Long { override fun value() = Toast.LENGTH_LONG }, ;

    abstract fun value(): Int
}
