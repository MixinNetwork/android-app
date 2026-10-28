package one.mixin.android.widget

import one.mixin.android.extension.isWideScreen
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Job
import one.mixin.android.R
import one.mixin.android.extension.backgroundDrawable
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.isTablet
import one.mixin.android.extension.displayMetrics
import one.mixin.android.util.avoidBottomSheetFolds
import kotlin.math.abs

class MixinBottomSheetDialog(
    context: Context,
    theme: Int,
    val transparentStatusBar: Boolean = false,
    private val applyBottomInsetToSheet: Boolean = true,
    private val allowTabletop: Boolean = false,
) : BottomSheetDialog(context, theme) {
    companion object {
        const val BACK_DRAWABLE_ALPHA = 51
    }

    private var foldJob: Job? = null

    private var startAnimationRunnable: Runnable? = null
    private var curSheetAnimation: AnimatorSet? = null
    private val backDrawable = ColorDrawable(-0x1000000)
    private var isDismissed = false
    private var isShown = false

    private lateinit var container: View
    private lateinit var sheetContainer: View

    override fun setContentView(view: View) {
        super.setContentView(view)
        container = window!!.findViewById(com.google.android.material.R.id.container)
        container.backgroundDrawable = backDrawable
        backDrawable.alpha = 0
        sheetContainer = window!!.findViewById(com.google.android.material.R.id.design_bottom_sheet)
        if (context.isExpandedSheetScreen()) {
            behavior.maxWidth = context.resources.getDimensionPixelSize(R.dimen.adaptive_sheet_max_width)
        } else {
            sheetContainer.updateLayoutParams<ViewGroup.LayoutParams> {
                width =
                    when {
                        context.isWideScreen() -> (context.displayMetrics.widthPixels * 0.5).toInt()
                        context.isTablet() -> (context.displayMetrics.widthPixels * 0.8).toInt()
                        else -> width
                    }
            }
        }
        behavior.isGestureInsetBottomIgnored = true
        behavior.addBottomSheetCallback(bottomSheetBehaviorCallback)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.takeIf { it.isExpandedSheetScreen() }?.findFragmentActivityOrNull()?.let { activity ->
            findViewById<View>(com.google.android.material.R.id.container)?.let { container ->
                foldJob?.cancel()
                foldJob = activity.avoidBottomSheetFolds(activity, container, allowTabletop)
            }
        }
        applyBottomSheetContainerInsets(
            transparentStatusBar = transparentStatusBar,
            applyBottomInsetToSheet = applyBottomInsetToSheet,
        )
    }

    override fun onDetachedFromWindow() {
        foldJob?.cancel()
        foldJob = null
        super.onDetachedFromWindow()
    }

    override fun show() {
        try {
            super.show()
        } catch (ignored: Exception) {
        }
        if (!::sheetContainer.isInitialized) return

        sheetContainer.measure(
            View.MeasureSpec.makeMeasureSpec(context.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(context.displayMetrics.heightPixels, View.MeasureSpec.AT_MOST),
        )
        if (isShown) return
        backDrawable.alpha = 0
        sheetContainer.translationY = sheetContainer.measuredHeight.toFloat()
        startAnimationRunnable =
            object : Runnable {
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
            ObjectAnimator.ofInt(backDrawable, "alpha", BACK_DRAWABLE_ALPHA),
        )
        animatorSet.duration = 200
        animatorSet.startDelay = 20
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (curSheetAnimation != null && curSheetAnimation == animation) {
                        curSheetAnimation = null
                        container.setLayerType(View.LAYER_TYPE_NONE, null)
                        isShown = true
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    if (curSheetAnimation != null && curSheetAnimation == animation) {
                        curSheetAnimation = null
                    }
                }
            },
        )
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
        if (!::sheetContainer.isInitialized) return

        cancelSheetAnimation()
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(sheetContainer, "translationY", sheetContainer.measuredHeight.toFloat()),
            ObjectAnimator.ofInt(backDrawable, "alpha", 0),
        )
        animatorSet.duration = 180
        animatorSet.interpolator = AccelerateInterpolator()
        animatorSet.addListener(
            object : AnimatorListenerAdapter() {
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
            },
        )
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

    private val bottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) {
                val alpha =
                    if (slideOffset in 0.0..1.0) {
                        BACK_DRAWABLE_ALPHA
                    } else {
                        (BACK_DRAWABLE_ALPHA - abs(slideOffset) * BACK_DRAWABLE_ALPHA).toInt()
                    }
                backDrawable.alpha = alpha
            }
        }
}

internal class AdaptiveBottomSheetDialog(
    context: Context,
    theme: Int,
    private val centerOnExpandedScreen: Boolean = false,
) : BottomSheetDialog(context, theme) {
    private var foldJob: Job? = null
    var isCentered = false
        private set
    private var closing = false
    private var originalWindowAnimations = 0
    private var originalPeekHeight = 0
    private var originalFitToContents = true
    private var originalDraggable = true
    private var originalPaddingBottom = 0
    private var originalWindowHeight = ViewGroup.LayoutParams.MATCH_PARENT
    private var originalWindowGravity = Gravity.BOTTOM
    private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        updatePosition()
        true
    }
    private var sheet: View? = null
    private var coordinator: View? = null
    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updatePosition() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        originalWindowAnimations = window?.attributes?.windowAnimations ?: 0
        if (centerOnExpandedScreen && context.isExpandedSheetScreen()) {
            window?.setWindowAnimations(R.style.DialogNoAnimation)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyBottomSheetContainerInsets()
        if (!context.isExpandedSheetScreen()) return
        val activity = context.findFragmentActivityOrNull() ?: return
        findViewById<View>(com.google.android.material.R.id.container)?.let { container ->
            foldJob = activity.avoidBottomSheetFolds(activity, container)
        }
        if (!centerOnExpandedScreen) return
        sheet = findViewById(com.google.android.material.R.id.design_bottom_sheet)
        coordinator = findViewById(com.google.android.material.R.id.coordinator)
        originalPeekHeight = behavior.peekHeight
        originalFitToContents = behavior.isFitToContents
        originalDraggable = behavior.isDraggable
        originalPaddingBottom = sheet?.paddingBottom ?: 0
        sheet?.addOnLayoutChangeListener(layoutListener)
        coordinator?.addOnLayoutChangeListener(layoutListener)
        coordinator?.viewTreeObserver?.addOnPreDrawListener(preDrawListener)
        sheet?.let { card ->
            ViewCompat.setOnApplyWindowInsetsListener(card) { view, insets ->
                val bottom = if (context.isExpandedSheetScreen()) {
                    0
                } else {
                    originalPaddingBottom + insets.getInsets(WindowInsetsCompat.Type.tappableElement()).bottom
                }
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottom)
                view.post { updatePosition() }
                insets
            }
        }
        updatePosition()
        if (isCentered) {
            sheet?.apply {
                alpha = 0f
                scaleX = 0.92f
                scaleY = 0.92f
                animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
            }
        }
    }

    fun updatePosition() {
        if (!centerOnExpandedScreen) return
        val card = sheet ?: return
        val parent = coordinator ?: return
        val expanded = context.isExpandedSheetScreen()
        if (!expanded) {
            if (isCentered) {
                isCentered = false
                window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, originalWindowHeight)
                window?.setGravity(originalWindowGravity)
                window?.setWindowAnimations(originalWindowAnimations)
                card.clipToOutline = false
                behavior.isFitToContents = originalFitToContents
                behavior.isDraggable = originalDraggable
                behavior.maxWidth = context.resources.getDimensionPixelSize(R.dimen.adaptive_sheet_max_width)
                behavior.maxHeight = -1
                behavior.peekHeight = originalPeekHeight
                card.layoutParams = card.layoutParams.apply { width = ViewGroup.LayoutParams.MATCH_PARENT }
                ViewCompat.requestApplyInsets(card)
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            return
        }
        if (!isCentered) {
            isCentered = true
            originalWindowHeight = window?.attributes?.height ?: ViewGroup.LayoutParams.MATCH_PARENT
            originalWindowGravity = window?.attributes?.gravity ?: Gravity.BOTTOM
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window?.setWindowAnimations(R.style.DialogNoAnimation)
            originalPeekHeight = behavior.peekHeight
            originalFitToContents = behavior.isFitToContents
            originalDraggable = behavior.isDraggable
            card.setPadding(card.paddingLeft, card.paddingTop, card.paddingRight, 0)
            card.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, context.resources.getDimension(R.dimen.centered_sheet_corner_radius))
                }
            }
            card.clipToOutline = true
            behavior.isFitToContents = false
            behavior.isDraggable = false
        }
        if (parent.width == 0 || parent.height == 0) return
        // The coordinator already occupies the pane selected by avoidBottomSheetFolds.
        val visible = Rect()
        parent.getWindowVisibleDisplayFrame(visible)
        val location = IntArray(2)
        parent.getLocationOnScreen(location)
        val left = (visible.left - location[0]).coerceIn(0, parent.width)
        val right = (visible.right - location[0]).coerceIn(left, parent.width)
        val top = (visible.top - location[1]).coerceIn(0, parent.height)
        val bottom = (visible.bottom - location[1]).coerceIn(top, parent.height)
        val margin = context.resources.getDimensionPixelSize(R.dimen.centered_sheet_margin)
        val width = minOf(context.resources.getDimensionPixelSize(R.dimen.centered_sheet_max_width), (right - left - 2 * margin).coerceAtLeast(0))
        val height = (bottom - top - 2 * margin).coerceAtLeast(0)
        if (behavior.maxWidth != width) behavior.maxWidth = width
        if (behavior.maxHeight != height) {
            behavior.maxHeight = height
            card.requestLayout()
        }
        if (card.layoutParams.width != width) card.layoutParams = card.layoutParams.apply { this.width = width }
        val offset = top + (bottom - top - card.height.coerceAtMost(height)) / 2
        if (behavior.expandedOffset != offset) behavior.expandedOffset = offset
        if (behavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            if (behavior.state != BottomSheetBehavior.STATE_EXPANDED && behavior.state != BottomSheetBehavior.STATE_SETTLING) {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            card.offsetTopAndBottom(offset - card.top)
            card.offsetLeftAndRight(left + (right - left - card.width) / 2 - card.left)
        }
    }

    override fun dismiss() {
        if (!isCentered || sheet == null) {
            super.dismiss()
            return
        }
        if (closing) return
        closing = true
        sheet?.animate()?.cancel()
        sheet?.animate()?.alpha(0f)?.scaleX(0.92f)?.scaleY(0.92f)?.setDuration(150)?.withEndAction {
            super.dismiss()
        }?.start()
    }

    override fun onDetachedFromWindow() {
        sheet?.removeOnLayoutChangeListener(layoutListener)
        coordinator?.removeOnLayoutChangeListener(layoutListener)
        coordinator?.viewTreeObserver?.removeOnPreDrawListener(preDrawListener)
        sheet?.animate()?.cancel()
        foldJob?.cancel()
        foldJob = null
        super.onDetachedFromWindow()
    }
}
