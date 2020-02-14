package one.mixin.android.widget.gallery.internal.ui.widget

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import one.mixin.android.extension.alertDialogBuilder

class IncapableDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments!!.getString(EXTRA_TITLE)
        val message = arguments!!.getString(EXTRA_MESSAGE)
        val builder = alertDialogBuilder()
        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title)
        }
        if (!TextUtils.isEmpty(message)) {
            builder.setMessage(message)
        }
        builder.setPositiveButton(android.R.string.ok) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        return builder.create()
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        @JvmStatic
        fun newInstance(title: String?, message: String?): IncapableDialog {
            val dialog = IncapableDialog()
            val args = Bundle()
            args.putString(EXTRA_TITLE, title)
            args.putString(EXTRA_MESSAGE, message)
            dialog.arguments = args
            return dialog
        }
    }
}
