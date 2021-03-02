package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout

class PlayView2 @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val STATUS_IDLE = 0
        const val STATUS_PLAYING = 1
    }

    var status = STATUS_IDLE
        set(value) {
            if (value != field) {
                field = value

                when (value) {
                    STATUS_IDLE -> {
                        playPauseDrawable.setPause(false)
                    }
                    STATUS_PLAYING -> {
                        playPauseDrawable.setPause(true)
                    }
                }
                invalidate()
            }
        }

    var playPauseDrawable = PlayPauseDrawable().apply {
        callback = this@PlayView2
    }

    init {
        setWillNotDraw(false)
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who == playPauseDrawable || super.verifyDrawable(who)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        playPauseDrawable.setBounds(0, 0, w, h)
        playPauseDrawable.setSize(w)
    }

    override fun onDraw(canvas: Canvas) {
        playPauseDrawable.draw(canvas)
    }
}
