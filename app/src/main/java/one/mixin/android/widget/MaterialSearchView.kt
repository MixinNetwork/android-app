package one.mixin.android.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.view_search.view.*
import one.mixin.android.R
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.translationX
import one.mixin.android.extension.translationY
import one.mixin.android.ui.search.SearchFragment.Companion.SEARCH_DEBOUNCE
import org.jetbrains.annotations.NotNull
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MaterialSearchView : FrameLayout {
    var isOpen = false
        private set
    private var mClearingFocus: Boolean = false

    private var mCurrentQuery: CharSequence? = null
    var mOnQueryTextListener: OnQueryTextListener? = null
    private var mSearchViewListener: SearchViewListener? = null

    private var disposable: Disposable? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_search, this, true)
        initStyle(attrs, defStyleAttr)
        initSearchView()
    }

    private val containerHeight by lazy {
        context.screenHeight() * 0.7f
    }

    @Suppress("unused")
    val currentQuery: String
        get() = if (!TextUtils.isEmpty(mCurrentQuery)) {
            mCurrentQuery.toString()
        } else ""

    private fun initStyle(attributeSet: AttributeSet?, defStyleAttribute: Int) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        val typedArray = context.obtainStyledAttributes(
            attributeSet,
            R.styleable.MaterialSearchView, defStyleAttribute, 0
        )
        if (typedArray.hasValue(R.styleable.MaterialSearchView_android_hint)) {
            setHint(typedArray.getString(R.styleable.MaterialSearchView_android_hint))
        }
        if (typedArray.hasValue(R.styleable.MaterialSearchView_searchCloseIcon)) {
            setCancelIcon(
                typedArray.getResourceId(
                    R.styleable.MaterialSearchView_searchCloseIcon,
                    R.drawable.ic_action_navigation_close
                )
            )
        }
        if (typedArray.hasValue(R.styleable.MaterialSearchView_searchBackIcon)) {
            setBackIcon(
                typedArray.getResourceId(
                    R.styleable.MaterialSearchView_searchBackIcon,
                    R.drawable.ic_wallet
                )
            )
        }
        if (typedArray.hasValue(R.styleable.MaterialSearchView_android_inputType)) {
            setInputType(
                typedArray.getInteger(
                    R.styleable.MaterialSearchView_android_inputType,
                    InputType.TYPE_CLASS_TEXT
                )
            )
        }
        if (typedArray.hasValue(R.styleable.MaterialSearchView_searchBarHeight)) {
            setSearchBarHeight(
                typedArray.getDimensionPixelSize(
                    R.styleable.MaterialSearchView_searchBarHeight, context.appCompatActionBarHeight()
                )
            )
        } else {
            setSearchBarHeight(context.appCompatActionBarHeight())
        }
        @Suppress("DEPRECATION")
        ViewCompat.setFitsSystemWindows(
            this,
            typedArray.getBoolean(R.styleable.MaterialSearchView_android_fitsSystemWindows, false)
        )
        typedArray.recycle()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val parcelable = super.onSaveInstanceState() ?: return null
        return SavedState(parcelable).apply {
            this.isOpen = this@MaterialSearchView.isOpen
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        isOpen = (state as? SavedState)?.isOpen ?: false
        if (isOpen) openSearch() else closeSearch()
        super.onRestoreInstanceState(state)
    }

    private fun initSearchView() {
        container_circle.translationY = -containerHeight
        (container_circle.layoutParams as ConstraintLayout.LayoutParams).matchConstraintMaxHeight = containerHeight.toInt()
        container_shadow.layoutParams.height = context.screenHeight()
        container_shadow.setOnClickListener {
            hideContainer()
        }
        search_et.setOnEditorActionListener { _, _, _ ->
            onSubmitQuery()
            true
        }

        disposable = search_et.textChanges().debounce(SEARCH_DEBOUNCE, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    this@MaterialSearchView.onTextChanged(it)
                },
                {}
            )

        right_clear.setOnClickListener {
            if (!search_et.text.isNullOrEmpty()) {
                search_et.setText("")
            }
        }
        logo_layout.setOnClickListener {
            if (containerDisplay) {
                hideContainer()
            } else {
                showContainer()
            }
        }
    }

    fun hideContainer() {
        containerDisplay = false
        container_shadow.fadeOut()
        action_va.fadeOut()
        group_ib.fadeIn()
        search_ib.fadeIn()
        container_circle.translationY(-containerHeight) {
            container_circle.isVisible = false
        }
        hideAction?.invoke()
    }

    var hideAction: (() -> Unit)? = null

    fun showContainer() {
        containerDisplay = true
        container_circle.isVisible = true
        container_shadow.fadeIn()
        action_va.fadeIn()
        group_ib.fadeOut()
        search_ib.fadeOut()
        container_circle.translationY(0f) {
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposable?.dispose()
    }

    private var oldLeftX = 0f
    private var oldSearchWidth = 0

    fun dragSearch(progress: Float) {
        group_ib.translationX = context.dpToPx(88f) * progress
        search_ib.translationX = context.dpToPx(88f) * progress
        val fastFadeOut = (1 - 2 * progress).coerceAtLeast(0f)
        val fastFadeIn = (progress.coerceAtLeast(.5f) - .5f) * 2
        search_et.isVisible = true
        search_et.alpha = fastFadeIn
        search_ib.isVisible = true
        logo_layout.isVisible = true
        back_ib.isVisible = true
        logo_layout.alpha = fastFadeOut
        back_ib.alpha = fastFadeIn
    }

    fun openSearch() {
        logo_layout.animate().apply {
            setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    op()
                }

                override fun onAnimationCancel(animation: Animator?) {
                    op()
                }

                private fun op() {
                    setListener(null)
                    search_et.isVisible = true
                    search_et.showKeyboard()
                    search_et.animate().apply {
                        setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationCancel(animation: Animator?) {
                                search_et.alpha = 1f
                            }
                        })
                    }.setDuration(150L).alpha(1f).start()
                    back_ib.isVisible = true
                    back_ib.animate().apply {
                        setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationCancel(animation: Animator?) {
                                back_ib.alpha = 1f
                            }
                        })
                    }.setDuration(150L).alpha(1f).start()
                }
            })
        }.alpha(0f).setDuration(150L).start()

        right_clear.visibility = View.GONE

        search_et.setText("")
        oldLeftX = logo_layout.x
        oldSearchWidth = search_et.measuredWidth
        group_ib.translationX(context.dpToPx(88f).toFloat())
        search_ib.translationX(context.dpToPx(88f).toFloat())
        mSearchViewListener?.onSearchViewOpened()
        isOpen = true
    }

    fun closeSearch() {
        search_et.animate().apply {
            setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    op()
                }

                override fun onAnimationCancel(animation: Animator?) {
                    op()
                }

                private fun op() {
                    setListener(null)
                    search_et.isGone = true
                }
            })
        }.alpha(0f).setDuration(150L).start()
        back_ib.animate().apply {
            setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    op()
                }

                override fun onAnimationCancel(animation: Animator?) {
                    op()
                }

                private fun op() {
                    setListener(null)
                    back_ib.isGone = true
                    logo_layout.isVisible = true
                    logo_layout.animate().apply {
                        setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationCancel(animation: Animator?) {
                                logo_layout.alpha = 1f
                            }
                        })
                    }.setDuration(150L).alpha(1f).start()
                }
            })
        }.setDuration(150L).alpha(0f).start()
        right_clear.visibility = View.GONE

        group_ib.translationX(0f)
        search_ib.translationX(0f)
        clearFocus()
        search_et.hideKeyboard()
        search_et.setText("")
        mSearchViewListener?.onSearchViewClosed()
        isOpen = false
    }

    private fun onTextChanged(newText: CharSequence) {
        mCurrentQuery = newText
        right_clear.isVisible = newText.isNotEmpty()
        mOnQueryTextListener?.onQueryTextChange(newText.toString())
    }

    private fun onSubmitQuery() {
        val query = search_et.text

        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryTextListener == null) {
                closeSearch()
                search_et.setText("")
            }
        }
    }

    fun setSearchViewListener(mSearchViewListener: SearchViewListener) {
        this.mSearchViewListener = mSearchViewListener
    }

    fun setQuery(query: CharSequence?, submit: Boolean) {
        search_et.setText(query)

        if (query != null) {
            search_et.setSelection(search_et.length())
            mCurrentQuery = query
        }

        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery()
        }
    }

    fun setSearchBarColor(color: Int) {
        search_et.setBackgroundColor(color)
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        if (factor < 0) return color

        val alpha = (Color.alpha(color) * factor).roundToInt()

        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun setHint(hint: CharSequence?) {
        search_et.hint = hint
    }

    fun setCancelIcon(resourceId: Int) {
        back_ib.setImageResource(resourceId)
    }

    fun setBackIcon(resourceId: Int) {
        search_ib.setImageResource(resourceId)
    }

    fun setInputType(inputType: Int) {
        search_et.inputType = inputType
    }

    fun setSearchBarHeight(height: Int) {
        search_view.minimumHeight = height
        search_view.layoutParams.height = height
    }

    fun setOnGroupClickListener(onClickListener: OnClickListener) {
        group_ib.setOnClickListener(onClickListener)
    }

    fun setOnAddClickListener(onClickListener: OnClickListener) {
        add_ib.setOnClickListener(onClickListener)
    }

    fun setOnConfirmClickListener(onClickListener: OnClickListener) {
        confirm_ib.setOnClickListener(onClickListener)
    }

    fun setOnLeftClickListener(onClickListener: OnClickListener) {
        search_ib.setOnClickListener(onClickListener)
    }

    var containerDisplay = false

    fun setOnBackClickListener(onClickListener: OnClickListener) {
        back_ib.setOnClickListener(onClickListener)
    }

    override fun clearFocus() {
        this.mClearingFocus = true
        hideKeyboard()
        super.clearFocus()
        search_et.clearFocus()
        this.mClearingFocus = false
    }

    interface OnQueryTextListener {
        fun onQueryTextChange(newText: String): Boolean
    }

    interface SearchViewListener {
        fun onSearchViewOpened()

        fun onSearchViewClosed()
    }
}

internal class SavedState : View.BaseSavedState {
    var isOpen: Boolean = false

    constructor(source: Parcel) : super(source) {
        isOpen = source.readInt() == 1
    }

    constructor(superState: Parcelable) : super(superState)

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeInt(if (isOpen) 1 else 0)
    }

    companion object {
        @JvmField
        @NotNull
        val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
            override fun createFromParcel(`in`: Parcel): SavedState {
                return SavedState(`in`)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}
