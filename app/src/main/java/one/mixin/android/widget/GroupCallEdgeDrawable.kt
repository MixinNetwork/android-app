package one.mixin.android.widget

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import one.mixin.android.extension.dp
import kotlin.math.min

class GroupCallEdgeDrawable(private val banner: View) : Drawable() {
    private val bannerBackground = banner.background.mutate() as GradientDrawable
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = requireNotNull(bannerBackground.color).defaultColor
    }
    private val bridge = Path()
    private val body = Path()
    private val bodyBounds = RectF()
    private val radii = FloatArray(8)
    private val attachmentDistance = 48.dp.toFloat()

    fun update() {
        val container = banner.parent as? View ?: return
        if (!banner.isLaidOut) return
        setBounds(0, 0, container.width, container.height)
        val maxX = (container.width - banner.width).coerceAtLeast(0).toFloat()
        val left = banner.x <= maxX / 2f
        val distance = (if (left) banner.x else maxX - banner.x).coerceAtLeast(0f)
        val progress = (distance / attachmentDistance).coerceIn(0f, 1f)
        val radius = banner.height / 2f
        val attachedRadius = radius * progress
        val leftRadius = if (left) attachedRadius else radius
        val rightRadius = if (left) radius else attachedRadius
        radii[0] = leftRadius
        radii[1] = leftRadius
        radii[2] = rightRadius
        radii[3] = rightRadius
        radii[4] = rightRadius
        radii[5] = rightRadius
        radii[6] = leftRadius
        radii[7] = leftRadius
        bannerBackground.cornerRadii = radii
        banner.invalidateOutline()

        bridge.rewind()
        if (distance > 0f && progress < 1f) {
            val edgeX = if (left) 0f else container.width.toFloat()
            val direction = if (left) 1f else -1f
            val joinX = edgeX + direction * (distance + attachedRadius)
            val centerY = banner.y + radius
            val anchorHalf = radius * (1f - progress)
            val bodyHalf = radius * (1f - progress * 0.5f)
            val neckHalf = anchorHalf * (1f - progress) * 0.5f
            val controlX = edgeX + (joinX - edgeX) * 0.5f
            bridge.moveTo(edgeX, centerY - anchorHalf)
            bridge.cubicTo(controlX, centerY - neckHalf, controlX, centerY - neckHalf, joinX, centerY - bodyHalf)
            bridge.lineTo(joinX, centerY + bodyHalf)
            bridge.cubicTo(controlX, centerY + neckHalf, controlX, centerY + neckHalf, edgeX, centerY + anchorHalf)
            bridge.close()

            body.rewind()
            bodyBounds.set(banner.x, banner.y, banner.x + banner.width, banner.y + banner.height)
            body.addRoundRect(bodyBounds, radii, Path.Direction.CW)
            // The overlay must not cover the banner's icon or text.
            bridge.op(body, Path.Op.DIFFERENCE)
            paint.alpha = (255 * min(1f, (1f - progress) * 4f)).toInt()
        }
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        if (banner.isShown) canvas.drawPath(bridge, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
