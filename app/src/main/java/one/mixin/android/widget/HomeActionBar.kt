package one.mixin.android.widget

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.view_home_action_bar.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.screenWidth
import one.mixin.android.extension.showKeyboard
import one.mixin.android.ui.search.SearchFragment.Companion.SEARCH_DEBOUNCE
import org.jetbrains.annotations.NotNull
import java.util.concurrent.TimeUnit

class HomeActionBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    private var disposable: Disposable? = null
    init {
        LayoutInflater.from(context).inflate(R.layout.view_home_action_bar, this, true)
        search_iv.setOnClickListener { openSearch() }
        contact_iv.setOnClickListener { callback?.onContactListener() }
        wallet_iv.setOnClickListener { callback?.onWalletListener() }
        cancel_tv.setOnClickListener { closeSearch() }

        search_et.listener = object : SearchView.OnSearchViewListener {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun onSearch() {
                callback?.onQueryTextSubmit(search_et.text.toString())
            }
        }
        disposable = search_et.textChanges().debounce(SEARCH_DEBOUNCE, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                callback?.onQueryTextChange(it.toString())
            }, {})
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposable?.dispose()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val parcelable = super.onSaveInstanceState() ?: return null
        return SavedState(parcelable).apply {
            this.isOpen = this@HomeActionBar.isOpen
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        isOpen = (state as? SavedState)?.isOpen ?: false
        if (isOpen) openSearch() else closeSearch()
        super.onRestoreInstanceState(state)
    }

    var isOpen = false
        private set

    var callback: Callback? = null

    fun openSearch() {
        showCircularReveal((search_iv.right + search_iv.left) / 2, (search_iv.bottom + search_iv.top) / 2, false)
    }

    fun closeSearch() {
        showCircularReveal((cancel_tv.right + cancel_tv.left) / 2, (cancel_tv.bottom + cancel_tv.top) / 2, true)
    }

    fun setCalling(click: () -> Unit) {
        pb.isGone = true
        title_tv.updateLayoutParams<RelativeLayout.LayoutParams> {
            marginStart = context.dpToPx(20f)
        }
        title_tv.text = context.getString(R.string.state_calling)
        title_tv.setOnClickListener {
            click.invoke()
        }
    }

    fun setConnecting() {
        pb.isVisible = true
        title_tv.updateLayoutParams<RelativeLayout.LayoutParams> {
            marginStart = context.dpToPx(4f)
        }
        title_tv.text = context.getString(R.string.state_connecting)
    }

    fun setSyncing() {
        pb.isVisible = true
        title_tv.updateLayoutParams<RelativeLayout.LayoutParams> {
            marginStart = context.dpToPx(4f)
        }
        title_tv.text = context.getString(R.string.state_syncing)
    }

    fun setNormal() {
        pb.isGone = true
        title_tv.updateLayoutParams<RelativeLayout.LayoutParams> {
            marginStart = context.dpToPx(20f)
        }
        title_tv.text = context.getString(R.string.app_name)
    }

    private fun showCircularReveal(x: Int, y: Int, close: Boolean) {
        val radius = context.screenWidth().toFloat()
        val startRadius = if (close) radius else 0f
        val endRadius = if (close) 0f else radius
        isOpen = !close
        if (!isAttachedToWindow) {
            search_rl.isVisible = isOpen
            return
        }
        ViewAnimationUtils.createCircularReveal(search_rl, x, y, startRadius, endRadius).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            doOnStart {
                if (!close) {
                    search_rl.isVisible = true
                    callback?.onSearchViewOpened()
                    search_et.showKeyboard()
                }
            }
            doOnEnd {
                if (close) {
                    search_rl.isGone = true
                    callback?.onSearchViewClosed()
                    search_et.hideKeyboard()
                }
            }
        }.start()
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

    interface Callback {
        fun onContactListener()
        fun onWalletListener()
        fun onQueryTextSubmit(query: String)
        fun onQueryTextChange(newText: String)
        fun onSearchViewOpened()
        fun onSearchViewClosed()
    }
}