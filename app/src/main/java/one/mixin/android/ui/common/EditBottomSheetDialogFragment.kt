package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import kotlinx.android.synthetic.main.fragment_bottom_edit.view.*
import one.mixin.android.R
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.withArgs
import one.mixin.android.util.Session
import one.mixin.android.widget.BottomSheet

@SuppressLint("InflateParams")
class EditBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "EditBottomSheetDialogFragment"

        const val IS_NAME = "is_name"

        fun newInstance(isName: Boolean) = EditBottomSheetDialogFragment().withArgs {
            putBoolean(IS_NAME, isName)
        }
    }

    private val isName by lazy { arguments!!.getBoolean(IS_NAME) }
    private val maxLength by lazy {
        if (isName) {
            50
        } else {
            140
        }
    }

    var changeAction: ((String) -> Unit)? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_bottom_edit, null)
        val str = if (isName) {
            Session.getAccount()?.full_name
        } else {
            Session.getAccount()?.biography
        }
        contentView.edit_et.setText(str)
        contentView.edit_title.setText(
            if (isName) {
                R.string.edit_name
            } else {
                R.string.edit_biography
            }
        )
        contentView.edit_et.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        if (str != null) {
            contentView.edit_et.setSelection(str.length)
            contentView.edit_counter.text = "${maxLength - str.length}"
        } else {
            contentView.edit_counter.text = "$maxLength"
        }
        contentView.edit_et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                contentView.edit_save.isEnabled = !(isName && s.isNullOrEmpty())
                if (s != null) {
                    contentView.edit_counter.text = "${maxLength - s.length}"
                } else {
                    contentView.edit_counter.text = "$maxLength"
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        })
        contentView.edit_cancel.setOnClickListener { dismiss() }
        contentView.edit_save.setOnClickListener {
            changeAction?.invoke(contentView.edit_et.text.toString())
            dismiss()
        }
        (dialog as BottomSheet).apply {
            fullScreen = true
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
