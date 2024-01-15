package one.mixin.android.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.R
import one.mixin.android.databinding.ViewMaterialSearchBinding
import one.mixin.android.extension.ANIMATION_DURATION_SHORT
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.translationX
import one.mixin.android.extension.withAlpha
import one.mixin.android.session.Session
import one.mixin.android.ui.search.SearchFragment.Companion.SEARCH_DEBOUNCE
import one.mixin.android.vo.toUser
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
    private var binding: ViewMaterialSearchBinding =
        ViewMaterialSearchBinding.inflate(LayoutInflater.from(context), this, true)
    val actionVa get() = binding.actionVa
    val logo get() = binding.logo
    val dot get() = binding.dot
    val desktop get() = binding.desktopIb

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        initStyle(attrs, defStyleAttr)
        initSearchView()
    }

    private val containerHeight by lazy {
        context.screenHeight() * 0.7f
    }

    @Suppress("unused")
    val currentQuery: String
        get() =
            if (!TextUtils.isEmpty(mCurrentQuery)) {
                mCurrentQuery.toString()
            } else {
                ""
            }

    private fun initStyle(
        attributeSet: AttributeSet?,
        defStyleAttribute: Int,
    ) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        val typedArray =
            context.obtainStyledAttributes(
                attributeSet,
                R.styleable.MaterialSearchView,
                defStyleAttribute,
                0,
            )
        if (typedArray.hasValue(R.styleable.MaterialSearchView_android_hint)) {
            setHint(typedArray.getString(R.styleable.MaterialSearchView_android_hint))
        }
        if (typedArray.hasValue(R.styleable.MaterialSearchView_searchCloseIcon)) {
            setCancelIcon(
                typedArray.getResourceId(
                    R.styleable.MaterialSearchView_searchCloseIcon,
                    R.drawable.ic_action_navigation_close,
                ),
            )
        }
        if (typedArray.hasValue(R.styleable.MaterialSearchView_searchBackIcon)) {
            setBackIcon(
                typedArray.getResourceId(
                    R.styleable.MaterialSearchView_searchBackIcon,
                    R.drawable.ic_wallet,
                ),
            )
        }
        if (typedArray.hasValue(R.styleable.MaterialSearchView_android_inputType)) {
            setInputType(
                typedArray.getInteger(
                    R.styleable.MaterialSearchView_android_inputType,
                    InputType.TYPE_CLASS_TEXT,
                ),
            )
        }
        if (typedArray.hasValue(R.styleable.MaterialSearchView_searchBarHeight)) {
            setSearchBarHeight(
                typedArray.getDimensionPixelSize(
                    R.styleable.MaterialSearchView_searchBarHeight,
                    context.appCompatActionBarHeight(),
                ),
            )
        } else {
            setSearchBarHeight(context.appCompatActionBarHeight())
        }
        @Suppress("DEPRECATION")
        ViewCompat.setFitsSystemWindows(
            this,
            typedArray.getBoolean(R.styleable.MaterialSearchView_android_fitsSystemWindows, false),
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
        if (isOpen) openNoAnimate() else closeNoAnimate()
        super.onRestoreInstanceState(state)
    }

    @SuppressLint("CheckResult")
    private fun initSearchView() {
        (binding.containerCircle.layoutParams as ConstraintLayout.LayoutParams).matchConstraintMaxHeight =
            containerHeight.toInt()
        binding.containerShadow.layoutParams.height = context.screenHeight()
        binding.containerShadow.setOnClickListener {
            hideContainer()
        }
        binding.searchEt.setOnEditorActionListener { _, _, _ ->
            onSubmitQuery()
            true
        }
        Session.getAccount()?.toUser()?.let { u ->
            binding.avatar.setInfo(u.fullName, u.avatarUrl, u.userId)
            binding.avatar.setTextSize(14f)
        }

        // Don't auto dispose
        disposable =
            binding.searchEt.textChanges().debounce(SEARCH_DEBOUNCE, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        this@MaterialSearchView.onTextChanged(it)
                    },
                    {},
                )

        binding.rightClear.setOnClickListener {
            if (!binding.searchEt.text.isNullOrEmpty()) {
                binding.searchEt.setText("")
            }
        }
        binding.logoLayout.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .throttleFirst(500, TimeUnit.MILLISECONDS)
            .subscribe {
                if (containerDisplay) {
                    hideContainer()
                } else {
                    showContainer()
                }
            }
    }

    fun hideContainer() {
        containerDisplay = false
        binding.searchIb.fadeIn()
        if (isDesktopLogin) {
            binding.desktopIb.fadeIn()
        }
        binding.avatar.fadeIn()
        binding.actionVa.fadeOut()
        ValueAnimator.ofFloat(1f, 0f).apply {
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.containerShadow.isVisible = false
                    }
                },
            )
            addUpdateListener {
                val c = Color.BLACK.withAlpha(0.32f * it.animatedValue as Float)
                binding.containerShadow.setBackgroundColor(c)
            }
            interpolator = AccelerateInterpolator()
            duration = ANIMATION_DURATION_SHORT
        }.start()
        binding.containerShadow.collapse()
        hideAction?.invoke()
    }

    var hideAction: (() -> Unit)? = null
    var showAction: (() -> Unit)? = null

    fun showContainer() {
        containerDisplay = true
        binding.searchIb.fadeOut()
        if (isDesktopLogin) {
            binding.desktopIb.fadeOut()
        }
        binding.avatar.fadeOut()
        binding.actionVa.fadeIn()
        binding.containerCircle.isVisible = true
        showAction?.invoke()
        ValueAnimator.ofFloat(0f, 1f).apply {
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        binding.containerShadow.isVisible = true
                        binding.containerShadow.setBackgroundColor(Color.TRANSPARENT)
                    }
                },
            )
            addUpdateListener {
                val c = Color.BLACK.withAlpha(0.32f * it.animatedValue as Float)
                binding.containerShadow.setBackgroundColor(c)
            }
            interpolator = DecelerateInterpolator()
            duration = ANIMATION_DURATION_SHORT
        }.start()
        binding.containerShadow.expand()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposable?.dispose()
    }

    private var oldLeftX = 0f
    private var oldSearchWidth = 0

    private val rightTranslationX = 132f

    private var isDesktopLogin = false

    fun dragSearch(progress: Float) {
        binding.avatar.translationX = context.dpToPx(rightTranslationX) * progress
        binding.searchIb.translationX = context.dpToPx(rightTranslationX) * progress
        if (isDesktopLogin) {
            binding.desktopIb.translationX = context.dpToPx(rightTranslationX) * progress
        }
        val fastFadeOut = (1 - 2 * progress).coerceAtLeast(0f)
        val fastFadeIn = (progress.coerceAtLeast(.5f) - .5f) * 2
        binding.searchEt.isVisible = true
        binding.searchEt.alpha = fastFadeIn
        binding.searchIb.isVisible = true
        if (isDesktopLogin) {
            binding.desktopIb.isVisible = true
        }
        binding.logoLayout.isVisible = true
        binding.backIb.isVisible = true
        binding.logoLayout.alpha = fastFadeOut
        binding.backIb.alpha = fastFadeIn
    }

    fun openSearch() {
        binding.logoLayout.animate().apply {
            setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        op()
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        op()
                    }

                    private fun op() {
                        setListener(null)
                        binding.logoLayout.isVisible = false
                        binding.searchEt.isVisible = true
                        binding.searchEt.showKeyboard()
                        binding.searchEt.animate().apply {
                            setListener(
                                object : AnimatorListenerAdapter() {
                                    override fun onAnimationCancel(animation: Animator) {
                                        binding.searchEt.alpha = 1f
                                    }
                                },
                            )
                        }.setDuration(150L).alpha(1f).start()
                        binding.backIb.isVisible = true
                        binding.backIb.animate().apply {
                            setListener(
                                object : AnimatorListenerAdapter() {
                                    override fun onAnimationCancel(animation: Animator) {
                                        binding.backIb.alpha = 1f
                                    }
                                },
                            )
                        }.setDuration(150L).alpha(1f).start()
                    }
                },
            )
        }.alpha(0f).setDuration(150L).start()

        binding.rightClear.visibility = View.GONE

        binding.searchEt.setText("")
        oldLeftX = binding.logoLayout.x
        oldSearchWidth = binding.searchEt.measuredWidth
        binding.avatar.translationX(context.dpToPx(rightTranslationX).toFloat())
        binding.searchIb.translationX(context.dpToPx(rightTranslationX).toFloat())
        if (isDesktopLogin) {
            binding.desktopIb.translationX(context.dpToPx(rightTranslationX).toFloat())
        }
        mSearchViewListener?.onSearchViewOpened()
        isOpen = true
    }

    private fun openNoAnimate() {
        binding.logoLayout.isVisible = false
        binding.searchEt.isVisible = true
        binding.searchEt.showKeyboard()
        binding.backIb.isVisible = true

        binding.rightClear.visibility = View.GONE

        binding.searchEt.setText("")
        oldLeftX = binding.logoLayout.x
        oldSearchWidth = binding.searchEt.measuredWidth
        binding.avatar.translationX(context.dpToPx(rightTranslationX).toFloat())
        binding.searchIb.translationX(context.dpToPx(rightTranslationX).toFloat())
        if (isDesktopLogin) {
            binding.desktopIb.translationX(context.dpToPx(rightTranslationX).toFloat())
        }
        mSearchViewListener?.onSearchViewOpened()
        isOpen = true
    }

    fun closeSearch() {
        binding.searchEt.animate().apply {
            setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        op()
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        op()
                    }

                    private fun op() {
                        setListener(null)
                        binding.searchEt.isGone = true
                    }
                },
            )
        }.alpha(0f).setDuration(150L).start()
        binding.backIb.animate().apply {
            setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        op()
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        op()
                    }

                    private fun op() {
                        setListener(null)
                        binding.backIb.isGone = true
                        binding.logoLayout.alpha = 0f
                        binding.logoLayout.isVisible = true
                        binding.logoLayout.animate().apply {
                            setListener(
                                object : AnimatorListenerAdapter() {
                                    override fun onAnimationCancel(animation: Animator) {
                                        binding.logoLayout.alpha = 1f
                                    }
                                },
                            )
                        }.setDuration(150L).alpha(1f).start()
                    }
                },
            )
        }.setDuration(150L).alpha(0f).start()
        closeNoAnimate()
    }

    private fun closeNoAnimate() {
        binding.rightClear.visibility = View.GONE
        hideLoading()

        binding.avatar.translationX(0f)
        binding.searchIb.translationX(0f)
        if (isDesktopLogin) {
            binding.desktopIb.translationX(0f)
        }
        clearFocus()
        binding.searchEt.hideKeyboard()
        binding.searchEt.setText("")
        mSearchViewListener?.onSearchViewClosed()
        isOpen = false
    }

    fun showLoading() {
        if (!isOpen) return

        binding.pb.isVisible = true
    }

    fun hideLoading() {
        if (!isOpen) return

        binding.pb.isInvisible = true
    }

    fun updateDesktop(login: Boolean) {
        isDesktopLogin = login
        if (login) {
            binding.desktopIb.isVisible = true
            binding.searchIb.updateLayoutParams<MarginLayoutParams> {
                marginEnd = 100.dp
            }
        } else {
            binding.desktopIb.isVisible = false
            binding.searchIb.updateLayoutParams<MarginLayoutParams> {
                marginEnd = 52.dp
            }
        }
    }

    private fun onTextChanged(newText: CharSequence) {
        mCurrentQuery = newText
        binding.rightClear.isVisible = newText.isNotEmpty()
        mOnQueryTextListener?.onQueryTextChange(newText.toString())
    }

    private fun onSubmitQuery() {
        val query = binding.searchEt.text

        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryTextListener == null) {
                closeSearch()
                binding.searchEt.setText("")
            }
        }
    }

    fun setSearchViewListener(mSearchViewListener: SearchViewListener) {
        this.mSearchViewListener = mSearchViewListener
    }

    fun setQuery(
        query: CharSequence?,
        submit: Boolean,
    ) {
        binding.searchEt.setText(query)

        if (query != null) {
            binding.searchEt.setSelection(binding.searchEt.length())
            mCurrentQuery = query
        }

        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery()
        }
    }

    fun setSearchBarColor(color: Int) {
        binding.searchEt.setBackgroundColor(color)
    }

    private fun adjustAlpha(
        color: Int,
        factor: Float,
    ): Int {
        if (factor < 0) return color

        val alpha = (Color.alpha(color) * factor).roundToInt()

        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun setHint(hint: CharSequence?) {
        binding.searchEt.hint = hint
    }

    fun setCancelIcon(resourceId: Int) {
        binding.backIb.setImageResource(resourceId)
    }

    fun setBackIcon(resourceId: Int) {
        binding.searchIb.setImageResource(resourceId)
    }

    fun setInputType(inputType: Int) {
        binding.searchEt.inputType = inputType
    }

    fun setSearchBarHeight(height: Int) {
        binding.searchView.minimumHeight = height
        binding.searchView.layoutParams.height = height
    }

    fun setOnGroupClickListener(onClickListener: OnClickListener) {
        binding.avatar.setOnClickListener(onClickListener)
    }

    fun setOnAddClickListener(onClickListener: OnClickListener) {
        binding.addIb.setOnClickListener(onClickListener)
    }

    fun setOnConfirmClickListener(onClickListener: OnClickListener) {
        binding.confirmIb.setOnClickListener(onClickListener)
    }

    fun setOnLeftClickListener(onClickListener: OnClickListener) {
        binding.searchIb.setOnClickListener(onClickListener)
    }

    var containerDisplay = false

    fun setOnBackClickListener(onClickListener: OnClickListener) {
        binding.backIb.setOnClickListener(onClickListener)
    }

    override fun clearFocus() {
        this.mClearingFocus = true
        hideKeyboard()
        super.clearFocus()
        binding.searchEt.clearFocus()
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

    override fun writeToParcel(
        out: Parcel,
        flags: Int,
    ) {
        super.writeToParcel(out, flags)
        out.writeInt(if (isOpen) 1 else 0)
    }

    companion object {
        @JvmField
        @NotNull
        val CREATOR: Parcelable.Creator<SavedState> =
            object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
