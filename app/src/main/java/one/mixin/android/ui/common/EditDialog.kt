package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.fragment_bottom_edit.view.*
import one.mixin.android.R
import one.mixin.android.extension.showKeyboard
import one.mixin.android.widget.BottomSheet

@Suppress("unused")
inline fun FragmentActivity.editDialog(
    builder: EditDialog.() -> Unit
): EditDialog {
    val dialog = EditDialog.newInstance()
    dialog.apply(builder)
    dialog.showNow(supportFragmentManager, EditDialog.TAG)
    return dialog
}

inline fun Fragment.editDialog(
    builder: EditDialog.() -> Unit
): EditDialog {
    val dialog = EditDialog.newInstance()
    dialog.apply(builder)
    dialog.showNow(parentFragmentManager, EditDialog.TAG)
    return dialog
}

class EditDialog : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "EditDialog"

        fun newInstance() = EditDialog()
    }

    var titleText: String? = null
    var editHint: String? = null
    var editText: String? = null

    var editInputType: Int? = null
    var maxTextCount: Int = -1
    var allowEmpty: Boolean = false

    @StringRes var leftText: Int = R.string.cancel
    var leftAction: (() -> Unit)? = null
    @StringRes var rightText: Int = R.string.save
    var rightAction: ((editContent: String) -> Unit)? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_bottom_edit, null)
        contentView.edit_et.setText(editText)
        contentView.edit_et.hint = editHint
        contentView.edit_title.text = titleText
        editInputType?.let {
            contentView.edit_et.inputType = it
        }
        if (maxTextCount != -1) {
            contentView.edit_et.filters = arrayOf(InputFilter.LengthFilter(maxTextCount))
        }
        contentView.edit_counter.isVisible = maxTextCount != -1
        if (editText != null) {
            contentView.edit_et.setSelection(editText!!.length)
            contentView.edit_counter.text = "${maxTextCount - editText!!.length}"
        } else {
            contentView.edit_counter.text = "$maxTextCount"
        }
        contentView.edit_et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                contentView.edit_save.isEnabled = !(!allowEmpty && s.isNullOrEmpty())
                if (maxTextCount != -1) {
                    if (s != null) {
                        contentView.edit_counter.text = "${maxTextCount - s.length}"
                    } else {
                        contentView.edit_counter.text = "$maxTextCount"
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        })
        contentView.edit_cancel.setText(leftText)
        contentView.edit_cancel.setOnClickListener {
            leftAction?.invoke()
            dismiss()
        }
        contentView.edit_save.setText(rightText)
        contentView.edit_save.setOnClickListener {
            rightAction?.invoke(contentView.edit_et.text.toString())
            dismiss()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)

            setOnShowListener {
                contentView.post {
                    contentView.edit_et.requestFocus()
                    contentView.edit_et.showKeyboard()
                }
            }
        }
    }
}
