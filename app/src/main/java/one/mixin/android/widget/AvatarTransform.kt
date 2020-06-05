package one.mixin.android.widget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.transition.Transition
import android.transition.TransitionValues
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import androidx.core.transition.doOnEnd

class AvatarTransform(private val bitmap: Bitmap) : Transition() {
    init { duration = DURATION }

    companion object {
        const val BOUNDS = "avatar_transform_bounds"
        const val DURATION = 250L
    }

    override fun captureStartValues(transitionValues: TransitionValues) = captureValues(transitionValues)
    override fun captureEndValues(transitionValues: TransitionValues) = captureValues(transitionValues)

    override fun createAnimator(sceneRoot: ViewGroup, startValues: TransitionValues?, endValues: TransitionValues?): Animator? {
        if (startValues == null || endValues == null) return null

        val startBounds = startValues.values[BOUNDS] as Rect
        val endBounds = endValues.values[BOUNDS] as Rect
        val v = endValues.view
        val translationX = (startBounds.centerX() - endBounds.centerX()).toFloat()
        val translationY = (startBounds.centerY() - endBounds.centerY()).toFloat()
        v.translationX = translationX
        v.translationY = translationY

        val startDrawable = BitmapDrawable(sceneRoot.resources, bitmap)
        startDrawable.setBounds(0, 0, endBounds.width(), endBounds.height())
        v.overlay.add(startDrawable)

        val circularReveal = ViewAnimationUtils.createCircularReveal(
            v,
            v.width / 2,
            v.height / 2,
            startBounds.width() / 2f,
            Math.hypot(endBounds.width() / 2.0, endBounds.height() / 2.0).toFloat()
        ).apply {
            duration = DURATION
        }

        val translate = ObjectAnimator.ofFloat(
            v,
            View.TRANSLATION_X,
            View.TRANSLATION_Y,
            pathMotion.getPath(translationX, translationY, 0f, 0f)
        ).apply {
            duration = DURATION
        }

        return AnimatorSet().apply {
            playTogether(circularReveal, translate)
            doOnEnd { v.overlay.clear() }
        }
    }

    private fun captureValues(transitionValues: TransitionValues) {
        val view = transitionValues.view
        if (view.width <= 0 || view.height <= 0) return
        transitionValues.values[BOUNDS] = Rect(view.left, view.top, view.right, view.bottom)
    }
}
