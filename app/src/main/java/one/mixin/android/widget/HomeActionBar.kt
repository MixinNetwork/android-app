package one.mixin.android.widget

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isGone
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_home_action_bar.view.*
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.screenWidth
import one.mixin.android.extension.showKeyboard

class HomeActionBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_home_action_bar, this, true)
        search_iv.setOnClickListener { openSearch() }
        contact_iv.setOnClickListener { callback?.onContactListener() }
        wallet_iv.setOnClickListener { callback?.onWalletListener() }
        cancel_tv.setOnClickListener { closeSearch() }

        search_et.listener = object : SearchView.OnSearchViewListener {
            override fun afterTextChanged(s: Editable?) {
                callback?.onQueryTextChange(s.toString())
            }

            override fun onSearch() {
                callback?.onQueryTextSubmit(search_et.text.toString())
            }
        }
    }

    var isOpen = false
        private set

    var callback: Callback? = null

    private fun openSearch() {
        if (isOpen) return
        showCircularReveal((search_iv.right + search_iv.left) / 2, (search_iv.bottom + search_iv.top) / 2, false)
    }

    fun closeSearch() {
        if (!isOpen) return
        showCircularReveal((cancel_tv.right + cancel_tv.left) / 2, (cancel_tv.bottom + cancel_tv.top) / 2, true)
    }

    private fun showCircularReveal(x: Int, y: Int, close: Boolean) {
        val radius = context.screenWidth().toFloat()
        val startRadius = if (close) radius else 0f
        val endRadius = if (close) 0f else radius
        isOpen = !close
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

    interface Callback {
        fun onContactListener()
        fun onWalletListener()
        fun onQueryTextSubmit(query: String)
        fun onQueryTextChange(newText: String)
        fun onSearchViewOpened()
        fun onSearchViewClosed()
    }
}