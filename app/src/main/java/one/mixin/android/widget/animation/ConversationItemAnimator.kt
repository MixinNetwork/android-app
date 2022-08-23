package one.mixin.android.widget.animation

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.extension.ANIMATION_DURATION_SHORTEST
import timber.log.Timber

class ConversationItemAnimator(
    private val isInMultiSelectMode: () -> Boolean,
    private val shouldPlayMessageAnimations: () -> Boolean,
    private val isParentFilled: () -> Boolean
) : RecyclerView.ItemAnimator() {

    private data class TweeningInfo(
        val startValue: Float,
        val endValue: Float
    ) {
        fun lerp(progress: Float): Float {
            return startValue + progress * (endValue - startValue)
        }
    }

    private data class AnimationInfo(
        val sharedAnimator: ValueAnimator,
        val tweeningInfo: TweeningInfo
    )

    private val pendingSlideAnimations: MutableMap<RecyclerView.ViewHolder, TweeningInfo> = mutableMapOf()
    private val slideAnimations: MutableMap<RecyclerView.ViewHolder, AnimationInfo> = mutableMapOf()

    private var firstDisappearanceAnimate = true
    override fun animateDisappearance(viewHolder: RecyclerView.ViewHolder, preLayoutInfo: ItemHolderInfo, postLayoutInfo: ItemHolderInfo?): Boolean {
        if (!pendingSlideAnimations.containsKey(viewHolder) &&
            !slideAnimations.containsKey(viewHolder) &&
            shouldPlayMessageAnimations() &&
            isParentFilled() &&
            postLayoutInfo != null
        ) {
            if (firstDisappearanceAnimate) {
                firstDisappearanceAnimate = false
                dispatchAnimationFinished(viewHolder)
                return false
            }
            pendingSlideAnimations[viewHolder] = TweeningInfo((preLayoutInfo.bottom -postLayoutInfo.bottom).toFloat(), 0f)
            dispatchAnimationStarted(viewHolder)
            return true
        }
        dispatchAnimationFinished(viewHolder)
        return false
    }

    private var firstAppearanceAnimate = true
    override fun animateAppearance(viewHolder: RecyclerView.ViewHolder, preLayoutInfo: ItemHolderInfo?, postLayoutInfo: ItemHolderInfo): Boolean {
        if (viewHolder.absoluteAdapterPosition >= 1) {
            dispatchAnimationFinished(viewHolder)
            return false
        }
        if (firstAppearanceAnimate) {
            firstAppearanceAnimate = false
            dispatchAnimationFinished(viewHolder)
            return false
        }
        return animateSlide(viewHolder, preLayoutInfo, postLayoutInfo)
    }

    private fun animateSlide(viewHolder: RecyclerView.ViewHolder, preLayoutInfo: ItemHolderInfo?, postLayoutInfo: ItemHolderInfo): Boolean {
        if (isInMultiSelectMode() || !shouldPlayMessageAnimations()) {
            Timber.d(TAG, "Dropping slide animation: (${isInMultiSelectMode()}, ${shouldPlayMessageAnimations()}) :: ${viewHolder.absoluteAdapterPosition}")
            dispatchAnimationFinished(viewHolder)
            return false
        }

        if (slideAnimations.containsKey(viewHolder)) {
            dispatchAnimationFinished(viewHolder)
            return false
        }

        val translationY = if (preLayoutInfo == null) {
            postLayoutInfo.bottom - postLayoutInfo.top
        } else {
            preLayoutInfo.top - postLayoutInfo.top
        }.toFloat()

        if (translationY == 0f) {
            viewHolder.itemView.translationY = 0f
            dispatchAnimationFinished(viewHolder)
            return false
        }

        viewHolder.itemView.translationY = translationY

        pendingSlideAnimations[viewHolder] = TweeningInfo(translationY, 0f)
        dispatchAnimationStarted(viewHolder)
        return true
    }

    private var firstPersistence = true
    override fun animatePersistence(viewHolder: RecyclerView.ViewHolder, preLayoutInfo: ItemHolderInfo, postLayoutInfo: ItemHolderInfo): Boolean {
        return if (!isInMultiSelectMode() && shouldPlayMessageAnimations() && isParentFilled()) {
            if (pendingSlideAnimations.contains(viewHolder) || slideAnimations.containsKey(viewHolder)) {
                dispatchAnimationFinished(viewHolder)
                false
            } else {
                if (firstPersistence) {
                    firstPersistence = false
                    dispatchAnimationFinished(viewHolder)
                    return false
                }
                animateSlide(viewHolder, preLayoutInfo, postLayoutInfo)
            }
        } else {
            Timber.d(TAG, "Dropping persistence animation: (${isInMultiSelectMode()}, ${shouldPlayMessageAnimations()}, ${isParentFilled()}) :: ${viewHolder.absoluteAdapterPosition}")
            dispatchAnimationFinished(viewHolder)
            false
        }
    }

    override fun animateChange(oldHolder: RecyclerView.ViewHolder, newHolder: RecyclerView.ViewHolder, preLayoutInfo: ItemHolderInfo, postLayoutInfo: ItemHolderInfo): Boolean {
        if (oldHolder != newHolder) {
            dispatchAnimationFinished(oldHolder)
        }

        return animatePersistence(newHolder, preLayoutInfo, postLayoutInfo)
    }

    override fun runPendingAnimations() {
        Timber.d(TAG, "Starting ${pendingSlideAnimations.size} animations.")
        runPendingSlideAnimations()
    }

    private fun runPendingSlideAnimations() {
        val animators: MutableList<Animator> = mutableListOf()
        for ((viewHolder, tweeningInfo) in pendingSlideAnimations) {
            val animator = ValueAnimator.ofFloat(0f, 1f)
            slideAnimations[viewHolder] = AnimationInfo(animator, tweeningInfo)
            animator.duration = ANIMATION_DURATION_SHORTEST
            animator.addUpdateListener {
                if (viewHolder in slideAnimations) {
                    viewHolder.itemView.translationY = tweeningInfo.lerp(it.animatedFraction)
                    (viewHolder.itemView.parent as RecyclerView?)?.invalidate()
                }
            }
            animator.doOnEnd {
                if (viewHolder in slideAnimations) {
                    handleAnimationEnd(viewHolder)
                }
            }
            animators.add(animator)
        }

        AnimatorSet().apply {
            playTogether(animators)
            start()
        }

        pendingSlideAnimations.clear()
    }

    private fun handleAnimationEnd(viewHolder: RecyclerView.ViewHolder) {
        viewHolder.itemView.translationY = 0f
        slideAnimations.remove(viewHolder)
        dispatchAnimationFinished(viewHolder)
        dispatchFinishedWhenDone()
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        endSlideAnimation(item)
    }

    override fun endAnimations() {
        endSlideAnimations()
        dispatchAnimationsFinished()
    }

    override fun isRunning(): Boolean {
        return slideAnimations.values.any { it.sharedAnimator.isRunning }
    }

    override fun onAnimationFinished(viewHolder: RecyclerView.ViewHolder) {
        val parent = (viewHolder.itemView.parent as? RecyclerView)
        parent?.post { parent.invalidate() }
    }

    private fun endSlideAnimation(item: RecyclerView.ViewHolder) {
        slideAnimations[item]?.sharedAnimator?.cancel()
    }

    private fun endSlideAnimations() {
        slideAnimations.values.map { it.sharedAnimator }.forEach {
            it.cancel()
        }
    }

    private fun dispatchFinishedWhenDone() {
        if (!isRunning) {
            Timber.d(TAG, "Finished running animations.")
            dispatchAnimationsFinished()
        }
    }

    companion object {
        private val TAG = ConversationItemAnimator::class.java.simpleName
    }
}
