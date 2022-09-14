package one.mixin.android.widget

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import one.mixin.android.R
import one.mixin.android.extension.dp

class ScanView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    init {
        doOnPreDraw {
            animator = ObjectAnimator.ofFloat(this, "offset", 0f, measuredHeight-padding)
                .setDuration(1500L)
                .apply {
                    this.repeatMode = ValueAnimator.RESTART
                    this.repeatCount = ValueAnimator.INFINITE
                }
            animator.start()
        }
    }

    private val paint = Paint()
    private var offset = 0f

    private fun getBitmap(context: Context, @DrawableRes drawableId: Int): Bitmap? {
        return when (val drawable = ContextCompat.getDrawable(context, drawableId)) {
            is BitmapDrawable -> {
                BitmapFactory.decodeResource(context.resources, drawableId)
            }
            is VectorDrawable -> {
                getBitmap(drawable)
            }
            else -> {
                throw IllegalArgumentException("unsupported drawable type")
            }
        }
    }

    private fun getBitmap(vectorDrawable: VectorDrawable): Bitmap? {
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }

    private lateinit var animator: Animator
    private var path: Path = Path()
    private val grid by lazy {
        getBitmap(context, R.drawable.scan_grid)
    }

    private val frame by lazy {
        getBitmap(context, R.drawable.scan_frame)
    }

    private val padding = 3.dp.toFloat()
    private val cornerRadius = 32.dp.toFloat()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        path.reset()
        path.addRoundRect(
            0f,
            0f,
            w.toFloat(),
            h.toFloat(),
            cornerRadius,
            cornerRadius,
            Path.Direction.CW
        )
        path.close()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val save = canvas.save()
        canvas.clipPath(path)
        paint.shader = null
        grid?.run {
            canvas.drawBitmap(this, padding, offset - this.height, paint)
        }

        paint.shader = LinearGradient(
            0f, offset + measuredHeight / 4f,
            0f, offset,
            Color.TRANSPARENT, Color.argb(153, 170, 255, 224),
            Shader.TileMode.MIRROR
        )
        canvas.drawRect(padding, offset - measuredHeight / 4, measuredWidth.toFloat()-padding, offset, paint)
        canvas.restoreToCount(save)
        frame?.run {
            canvas.drawBitmap(this, 0f, 0f, paint)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (::animator.isInitialized) {
            animator.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (::animator.isInitialized) {
            animator.pause()
        }
    }

    @Keep
    fun setOffset(offset: Float) {
        this.offset = offset
        invalidate()
    }

    @Keep
    fun getOffset() = this.offset
}