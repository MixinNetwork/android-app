package one.mixin.android.extension

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Outline
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import one.mixin.android.R
import org.jetbrains.anko.dip
import timber.log.Timber

const val ANIMATION_DURATION_SHORTEST = 260L

fun View.hideKeyboard() {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    requestFocus()
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(this, SHOW_IMPLICIT)
}

fun View.fadeIn() {
    this.fadeIn(ANIMATION_DURATION_SHORTEST)
}

fun View.fadeIn(duration: Long) {
    this.visibility = View.VISIBLE
    this.alpha = 0f
    ViewCompat.animate(this).alpha(1f).setDuration(duration).setListener(object : ViewPropertyAnimatorListener {
        override fun onAnimationStart(view: View) {
        }

        override fun onAnimationEnd(view: View) {
        }

        override fun onAnimationCancel(view: View) {}
    }).start()
}

fun View.fadeOut() {
    this.fadeOut(ANIMATION_DURATION_SHORTEST)
}

fun View.fadeOut(duration: Long, delay: Long = 0) {
    this.alpha = 1f
    ViewCompat.animate(this).alpha(0f).setStartDelay(delay).setDuration(duration).setListener(object : ViewPropertyAnimatorListener {
        override fun onAnimationStart(view: View) {
            view.isDrawingCacheEnabled = true
        }

        override fun onAnimationEnd(view: View) {
            view.visibility = View.INVISIBLE
            view.alpha = 0f
            view.isDrawingCacheEnabled = false
        }

        override fun onAnimationCancel(view: View) {}
    })
}

fun View.translationX(value: Float) {
    this.translationX(value, ANIMATION_DURATION_SHORTEST)
}

fun View.translationX(value: Float, duration: Long) {
    ViewCompat.animate(this).setDuration(duration).translationX(value).start()
}

fun View.translationY(value: Float, endAction: (() -> Unit)? = null) {
    this.translationY(value, ANIMATION_DURATION_SHORTEST, endAction)
}

fun View.translationY(value: Float, duration: Long, endAction: (() -> Unit)? = null) {
    ViewCompat.animate(this).setDuration(duration).translationY(value)
        .setListener(object : ViewPropertyAnimatorListener {
            override fun onAnimationEnd(view: View?) {
                endAction?.let { it() }
            }

            override fun onAnimationCancel(view: View?) {
                endAction?.let { it() }
            }

            override fun onAnimationStart(view: View?) {}
        })
        .start()
}

fun View.shaking() {
    val dp20 = dip(20).toFloat()
    val dp10 = dip(10).toFloat()
    val dp5 = dip(5).toFloat()
    ObjectAnimator.ofFloat(this, "translationX", -dp20, dp20, -dp20, dp20, -dp10, dp10, -dp5, dp5, 0f)
        .setDuration(600).start()
}

fun View.animateWidth(form: Int, to: Int) {
    this.animateWidth(form, to, ANIMATION_DURATION_SHORTEST)
}

fun View.animateWidth(form: Int, to: Int, duration: Long) {
    val anim = ValueAnimator.ofInt(form, to)
    anim.addUpdateListener { valueAnimator ->
        layoutParams.width = valueAnimator.animatedValue as Int
        requestLayout()
    }
    anim.duration = duration
    anim.start()
}

fun View.animateHeight(
    from: Int,
    to: Int,
    duration: Long = ANIMATION_DURATION_SHORTEST,
    interpolator: Interpolator = DecelerateInterpolator(),
    action: ((ValueAnimator) -> Unit)? = null,
    onEndAction: (() -> Unit)? = null
) {
    val anim = ValueAnimator.ofInt(from, to).apply {
        this.duration = duration
        this.interpolator = interpolator
        addUpdateListener { valueAnimator ->
            updateLayoutParams<ViewGroup.LayoutParams> {
                this.height = valueAnimator.animatedValue as Int
            }
        }
        doOnEnd {
            if (to == 0) {
                this@animateHeight.visibility = GONE
            }
            onEndAction?.invoke()
        }
        doOnStart {
            if (from == 0) {
                this@animateHeight.visibility = VISIBLE
            }
        }
    }
    action?.invoke(anim)
    anim.start()
}

fun View.round(radius: Float) {
    this.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }
    this.clipToOutline = true
}

fun View.round(radius: Int) {
    round(radius.toFloat())
}

fun EditText.showCursor() {
    this.requestFocus()
    this.isCursorVisible = true
}

fun EditText.hideCursor() {
    this.clearFocus()
    this.isCursorVisible = false
}

fun ViewGroup.inflate(
    @LayoutRes layoutRes: Int,
    attachToRoot: Boolean = false
) = LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)!!

fun TextView.timeAgo(str: String) {
    text = str.timeAgo(context)
}

fun TextView.timeAgoClock(str: String) {
    text = str.timeAgoClock()
}

fun TextView.timeAgoDate(str: String) {
    text = str.timeAgoDate(context)
}

fun TextView.timeAgoDay(str: String) {
    text = str.timeAgoDay()
}

fun View.navigate(
    resId: Int,
    bundle: Bundle? = null,
    navOptions: NavOptions? = null
) {
    try {
        findNavController().navigate(resId, bundle, navOptions)
    } catch (e: IllegalArgumentException) {
        // Workaround with https://issuetracker.google.com/issues/128881182
    } catch (e: IllegalStateException) {
        Timber.w("View $this does not have a NavController set")
    }
}

fun TextView.highLight(
    target: String?,
    ignoreCase: Boolean = true,
    @ColorInt color: Int = resources.getColor(R.color.wallet_blue_secondary, null)
) {
    if (target == null) {
        text = target
        return
    }
    val text = this.text.toString()
    val spannable = SpannableString(text)
    var index = text.indexOf(target, ignoreCase = ignoreCase)
    while (index != -1) {
        spannable.setSpan(
            TextAppearanceSpan(null, 0, 0, ColorStateList.valueOf(color), null),
            index, index + target.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        index = text.indexOf(target, index + target.length, ignoreCase = ignoreCase)
    }
    setText(spannable)
}