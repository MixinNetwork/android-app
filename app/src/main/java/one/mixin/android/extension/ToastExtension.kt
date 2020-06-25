@file:Suppress("NOTHING_TO_INLINE")

package one.mixin.android.extension

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import androidx.annotation.StringRes
import com.dovar.dtoast.DToast
import one.mixin.android.R

inline fun Context.toast(text: String, duration: Int = DToast.DURATION_LONG) {
    DToast.make(this).setView(LayoutInflater.from(this).inflate(R.layout.view_toast, null))
        .setText(R.id.tv_content_default, text).setGravity(Gravity.BOTTOM or Gravity.CENTER, 0, 128.dp)
        .setDuration(duration).show()
}

inline fun Context.toast(@StringRes resId: Int, duration: Int = DToast.DURATION_LONG) {
    DToast.make(this).setView(LayoutInflater.from(this).inflate(R.layout.view_toast, null))
        .setText(R.id.tv_content_default, getString(resId)).setGravity(Gravity.BOTTOM or Gravity.CENTER, 0, 128.dp)
        .setDuration(duration).show()
}
