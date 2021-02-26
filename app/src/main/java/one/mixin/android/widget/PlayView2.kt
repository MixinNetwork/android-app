package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.ColorInt

class PlayView2 @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        val DEFAULT_BG = Color.parseColor("#B2212121")

        const val STATUS_IDLE = 0
        const val STATUS_PLAYING = 1
    }

    var status = STATUS_IDLE
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }

    private var bg = DEFAULT_BG

    private val bgPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bg
        }
    }

    private val playDrawable: PlayPauseDrawable = PlayPauseDrawable().apply {
        callback = this@PlayView2
    }

    init {
        setWillNotDraw(false)
    }

    fun setColor(@ColorInt bgColor: Int?, @ColorInt drawableColor: Int?) {
        bgColor?.let { bgPaint.color = it }
        drawableColor?.let { playDrawable.color = it }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        playDrawable.setBounds(0, 0, w, h)
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who == playDrawable || super.verifyDrawable(who)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height
        canvas.drawCircle(w / 2f, h / 2f, w / 2f, bgPaint)
        when (status) {
            STATUS_IDLE -> {
                playDrawable.isPlay = true
            }
            STATUS_PLAYING -> {
                playDrawable.isPlay = false
            }
        }
        playDrawable.draw(canvas)
    }
}
