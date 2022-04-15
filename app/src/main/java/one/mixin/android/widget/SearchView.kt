package one.mixin.android.widget

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.TextView.OnEditorActionListener
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import one.mixin.android.R
import one.mixin.android.databinding.ViewSearchBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.hideKeyboard

class SearchView : FrameLayout {
    private var binding: ViewSearchBinding =
        ViewSearchBinding.inflate(LayoutInflater.from(context), this)
    val et get() = binding.searchEt

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.SearchView,
            defStyleAttr,
            0
        )
        val circleClearIcon = typedArray.getBoolean(R.styleable.SearchView_circle_clear_icon, false)
        val size = if (circleClearIcon) small else medium
        val clearIcon = if (circleClearIcon) R.drawable.ic_asset_add_search_clear else R.drawable.ic_close_black
        binding.rightClear.setImageResource(clearIcon)
        binding.rightClear.updateLayoutParams<LayoutParams> {
            width = size
            height = size
        }
        typedArray.recycle()

        binding.searchEt.apply {
            hint = resources.getString(R.string.Search)
            addTextChangedListener(watcher)
            setOnEditorActionListener(onEditorActionListener)
        }
        binding.rightClear.setOnClickListener {
            if (!binding.searchEt.text.isNullOrEmpty()) {
                binding.searchEt.setText("")
            }
        }

        remainFocusable()
    }

    private val medium = 24.dp
    private val small = 16.dp

    private val watcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            binding.rightClear.isVisible = s?.isNotEmpty() == true
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

    // remove focus but remain focusable
    fun remainFocusable() {
        post {
            binding.searchEt.apply {
                isFocusableInTouchMode = false
                isFocusable = false
                isFocusableInTouchMode = true
                isFocusable = true
            }
        }
    }

    fun setHint(hintText: String) {
        binding.searchEt.hint = hintText
    }

    var listener: OnSearchViewListener? = null

    interface OnSearchViewListener {
        fun afterTextChanged(s: Editable?)
        fun onSearch()
    }
}
