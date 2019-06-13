package one.mixin.android.extension

import android.content.Context
import android.content.DialogInterface
import androidx.fragment.app.Fragment
import one.mixin.android.widget.ProgressDialog
import org.jetbrains.anko.AlertBuilder
import org.jetbrains.anko.alert
import org.jetbrains.anko.selector

fun Fragment.toast(textResource: Int) = requireActivity().toast(textResource)

fun Fragment.toast(text: CharSequence) = requireActivity().toast(text)

fun Fragment.selector(
    title: CharSequence? = null,
    items: List<CharSequence>,
    onClick: (DialogInterface, Int) -> Unit
): Unit = requireActivity().selector(title, items, onClick)

fun Fragment.alert(
    message: String,
    title: String? = null,
    init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = requireActivity().alert(message, title, init)

fun Fragment.indeterminateProgressDialog(message: String? = null, title: String? = null, init: (ProgressDialog.() -> Unit)? = null): ProgressDialog {
    return requireActivity().progressDialog(message, title, init)
}

fun Fragment.indeterminateProgressDialog(message: Int? = null, title: Int? = null, init: (ProgressDialog.() -> Unit)? = null): ProgressDialog {
    return requireActivity().progressDialog(message?.let { requireActivity().getString(it) }, title?.let { requireActivity().getString(it) }, init)
}

fun Context.progressDialog(
    message: CharSequence? = null,
    title: CharSequence? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = ProgressDialog(this).apply {
    if (message != null) setMessage(message)
    if (title != null) setTitle(title)
    if (init != null) init()
}
