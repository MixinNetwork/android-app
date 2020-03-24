package one.mixin.android.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.jetbrains.anko.backgroundDrawable
import org.jetbrains.anko.displayMetrics

class MixinBottomSheetDialog(context: Context, theme: Int) : BottomSheetDialog(context, theme) {

    private var startAnimationRunnable: Runnable? = null
    private var curSheetAnimation: AnimatorSet? = null
    private val backDrawable = ColorDrawable(-0x1000000)
    private var isDismissed = false
    private var isShown = false

    private lateinit var container: View
    private lateinit var sheetContainer: View

    override fun setContentView(view: View) {
        super.setContentView(view)
        container = window!!.findViewById<View>(com.google.android.material.R.id.container)
        container.backgroundDrawable = backDrawable
        backDrawable.alpha = 0
        sheetContainer = window!!.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
    }

    override fun show() {
        try {
            super.show()
        } catch (ignored: Exception) {
        }

        sheetContainer.measure(View.MeasureSpec.makeMeasureSpec(context.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(context.displayMetrics.heightPixels, View.MeasureSpec.AT_MOST))
        if (isShown) return
        backDrawable.alpha = 0
        sheetContainer.translationY = sheetContainer.measuredHeight.toFloat()
        startAnimationRunnable = object : Runnable {
            override fun run() {
                if (startAnimationRunnable != this || isDismissed) {
                    return
                }
                startAnimationRunnable = null
                startOpenAnimation()
            }
        }
        sheetContainer.post(startAnimationRunnable)
    }

    private fun startOpenAnimation() {
        if (isDismissed) {
            return
        }
        sheetContainer.visibility = View.VISIBLE
        container.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        sheetContainer.translationY = sheetContainer.measuredHeight.toFloat()
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(sheetContainer, "translationY", 0f),
            ObjectAnimator.ofInt(backDrawable, "alpha", 51)
        )
        animatorSet.duration = 200
        animatorSet.startDelay = 20
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                if (curSheetAnimation != null && curSheetAnimation == animation) {
                    curSheetAnimation = null
                    container.setLayerType(View.LAYER_TYPE_NONE, null)
                    isShown = true
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
                if (curSheetAnimation != null && curSheetAnimation == animation) {
                    curSheetAnimation = null
                }
            }
        })
        animatorSet.start()
        curSheetAnimation = animatorSet
    }

    override fun dismiss() {
        if (isDismissed) {
            return
        }
        isDismissed = true
        isShown = false
        dismissInternal()
    }

    private fun dismissInternal() {
        cancelSheetAnimation()
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(sheetContainer, "translationY", sheetContainer.measuredHeight.toFloat()),
            ObjectAnimator.ofInt(backDrawable, "alpha", 0)
        )
        animatorSet.duration = 180
        animatorSet.interpolator = AccelerateInterpolator()
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (curSheetAnimation != null && curSheetAnimation == animation) {
                    curSheetAnimation = null
                    sheetContainer.post {
                        superDismiss()
                    }
                }
            }

            override fun onAnimationCancel(animation: Animator) {
                if (curSheetAnimation != null && curSheetAnimation == animation) {
                    curSheetAnimation = null
                }
            }
        })
        animatorSet.start()
        curSheetAnimation = animatorSet
    }

    private fun superDismiss() {
        try {
            super.dismiss()
        } catch (e: Exception) {
        }
    }

    private fun cancelSheetAnimation() {
        curSheetAnimation?.cancel()
        curSheetAnimation = null
    }
}
