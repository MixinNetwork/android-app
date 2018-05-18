package one.mixin.android.extension

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Outline
import android.support.annotation.LayoutRes
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPropertyAnimatorListener
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
import android.widget.EditText
import android.widget.TextView

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
    ViewCompat.animate(this).alpha(1f).setDuration(duration).setListener(null).start()
}

fun View.fadeOut() {
    this.fadeOut(ANIMATION_DURATION_SHORTEST)
}

fun View.fadeOut(duration: Long) {
    this.alpha = 1f
    ViewCompat.animate(this).alpha(0f).setDuration(duration).setListener(object : ViewPropertyAnimatorListener {
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

fun View.translationY(value: Float) {
    this.translationY(value, ANIMATION_DURATION_SHORTEST)
}

fun View.translationY(value: Float, duration: Long) {
    ViewCompat.animate(this).setDuration(duration).translationY(value).start()
}

fun View.animateWidth(form: Int, to: Int) {
    this.animateWidth(form, to, one.mixin.android.extension.ANIMATION_DURATION_SHORTEST)
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

fun View.animateHeight(form: Int, to: Int) {
    this.animateHeight(form, to, one.mixin.android.extension.ANIMATION_DURATION_SHORTEST)
}

fun View.animateHeight(form: Int, to: Int, duration: Long) {
    val anim = ValueAnimator.ofInt(form, to)
    anim.addUpdateListener { valueAnimator ->
        layoutParams.height = valueAnimator.animatedValue as Int
        requestLayout()
    }
    anim.duration = duration
    if (to == 0 || form == 0) {
        anim.addListener(object : AnimatorListenerAdapter() {

            override fun onAnimationEnd(animation: Animator?) {
                if (to == 0) {
                    this@animateHeight.visibility = GONE
                }
            }

            override fun onAnimationStart(animation: Animator?) {
                if (form == 0) {
                    this@animateHeight.visibility = VISIBLE
                }
            }
        })
    }
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