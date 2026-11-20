package one.mixin.android.ui.media.pager.overlay

import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.widget.AvatarView

data class MediaThumbLocation(
    val screenRect: RectF,
    val clipRect: RectF,
    val cornerRadii: FloatArray,
    val drawable: Drawable?,
    val contentWidth: Int = 0,
    val contentHeight: Int = 0,
    val scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER,
    val setSourceVisible: (Boolean) -> Unit = {},
) {
    fun isEmpty(): Boolean =
        screenRect.width() <= 0f || screenRect.height() <= 0f ||
            clipRect.width() <= 0f || clipRect.height() <= 0f

    companion object {
        val ZERO_RADII = FloatArray(8) { 0f }

        fun uniformRadii(radius: Float): FloatArray = FloatArray(8) { radius }

        fun topRadii(radius: Float): FloatArray =
            floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)

        fun fromView(
            view: View,
            clipHost: View? = null,
            cornerRadii: FloatArray = ZERO_RADII,
            contentWidth: Int = 0,
            contentHeight: Int = 0,
            scaleType: ImageView.ScaleType? = null,
            customDrawable: Drawable? = null,
            setSourceVisible: ((Boolean) -> Unit)? = null,
        ): MediaThumbLocation? {
            if (!view.isAttachedToWindow || view.width <= 0 || view.height <= 0 || !view.isShown) {
                return null
            }
            val loc = IntArray(2)
            view.getLocationOnScreen(loc)
            val screenRect = RectF(
                loc[0].toFloat(),
                loc[1].toFloat(),
                (loc[0] + view.width).toFloat(),
                (loc[1] + view.height).toFloat(),
            )
            val clipRect = computeClipRect(screenRect, clipHost) ?: return null
            val imageView = resolveImageView(view)
            val resolvedDrawable = customDrawable ?: extractDrawable(imageView)
            val resolvedScaleType =
                scaleType ?: imageView?.scaleType ?: ImageView.ScaleType.FIT_CENTER
            val resolvedRadii =
                if (view is AvatarView && cornerRadii.contentEquals(ZERO_RADII)) {
                    uniformRadii(minOf(screenRect.width(), screenRect.height()) / 2f)
                } else {
                    cornerRadii
                }
            val visibilityCallback = setSourceVisible ?: { visible ->
                view.alpha = if (visible) 1f else 0f
            }
            return MediaThumbLocation(
                screenRect = screenRect,
                clipRect = clipRect,
                cornerRadii = resolvedRadii,
                drawable = resolvedDrawable,
                contentWidth = contentWidth.takeIf { it > 0 } ?: (resolvedDrawable?.intrinsicWidth ?: 0),
                contentHeight = contentHeight.takeIf { it > 0 } ?: (resolvedDrawable?.intrinsicHeight ?: 0),
                scaleType = resolvedScaleType,
                setSourceVisible = visibilityCallback,
            )
        }

        fun fromScreenBounds(
            screenRect: RectF,
            clipHost: View? = null,
            cornerRadii: FloatArray = ZERO_RADII,
            drawable: Drawable? = null,
            contentWidth: Int = 0,
            contentHeight: Int = 0,
            scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP,
            setSourceVisible: (Boolean) -> Unit = {},
        ): MediaThumbLocation? {
            if (screenRect.width() <= 0f || screenRect.height() <= 0f) return null
            val clipRect = computeClipRect(screenRect, clipHost) ?: return null
            return MediaThumbLocation(
                screenRect = RectF(screenRect),
                clipRect = clipRect,
                cornerRadii = cornerRadii,
                drawable = drawable,
                contentWidth = contentWidth.takeIf { it > 0 } ?: (drawable?.intrinsicWidth ?: 0),
                contentHeight = contentHeight.takeIf { it > 0 } ?: (drawable?.intrinsicHeight ?: 0),
                scaleType = scaleType,
                setSourceVisible = setSourceVisible,
            )
        }

        private fun computeClipRect(screenRect: RectF, clipHost: View?): RectF? {
            if (clipHost == null || !clipHost.isAttachedToWindow) {
                return RectF(screenRect)
            }
            val hostLoc = IntArray(2)
            clipHost.getLocationOnScreen(hostLoc)
            val hostVisible = Rect()
            val visible = clipHost.getLocalVisibleRect(hostVisible)
            val hostScreenRect =
                if (visible) {
                    RectF(
                        (hostLoc[0] + hostVisible.left).toFloat(),
                        (hostLoc[1] + hostVisible.top).toFloat(),
                        (hostLoc[0] + hostVisible.right).toFloat(),
                        (hostLoc[1] + hostVisible.bottom).toFloat(),
                    )
                } else {
                    RectF(
                        hostLoc[0].toFloat(),
                        hostLoc[1].toFloat(),
                        (hostLoc[0] + clipHost.width).toFloat(),
                        (hostLoc[1] + clipHost.height).toFloat(),
                    )
                }
            val clipped = RectF(screenRect)
            if (!clipped.intersect(hostScreenRect)) {
                return null
            }
            if (clipped.width() < 2f || clipped.height() < 2f) {
                return null
            }
            return clipped
        }

        private fun resolveImageView(view: View): ImageView? =
            when (view) {
                is ImageView -> view
                is AvatarView -> view.avatarSimple
                else -> null
            }

        private fun extractDrawable(imageView: ImageView?): Drawable? {
            val d = imageView?.drawable ?: return null
            return d.constantState?.newDrawable(imageView.resources)?.mutate() ?: d
        }
    }
}

fun interface MediaThumbProvider {
    fun findThumbLocation(messageId: String): MediaThumbLocation?
}

interface MediaThumbViewHolder {
    val thumbMessageId: String?

    fun createThumbLocation(clipHost: View?): MediaThumbLocation?
}

fun RecyclerView.createMediaThumbProvider(): MediaThumbProvider =
    MediaThumbProvider { messageId ->
        for (i in 0 until childCount) {
            val holder = getChildViewHolder(getChildAt(i))
            if (holder is MediaThumbViewHolder && holder.thumbMessageId == messageId) {
                return@MediaThumbProvider holder.createThumbLocation(this)
            }
        }
        null
    }
