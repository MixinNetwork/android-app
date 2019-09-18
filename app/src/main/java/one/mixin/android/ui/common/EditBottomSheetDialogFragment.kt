package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
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
            64
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
        contentView.biography_et.setText(str)
        contentView.title.setText(
            if (isName) {
                R.string.edit_name
            } else {
                R.string.edit_biography
            }
        )
        if (str != null) {
            contentView.biography_et.setSelection(str.length)
            contentView.biography_counter.text = "${maxLength - str.length}"
        } else {
            contentView.biography_counter.text = "$maxLength"
        }
        contentView.biography_et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (s != null) {
                    contentView.biography_counter.text = "${maxLength - s.length}"
                } else {
                    contentView.biography_counter.text = "$maxLength"
                }
            }
        })
        contentView.biography_cancel.setOnClickListener { dismiss() }
        contentView.biography_save.setOnClickListener {
            changeAction?.invoke(contentView.biography_et.text.toString())
            dismiss()
        }
        (dialog as BottomSheet).apply {
            fullScreen = true
            setCustomView(contentView)
            
            setOnShowListener {
                contentView.biography_et.requestFocus()
                contentView.biography_et.showKeyboard()
            }
        }
    }
}
