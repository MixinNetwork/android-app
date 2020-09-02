package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.fragment_bottom_edit.view.*
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.showKeyboard
import one.mixin.android.widget.BottomSheet

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

        const val MAX_LINE = 6L

        fun newInstance() = EditDialog()
    }

    var titleText: String? = null
    var editHint: String? = null
    var editText: String? = null

    var editInputType: Int? = null
    @IntRange(from = 1, to = MAX_LINE)
    var editMaxLines: Int = 1
    var maxTextCount: Int = -1
    var allowEmpty: Boolean = false
    var defaultEditEnable: Boolean = true

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
        val maxLines = if (editMaxLines > MAX_LINE) {
            MAX_LINE.toInt()
        } else editMaxLines
        if (maxLines == 1) {
            contentView.edit_et.isSingleLine = true
        }
        contentView.edit_save.isEnabled = defaultEditEnable
        contentView.edit_et.maxLines = maxLines
        if (maxTextCount != -1) {
            contentView.input_layout.isCounterEnabled = true
            contentView.input_layout.counterMaxLength = maxTextCount
        }
        if (!editText.isNullOrEmpty()) {
            contentView.edit_et.setSelection(editText!!.length)
        }
        contentView.edit_et.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    contentView.edit_save.isEnabled = when {
                        s.isNullOrEmpty() -> allowEmpty
                        maxTextCount == -1 -> true
                        else -> s.length <= maxTextCount
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            }
        )
        contentView.edit_cancel.setText(leftText)
        contentView.edit_cancel.setOnClickListener {
            leftAction?.invoke()
            contentView.edit_et.hideKeyboard()
            dismiss()
        }
        contentView.edit_save.setText(rightText)
        contentView.edit_save.setOnClickListener {
            rightAction?.invoke(contentView.edit_et.text.toString())
            contentView.edit_et.hideKeyboard()
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
