package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import one.mixin.android.R

class PlayButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    FrameLayout(context, attrs, defStyleAttr) {
    companion object {
        const val DEFAULT_COLOR = Color.WHITE
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
    private var color = DEFAULT_COLOR
    private val playDrawable = PlayPauseDrawable().apply {
        callback = this@PlayButton
        setFirstTimeNotAnimated(true)
    }

    init {
        setWillNotDraw(false)
        val ta = context.obtainStyledAttributes(attrs, R.styleable.PlayButton, defStyleAttr, 0)
        ta.let {
            if (ta.hasValue(R.styleable.PlayButton_play_color)) {
                color = ta.getColor(R.styleable.PlayButton_play_color, DEFAULT_COLOR)
            }
            ta.recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        playDrawable.setBounds(0, 0, w, h)
        playDrawable.setSize((2.2f * w).toInt())
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who == playDrawable || super.verifyDrawable(who)
    }

    override fun onDraw(canvas: Canvas) {
        when (status) {
            STATUS_IDLE -> {
                playDrawable.setPause(false)
                playDrawable.draw(canvas)
            }
            STATUS_PLAYING -> {
                playDrawable.setPause(true)
                playDrawable.draw(canvas)
            }
        }
    }
}
