package one.mixin.android.extension

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.util.ArrayMap
import androidx.fragment.app.Fragment
import org.jetbrains.anko.AlertBuilder
import org.jetbrains.anko.AnkoContext
import org.jetbrains.anko.alert
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.longToast
import org.jetbrains.anko.progressDialog
import org.jetbrains.anko.selector

inline fun Fragment.toast(textResource: Int) = requireActivity().toast(textResource)

inline fun Fragment.toast(text: CharSequence) = requireActivity().toast(text)

inline fun Fragment.longToast(textResource: Int) = requireActivity().longToast(textResource)

inline fun Fragment.longToast(text: CharSequence) = requireActivity().longToast(text)

inline fun Fragment.selector(
    title: CharSequence? = null,
    items: List<CharSequence>,
    noinline onClick: (DialogInterface, Int) -> Unit
): Unit = requireActivity().selector(title, items, onClick)

inline fun Fragment.alert(
    message: String,
    title: String? = null,
    noinline init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = requireActivity().alert(message, title, init)

inline fun Fragment.alert(
    message: Int,
    title: Int? = null,
    noinline init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = requireActivity().alert(message, title, init)

inline fun Fragment.alert(noinline init: AlertBuilder<DialogInterface>.() -> Unit) = requireActivity().alert(init)

inline fun Fragment.progressDialog(
    message: String? = null,
    title: String? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = requireActivity().progressDialog(message, title, init)

inline fun Fragment.indeterminateProgressDialog(message: String? = null, title: String? = null, noinline init: (ProgressDialog.() -> Unit)? = null): ProgressDialog {
    return requireActivity().indeterminateProgressDialog(message, title, init)
}

inline fun Fragment.progressDialog(message: Int? = null, title: Int? = null, noinline init: (ProgressDialog.() -> Unit)? = null): ProgressDialog {
    return requireActivity().progressDialog(message?.let { requireActivity().getString(it) }, title?.let { requireActivity().getString(it) }, init)
}

inline fun Fragment.indeterminateProgressDialog(message: Int? = null, title: Int? = null, noinline init: (ProgressDialog.() -> Unit)? = null): ProgressDialog {
    return requireActivity().indeterminateProgressDialog(message?.let { requireActivity().getString(it) }, title?.let { requireActivity().getString(it) }, init)
}

inline fun AnkoContext<*>.alert(
    message: CharSequence,
    title: CharSequence? = null,
    noinline init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = ctx.alert(message, title, init)

inline fun android.app.Fragment.alert(
    message: CharSequence,
    title: CharSequence? = null,
    noinline init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = activity.alert(message, title, init)

inline fun AnkoContext<*>.alert(
    message: Int,
    title: Int? = null,
    noinline init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = ctx.alert(message, title, init)

inline fun android.app.Fragment.alert(
    message: Int,
    title: Int? = null,
    noinline init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = activity.alert(message, title, init)

inline fun AnkoContext<*>.alert(noinline init: AlertBuilder<DialogInterface>.() -> Unit) = ctx.alert(init)
inline fun android.app.Fragment.alert(noinline init: AlertBuilder<DialogInterface>.() -> Unit) = activity.alert(init)

inline fun AnkoContext<*>.progressDialog(
    message: Int? = null,
    title: Int? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = ctx.progressDialog(message, title, init)

inline fun android.app.Fragment.progressDialog(
    message: Int? = null,
    title: Int? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = activity.progressDialog(message, title, init)

fun Context.progressDialog(
    message: Int? = null,
    title: Int? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = progressDialog(false, message?.let { getString(it) }, title?.let { getString(it) }, init)

inline fun AnkoContext<*>.indeterminateProgressDialog(
    message: Int? = null,
    title: Int? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = ctx.indeterminateProgressDialog(message, title, init)

inline fun android.app.Fragment.indeterminateProgressDialog(
    message: Int? = null,
    title: Int? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = activity.indeterminateProgressDialog(message, title, init)

fun Context.indeterminateProgressDialog(
    message: Int? = null,
    title: Int? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = progressDialog(true, message?.let { getString(it) }, title?.let { getString(it) }, init)

inline fun AnkoContext<*>.progressDialog(
    message: CharSequence? = null,
    title: CharSequence? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = ctx.progressDialog(message, title, init)

inline fun android.app.Fragment.progressDialog(
    message: CharSequence? = null,
    title: CharSequence? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = activity.progressDialog(message, title, init)

fun Context.progressDialog(
    message: CharSequence? = null,
    title: CharSequence? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = progressDialog(false, message, title, init)

inline fun AnkoContext<*>.indeterminateProgressDialog(
    message: CharSequence? = null,
    title: CharSequence? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = ctx.indeterminateProgressDialog(message, title, init)

inline fun android.app.Fragment.indeterminateProgressDialog(
    message: CharSequence? = null,
    title: CharSequence? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = activity.indeterminateProgressDialog(message, title, init)

fun Context.indeterminateProgressDialog(
    message: CharSequence? = null,
    title: CharSequence? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = progressDialog(true, message, title, init)

private fun Context.progressDialog(
    indeterminate: Boolean,
    message: CharSequence? = null,
    title: CharSequence? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = ProgressDialog(this).apply {
    isIndeterminate = indeterminate
    if (!indeterminate) setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
    if (message != null) setMessage(message)
    if (title != null) setTitle(title)
    if (init != null) init()
    show()
}

inline fun <K, V> arrayMapOf(): ArrayMap<K, V> = ArrayMap()

fun <K, V> arrayMapOf(vararg pairs: Pair<K, V>): ArrayMap<K, V> {
    val map = ArrayMap<K, V>(pairs.size)
    for (pair in pairs) {
        map[pair.first] = pair.second
    }
    return map
}
