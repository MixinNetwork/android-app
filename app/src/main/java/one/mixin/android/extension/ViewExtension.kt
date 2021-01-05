package one.mixin.android.extension

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Outline
import android.net.Uri
import android.os.Bundle
import android.util.Property
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringSystem
import org.jetbrains.anko.dip
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.jvm.Throws

const val ANIMATION_DURATION_SHORT = 260L
const val ANIMATION_DURATION_SHORTEST = 120L

const val FLAGS_FULLSCREEN =
    View.SYSTEM_UI_FLAG_LOW_PROFILE or
        View.SYSTEM_UI_FLAG_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

fun View.hideKeyboard() {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    if (requestFocus()) {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(this, SHOW_IMPLICIT)
    }
}

fun View.fadeIn(maxAlpha: Float = 1f) {
    this.fadeIn(ANIMATION_DURATION_SHORT, maxAlpha)
}

fun View.fadeIn(duration: Long, maxAlpha: Float = 1f) {
    this.visibility = VISIBLE
    this.alpha = 0f
    ViewCompat.animate(this).alpha(maxAlpha).setDuration(duration).setListener(
        object : ViewPropertyAnimatorListener {
            override fun onAnimationStart(view: View) {
            }

            override fun onAnimationEnd(view: View) {
            }

            override fun onAnimationCancel(view: View) {}
        }
    ).start()
}

fun View.fadeOut(isGone: Boolean = false) {
    this.fadeOut(ANIMATION_DURATION_SHORT, isGone = isGone)
}

fun View.fadeOut(duration: Long, delay: Long = 0, isGone: Boolean = false) {
    this.alpha = 1f
    ViewCompat.animate(this).alpha(0f).setStartDelay(delay).setDuration(duration).setListener(
        object : ViewPropertyAnimatorListener {
            override fun onAnimationStart(view: View) {
                view.isDrawingCacheEnabled = true
            }

            override fun onAnimationEnd(view: View) {
                view.visibility = if (isGone) GONE else INVISIBLE
                view.alpha = 0f
                view.isDrawingCacheEnabled = false
            }

            override fun onAnimationCancel(view: View) {}
        }
    )
}

fun View.translationX(value: Float) {
    this.translationX(value, ANIMATION_DURATION_SHORT)
}

fun View.translationX(value: Float, duration: Long) {
    ViewCompat.animate(this).setDuration(duration).translationX(value).start()
}

fun View.translationY(value: Float, endAction: (() -> Unit)? = null) {
    this.translationY(value, ANIMATION_DURATION_SHORT, endAction)
}

fun View.translationY(value: Float, duration: Long, endAction: (() -> Unit)? = null) {
    ViewCompat.animate(this).setDuration(duration).translationY(value)
        .setListener(
            object : ViewPropertyAnimatorListener {
                override fun onAnimationEnd(view: View?) {
                    endAction?.let { it() }
                }

                override fun onAnimationCancel(view: View?) {
                    endAction?.let { it() }
                }

                override fun onAnimationStart(view: View?) {}
            }
        )
        .start()
}

fun View.shaking() {
    val dp20 = dip(20).toFloat()
    val dp10 = dip(10).toFloat()
    val dp5 = dip(5).toFloat()
    ObjectAnimator.ofFloat(this, "translationX", -dp20, dp20, -dp20, dp20, -dp10, dp10, -dp5, dp5, 0f)
        .setDuration(600).start()
}

fun View.shakeAnimator() =
    ObjectAnimator.ofFloat(this, "rotation", 0f, -1f, 0f, 1f, 0f).apply {
        repeatCount = ObjectAnimator.INFINITE
        repeatMode = ObjectAnimator.REVERSE
        duration = 450
    }

fun View.animateWidth(form: Int, to: Int) {
    this.animateWidth(form, to, ANIMATION_DURATION_SHORT)
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
    duration: Long = ANIMATION_DURATION_SHORT,
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

fun View.roundTopOrBottom(radius: Float, top: Boolean, bottom: Boolean) {
    this.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            val t = if (!top) {
                -radius.toInt()
            } else {
                0
            }
            val b = if (!bottom) {
                (height + radius).toInt()
            } else {
                height
            }
            outline.setRoundRect(0, t, view.width, b, radius)
        }
    }
    this.clipToOutline = true
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

fun View.navigateUp() {
    try {
        findNavController().navigateUp()
    } catch (e: IllegalArgumentException) {
        // Workaround with https://issuetracker.google.com/issues/128881182
    } catch (e: IllegalStateException) {
        Timber.w("View $this does not have a NavController set")
    }
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

@Throws(IOException::class)
fun View.capture(context: Context): String? {
    val outFile = context.getPublicPicturePath().createImagePngTemp(false)
    val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    draw(c)
    b.save(outFile)
    return try {
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(outFile)))
        outFile.absolutePath
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
        null
    }
}

private val springSystem = SpringSystem.create()
private val sprintConfig = SpringConfig.fromOrigamiTensionAndFriction(80.0, 4.0)

fun View.bounce() {
    val spring = springSystem.createSpring()
        .setSpringConfig(sprintConfig)
        .addListener(
            object : SimpleSpringListener() {
                override fun onSpringUpdate(spring: Spring) {
                    val value = spring.currentValue.toFloat()
                    scaleX = value
                    scaleY = value
                }
            }
        )
    spring.endValue = 1.0
}

fun View.IntProperty(name: String, getAction: (View) -> Int, setAction: (View, Int) -> Unit): Property<View, Int> {
    return object : Property<View, Int>(Int::class.java, name) {
        override fun get(obj: View): Int {
            return getAction(obj)
        }

        override fun set(obj: View, value: Int) {
            return setAction(obj, value)
        }
    }
}

fun View.isActivityNotDestroyed(): Boolean {
    val context = this.context ?: return false
    if (context is Activity) {
        if (context.isDestroyed || context.isFinishing) {
            return false
        }
    }
    return true
}

fun isDarkColor(@ColorInt color: Int) = ColorUtils.calculateLuminance(color) < 0.5

@ColorInt
fun Int.withAlpha(alpha: Float): Int {
    val result = 255.coerceAtMost(0.coerceAtLeast((alpha * 255).toInt())) shl 24
    val rgb = 0x00ffffff and this
    return result + rgb
}
