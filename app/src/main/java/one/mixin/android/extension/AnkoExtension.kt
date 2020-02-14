package one.mixin.android.extension

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import androidx.annotation.ArrayRes
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import one.mixin.android.R

fun Fragment.toast(textResource: Int) = requireActivity().toast(textResource)

fun Fragment.toast(text: CharSequence) = requireActivity().toast(text)

fun Fragment.singleChoice(
    title: CharSequence? = null,
    @ArrayRes itemsId: Int,
    checkedItem: Int,
    onClick: (DialogInterface, Int) -> Unit
): Unit = requireActivity().singleChoice(title, itemsId, checkedItem, onClick)

fun Context.singleChoice(
    title: CharSequence? = null,
    @ArrayRes itemsId: Int,
    checkedItem: Int,
    onClick: (DialogInterface, Int) -> Unit
) {
    MaterialAlertDialogBuilder(this, getAlertDialogTheme()).apply {
        setTitle(title)
        setSingleChoiceItems(itemsId, checkedItem, onClick)
        setPositiveButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
    }.create().show()
}

fun Fragment.alert(
    message: String,
    title: String? = null
) = requireActivity().alert(message, title)

fun Context.alert(
    message: CharSequence,
    title: CharSequence? = null
): MaterialAlertDialogBuilder {
    return MaterialAlertDialogBuilder(this, getAlertDialogTheme()).apply {
        if (title != null) {
            setTitle(title)
        }
        setMessage(message)
    }
}

fun Context.getAlertDialogTheme(): Int {
    return if (booleanFromAttribute(R.attr.flag_night)) {
        R.style.MixinAlertDialogNightTheme
    } else {
        R.style.MixinAlertDialogTheme
    }
}

fun Fragment.indeterminateProgressDialog(message: String? = null, title: String? = null, init: (ProgressDialog.() -> Unit)? = null): ProgressDialog {
    return requireActivity().indeterminateProgressDialog(message, title, init)
}

fun Fragment.indeterminateProgressDialog(message: Int? = null, title: Int? = null, init: (ProgressDialog.() -> Unit)? = null): ProgressDialog {
    return requireActivity().indeterminateProgressDialog(message?.let { requireActivity().getString(it) }, title?.let { requireActivity().getString(it) }, init)
}

@Deprecated(message = "Android progress dialogs are deprecated")
fun Context.indeterminateProgressDialog(
    message: CharSequence? = null,
    title: CharSequence? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = progressDialog(true, message, title, init)

@Deprecated(message = "Android progress dialogs are deprecated")
private fun Context.progressDialog(
    indeterminate: Boolean,
    message: CharSequence? = null,
    title: CharSequence? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = ProgressDialog(this, getAlertDialogTheme()).apply {
    isIndeterminate = indeterminate
    if (!indeterminate) setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
    if (message != null) setMessage(message)
    if (title != null) setTitle(title)
    if (init != null) init()
    show()
}
