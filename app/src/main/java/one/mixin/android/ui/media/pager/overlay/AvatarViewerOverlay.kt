package one.mixin.android.ui.media.pager.overlay

import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import one.mixin.android.databinding.OverlayAvatarViewerBinding
import one.mixin.android.extension.loadImage
import kotlin.math.min

/**
 * Telegram X-style non-Activity overlay for zooming user/group avatars directly above
 * the current page or BottomSheetDialogFragment, morphing seamlessly from/to the circular avatar.
 */
class AvatarViewerOverlay private constructor(
    private val activity: FragmentActivity,
    private val avatarUrl: String,
    private val sourceView: View?,
) {
    private val binding = OverlayAvatarViewerBinding.inflate(LayoutInflater.from(activity))
    private val dialog =
        MediaOverlayDialog(
            hostActivity = activity,
            keepScreenOn = false,
            hideFloatingViews = false,
            onBackRequest = { dismissWithAnimation(0f) },
        )

    private var initialThumb: MediaThumbLocation? = null
    private var isClosing = false
    private var hasDismissed = false
    private var chromeVisible = true
    private var chromeVisibleBeforeDrag = true
    private var topBarInsetTop = 0

    fun show() {
        initialThumb =
            sourceView?.let { v ->
                MediaThumbLocation.fromView(
                    view = v,
                    scaleType = ImageView.ScaleType.CENTER_CROP,
                )
            }

        setupInsets()
        setupViews()
        val thumb = initialThumb
        binding.backgroundView.alpha = 0f
        binding.topBar.alpha = 0f
        if (thumb != null && thumb.drawable != null) {
            binding.dismissLayout.visibility = View.INVISIBLE
        }
        dialog.setContentView(binding.root)
        dialog.show()

        if (thumb != null && thumb.drawable != null) {
            binding.transitionView.doOnLayout {
                if (isClosing) return@doOnLayout
                if (!binding.transitionView.prepareOpen(thumb)) {
                    binding.backgroundView.alpha = 1f
                    dialog.syncSystemBarAppearance(1f)
                    binding.topBar.alpha = if (chromeVisible) 1f else 0f
                    binding.dismissLayout.visibility = View.VISIBLE
                    binding.transitionView.clearAndHide()
                    return@doOnLayout
                }
                binding.transitionView.doOnPreDraw {
                    binding.transitionView.post {
                        if (isClosing) return@post
                        thumb.setSourceVisible(false)
                        binding.transitionView.startPreparedOpen(
                            onProgress = { t ->
                                binding.backgroundView.alpha = t
                                dialog.syncSystemBarAppearance(t)
                                binding.topBar.alpha = t
                            },
                            onEnd = {
                                binding.backgroundView.alpha = 1f
                                dialog.syncSystemBarAppearance(1f)
                                binding.topBar.alpha = if (chromeVisible) 1f else 0f
                                binding.dismissLayout.visibility = View.VISIBLE
                                binding.dismissLayout.doOnPreDraw {
                                    binding.dismissLayout.post {
                                        if (!isClosing) {
                                            binding.transitionView.clearAndHide()
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        } else {
            binding.backgroundView.animate()
                .alpha(1f)
                .setDuration(200L)
                .setUpdateListener { anim ->
                    dialog.syncSystemBarAppearance(anim.animatedFraction)
                }
                .withEndAction {
                    dialog.syncSystemBarAppearance(1f)
                }
                .start()
            binding.topBar.animate().alpha(1f).setDuration(200L).start()
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars =
                insets.getInsetsIgnoringVisibility(
                    WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout(),
                )
            val navBars = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
            topBarInsetTop = bars.top
            binding.topBar.setPadding(
                maxOf(bars.left, navBars.left),
                topBarInsetTop,
                maxOf(bars.right, navBars.right),
                0,
            )
            insets
        }
    }

    private fun setupViews() {
        binding.backIv.setOnClickListener {
            dismissWithAnimation(0f)
        }

        val photoView = binding.avatarPhotoView
        photoView.setZoomable(true)
        initialThumb?.drawable?.let { placeholder ->
            photoView.setImageDrawable(placeholder)
        }
        photoView.loadImage(
            avatarUrl,
            onSuccess = { _, _ ->
                photoView.attacher.isZoomable = true
            },
        )
        photoView.setOnClickListener {
            toggleChrome()
        }
        photoView.setOnOutsidePhotoTapListener {
            toggleChrome()
        }

        binding.dismissLayout.dismissListener =
            object : MediaDismissLayout.OnDismissListener {
                override fun onDragStart() {
                    chromeVisibleBeforeDrag = chromeVisible
                    if (chromeVisible) {
                        binding.topBar.animate().cancel()
                        binding.topBar.animate().alpha(0f).setDuration(120L).start()
                    }
                }

                override fun onDragProgress(progress: Float) {
                    val bgAlpha = (1f - progress).coerceIn(0f, 1f)
                    binding.backgroundView.alpha = bgAlpha
                    dialog.syncSystemBarAppearance(bgAlpha)
                }

                override fun onDismiss(velocityY: Float) {
                    dismissWithAnimation(velocityY)
                }

                override fun onDragCancel() {
                    binding.backgroundView.alpha = 1f
                    dialog.syncSystemBarAppearance(1f)
                    if (chromeVisibleBeforeDrag) {
                        binding.topBar.animate().alpha(1f).setDuration(150L).start()
                    }
                }
            }
    }

    private fun toggleChrome() {
        if (isClosing || binding.transitionView.isAnimating) return
        chromeVisible = !chromeVisible
        binding.topBar.animate().cancel()
        if (chromeVisible) {
            binding.topBar.isVisible = true
            binding.topBar.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(CHROME_ANIM_DURATION_MS)
                .setInterpolator(CHROME_INTERPOLATOR)
                .start()
        } else {
            val shift = -(binding.topBar.height.takeIf { it > 0 } ?: 120).toFloat() * 0.25f
            binding.topBar.animate()
                .alpha(0f)
                .translationY(shift)
                .setDuration(CHROME_ANIM_DURATION_MS)
                .setInterpolator(CHROME_INTERPOLATOR)
                .withEndAction {
                    if (!chromeVisible) {
                        binding.topBar.isVisible = false
                    }
                }
                .start()
        }
        dialog.setSystemBarsVisible(chromeVisible)
    }

    private fun dismissWithAnimation(velocityY: Float) {
        if (isClosing) return
        isClosing = true
        binding.dismissLayout.dismissEnabled = false
        binding.topBar.animate().cancel()
        val startTopBarAlpha = if (binding.topBar.isVisible) binding.topBar.alpha else 0f
        dialog.restoreSystemBarsForDismiss()

        val targetThumb =
            sourceView?.let { v ->
                MediaThumbLocation.fromView(
                    view = v,
                    scaleType = ImageView.ScaleType.CENTER_CROP,
                )
            }
        val currentRect = resolveCurrentAvatarScreenRect()
        val currentDrawable = extractDrawable(binding.avatarPhotoView.drawable) ?: targetThumb?.drawable
        val startBgAlpha = binding.backgroundView.alpha

        if (targetThumb != null && currentDrawable != null) {
            targetThumb.setSourceVisible(false)
            binding.transitionView.animateCloseToTarget(
                currentScreenRect = currentRect,
                currentDrawable = currentDrawable,
                targetLocation = targetThumb,
                velocityY = velocityY,
                onProgress = { t ->
                    val bgAlpha = startBgAlpha * (1f - t)
                    binding.backgroundView.alpha = bgAlpha
                    dialog.syncSystemBarAppearance(bgAlpha)
                    if (startTopBarAlpha > 0f) {
                        binding.topBar.alpha = startTopBarAlpha * (1f - (t * 2.5f).coerceIn(0f, 1f))
                    }
                },
                onEnd = {
                    completeCloseAfterSourceFrame(targetThumb)
                },
            )
            binding.dismissLayout.visibility = View.INVISIBLE
        } else if (currentDrawable != null) {
            binding.transitionView.animateFadeClose(
                currentScreenRect = currentRect,
                currentDrawable = currentDrawable,
                velocityY = velocityY,
                onProgress = { t ->
                    val bgAlpha = startBgAlpha * (1f - t)
                    binding.backgroundView.alpha = bgAlpha
                    dialog.syncSystemBarAppearance(bgAlpha)
                    if (startTopBarAlpha > 0f) {
                        binding.topBar.alpha = startTopBarAlpha * (1f - (t * 2.5f).coerceIn(0f, 1f))
                    }
                },
                onEnd = {
                    completeCloseAfterSourceFrame(targetThumb)
                },
            )
            binding.dismissLayout.visibility = View.INVISIBLE
        } else {
            restoreAllSourceVisibility(targetThumb)
            finishDismiss()
        }
    }

    private fun completeCloseAfterSourceFrame(targetThumb: MediaThumbLocation?) {
        binding.backgroundView.alpha = 0f
        binding.topBar.isVisible = false
        dialog.syncSystemBarAppearance(0f)
        restoreAllSourceVisibility(targetThumb)
        val hostRoot = sourceView?.rootView ?: activity.window?.decorView
        if (hostRoot == null || !hostRoot.isAttachedToWindow) {
            finishDismiss()
            return
        }
        val finishRunnable = Runnable { finishDismiss() }
        hostRoot.postDelayed(finishRunnable, 48L)
        hostRoot.doOnPreDraw {
            hostRoot.post {
                hostRoot.removeCallbacks(finishRunnable)
                finishDismiss()
            }
        }
        hostRoot.invalidate()
    }

    private fun resolveCurrentAvatarScreenRect(): RectF {
        val photoView = binding.avatarPhotoView
        val loc = IntArray(2)
        photoView.getLocationOnScreen(loc)
        val displayRect = photoView.displayRect
        val scale = photoView.scaleX
        if (displayRect != null && displayRect.width() > 0f && displayRect.height() > 0f) {
            return RectF(
                loc[0] + displayRect.left * scale,
                loc[1] + displayRect.top * scale,
                loc[0] + displayRect.right * scale,
                loc[1] + displayRect.bottom * scale,
            )
        }
        val size = min(photoView.width, photoView.height).toFloat() * scale
        val cx = loc[0] + photoView.width * scale / 2f
        val cy = loc[1] + photoView.height * scale / 2f
        return RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f)
    }

    private fun extractDrawable(drawable: Drawable?): Drawable? {
        drawable ?: return null
        return drawable.constantState?.newDrawable(activity.resources)?.mutate() ?: drawable
    }

    private fun restoreAllSourceVisibility(targetThumb: MediaThumbLocation?) {
        initialThumb?.setSourceVisible?.invoke(true)
        targetThumb?.setSourceVisible?.invoke(true)
    }

    private fun finishDismiss() {
        if (hasDismissed) return
        hasDismissed = true
        isClosing = true
        binding.transitionView.clearAndHide()
        initialThumb?.setSourceVisible?.invoke(true)
        currentOverlay = null
        dialog.dismiss()
    }

    companion object {
        private const val CHROME_ANIM_DURATION_MS = 180L
        private val CHROME_INTERPOLATOR = PathInterpolator(0.2f, 0.88f, 0.3f, 1f)

        @Volatile
        private var currentOverlay: AvatarViewerOverlay? = null

        fun show(
            activity: FragmentActivity,
            url: String,
            sourceView: View?,
        ) {
            if (activity.isFinishing || activity.isDestroyed) return
            currentOverlay?.finishDismiss()
            val overlay = AvatarViewerOverlay(activity, url, sourceView)
            currentOverlay = overlay
            overlay.show()
        }
    }
}
