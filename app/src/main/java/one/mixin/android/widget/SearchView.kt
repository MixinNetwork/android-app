package one.mixin.android.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.widget.AppCompatEditText
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.hideKeyboard

class SearchView(context: Context, attrs: AttributeSet?) : AppCompatEditText(context, attrs) {

    private val iconClear: Drawable by lazy {
        val size = context.dpToPx(16f)
        context.getDrawable(R.drawable.ic_asset_add_search_clear).apply {
            this?.setBounds(0, 0, size, size)
        }!!
    }
    private val iconSearch: Drawable by lazy {
        val size = context.dpToPx(16f)
        context.getDrawable(R.drawable.ic_asset_add_search).apply {
            this?.setBounds(0, 0, size, size)
        }!!
    }

    private val watcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            checkDrawables()
            listener?.afterTextChanged(s)
        }
    }

    private val onEditorActionListener = OnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            listener?.onSearch()
            hideKeyboard()
            return@OnEditorActionListener true
        }
        false
    }

    private val onTouchListener = object : OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                val rightDrawable = compoundDrawables[2]
                if (rightDrawable != null &&
                    event.rawX >= right - (rightDrawable.bounds.width() + context.dpToPx(16f))
                ) {
                    text?.clear()
                    return true
                } else {
                    performClick()
                }
            }
            return false
        }
    }

    private val onFocusChangeListener = object : OnFocusChangeListener {
        override fun onFocusChange(v: View, hasFocus: Boolean) {
            checkDrawables()
        }
    }

    init {
        hint = resources.getString(R.string.search)
        addTextChangedListener(watcher)
        setOnEditorActionListener(onEditorActionListener)
        setOnTouchListener(onTouchListener)
        setOnFocusChangeListener(onFocusChangeListener)

        remainFocusable()
    }

    // remove focus but remain focusable
    fun remainFocusable() {
        post {
            isFocusableInTouchMode = false
            isFocusable = false
            isFocusableInTouchMode = true
            isFocusable = true
        }
    }

    private fun checkDrawables() {
        val hasFocus = hasFocus()
        val hasText = !text.isNullOrBlank()
        if (hasFocus && hasText) {
            setCompoundDrawables(null, null, iconClear, null)
        } else if (hasFocus) {
            setCompoundDrawables(null, null, null, null)
        } else if (hasText) {
            setCompoundDrawables(null, null, iconClear, null)
        } else {
            setCompoundDrawables(iconSearch, null, null, null)
        }
    }

    var listener: OnSearchViewListener? = null

    interface OnSearchViewListener {
        fun afterTextChanged(s: Editable?)
        fun onSearch()
    }
}
