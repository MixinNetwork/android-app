package one.mixin.android.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.annotation.RequiresApi

class BottomNavigationBackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var source: ViewGroup? = null

    // Blur in a child node so the view's outline clip applies to the blurred result.
    // A RenderEffect set on the view itself also blurs the clipped edge on the Skia GL pipeline.
    @get:RequiresApi(Build.VERSION_CODES.S)
    @set:RequiresApi(Build.VERSION_CODES.S)
    var blurEffect: RenderEffect? = null
        set(value) {
            field = value
            invalidate()
        }

    private var blurNode: RenderNode? = null

    init {
        // The glass background is translucent, so its own outline casts no shadow. The blurred content is opaque.
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                view.background?.getOutline(outline) ?: outline.setEmpty()
                outline.alpha = 1f
            }
        }
    }

    private val sourceLocation = IntArray(2)
    private val backdropLocation = IntArray(2)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val source = source ?: return
        source.getLocationInWindow(sourceLocation)
        getLocationInWindow(backdropLocation)
        val dx = (sourceLocation[0] - backdropLocation[0]).toFloat()
        val dy = (sourceLocation[1] - backdropLocation[1]).toFloat()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && canvas.isHardwareAccelerated) {
            val effect = blurEffect
            if (effect != null) {
                val node = blurNode ?: RenderNode("BottomNavigationBackdrop").also { blurNode = it }
                node.setPosition(0, 0, width, height)
                node.setRenderEffect(effect)
                val recordingCanvas = node.beginRecording()
                recordingCanvas.translate(dx, dy)
                source.draw(recordingCanvas)
                node.endRecording()
                canvas.drawRenderNode(node)
                return
            }
        }
        val saveCount = canvas.save()
        canvas.translate(dx, dy)
        source.draw(canvas)
        canvas.restoreToCount(saveCount)
    }
}
