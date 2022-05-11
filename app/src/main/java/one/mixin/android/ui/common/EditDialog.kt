package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import one.mixin.android.R
import one.mixin.android.databinding.FragmentBottomEditBinding
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.showKeyboard
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

inline fun FragmentActivity.editDialog(
    builder: EditDialog.() -> Unit
): EditDialog {
    val dialog = EditDialog.newInstance()
    dialog.apply(builder)
    dialog.showNow(supportFragmentManager, EditDialog.TAG)
    dialog.dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    return dialog
}

inline fun Fragment.editDialog(
    builder: EditDialog.() -> Unit
): EditDialog {
    val dialog = EditDialog.newInstance()
    dialog.apply(builder)
    dialog.showNow(parentFragmentManager, EditDialog.TAG)
    dialog.dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
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

    @StringRes var leftText: Int = R.string.Cancel
    var leftAction: (() -> Unit)? = null
    @StringRes var rightText: Int = R.string.Save
    var rightAction: ((editContent: String) -> Unit)? = null

    private val binding by viewBinding(FragmentBottomEditBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.editEt.setText(editText)
        binding.editEt.hint = editHint
        binding.editTitle.text = titleText
        editInputType?.let {
            binding.editEt.inputType = it
        }
        val maxLines = if (editMaxLines > MAX_LINE) {
            MAX_LINE.toInt()
        } else editMaxLines
        if (maxLines == 1) {
            binding.editEt.isSingleLine = true
        }
        binding.editSave.isEnabled = defaultEditEnable
        binding.editEt.maxLines = maxLines
        if (maxTextCount != -1) {
            binding.inputLayout.isCounterEnabled = true
            binding.inputLayout.counterMaxLength = maxTextCount
        }
        if (!editText.isNullOrEmpty()) {
            binding.editEt.setSelection(editText!!.length)
        }
        binding.editEt.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    binding.editSave.isEnabled = when {
                        s.isNullOrBlank() -> allowEmpty
                        maxTextCount == -1 -> true
                        else -> s.length <= maxTextCount
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            }
        )
        binding.editCancel.setText(leftText)
        binding.editCancel.setOnClickListener {
            leftAction?.invoke()
            binding.editEt.hideKeyboard()
            dismiss()
        }
        binding.editSave.setText(rightText)
        binding.editSave.setOnClickListener {
            rightAction?.invoke(binding.editEt.text.toString())
            binding.editEt.hideKeyboard()
            dismiss()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)

            setOnShowListener {
                contentView.post {
                    binding.editEt.showKeyboard()
                }
            }
        }
    }
}
