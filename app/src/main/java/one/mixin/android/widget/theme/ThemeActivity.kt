package one.mixin.android.widget.theme

import android.animation.Animator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import one.mixin.android.ui.common.BlazeBaseActivity
import kotlin.math.sqrt

abstract class ThemeActivity : BlazeBaseActivity() {
    private lateinit var root: View
    private lateinit var frontFakeThemeView: FakeThemeView
    private lateinit var behindFakeThemeView: FakeThemeView
    private lateinit var decorView: FrameLayout

    private var anim: Animator? = null

    override fun onCreate(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?,
    ) {
        super.onCreate(savedInstanceState, persistentState)
        initViews()
        super.setContentView(root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
        super.setContentView(root)
    }

    override fun setContentView(
        @LayoutRes layoutResID: Int,
    ) {
        setContentView(LayoutInflater.from(this).inflate(layoutResID, decorView))
    }

    override fun setContentView(view: View?) {
        decorView.removeAllViews()
        decorView.addView(view)
    }

    override fun setContentView(
        view: View?,
        params: ViewGroup.LayoutParams?,
    ) {
        decorView.removeAllViews()
        decorView.addView(view, params)
    }

    private fun initViews() {
        // create roo view
        root =
            FrameLayout(this).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )

                // create and add behindFakeThemeImageView
                addView(
                    FakeThemeView(context).apply {
                        layoutParams =
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                            )
                        visibility = View.GONE
                        behindFakeThemeView = this
                    },
                )

                // create and add decorView, ROOT_ID is generated ID
                addView(
                    FrameLayout(context).apply {
                        layoutParams =
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                            )
                        decorView = this
                        id = ROOT_ID
                    },
                )

                // create and add frontFakeThemeImageView
                addView(
                    FakeThemeView(context).apply {
                        layoutParams =
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                            )
                        visibility = View.GONE
                        frontFakeThemeView = this
                    },
                )
            }
    }

    fun changeTheme(
        sourceCoordinate: Coordinate,
        animDuration: Long,
        isReverse: Boolean,
        callback: () -> Unit,
    ) {
        if (frontFakeThemeView.visibility == View.VISIBLE ||
            behindFakeThemeView.visibility == View.VISIBLE ||
            isRunningChangeThemeAnimation()
        ) {
            return
        }

        // take screenshot
        val w = decorView.measuredWidth
        val h = decorView.measuredHeight
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        decorView.draw(canvas)

        // update theme

        // create anim
        val finalRadius = sqrt((w * w + h * h).toDouble()).toFloat()
        if (isReverse) {
            frontFakeThemeView.bitmap = bitmap
            frontFakeThemeView.visibility = View.VISIBLE
            anim =
                ViewAnimationUtils.createCircularReveal(
                    frontFakeThemeView,
                    sourceCoordinate.x,
                    sourceCoordinate.y,
                    finalRadius,
                    0f,
                )
        } else {
            behindFakeThemeView.bitmap = bitmap
            behindFakeThemeView.visibility = View.VISIBLE
            anim =
                ViewAnimationUtils.createCircularReveal(
                    decorView,
                    sourceCoordinate.x,
                    sourceCoordinate.y,
                    0f,
                    finalRadius,
                )
        }

        // set duration
        anim?.duration = animDuration

        // set listener
        anim?.addListener(
            object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    behindFakeThemeView.bitmap = null
                    frontFakeThemeView.bitmap = null
                    frontFakeThemeView.visibility = View.GONE
                    behindFakeThemeView.visibility = View.GONE
                    callback.invoke()
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            },
        )

        anim?.start()
    }

    private fun isRunningChangeThemeAnimation(): Boolean {
        return anim?.isRunning == true
    }

    companion object {
        internal val ROOT_ID = ViewCompat.generateViewId()
    }
}
