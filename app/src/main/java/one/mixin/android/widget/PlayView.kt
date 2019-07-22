package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.ProgressBar
import one.mixin.android.R

class PlayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_COLOR = Color.WHITE
        val DEFAULT_BG = Color.parseColor("#33000000")

        const val STATUS_IDLE = 0
        const val STATUS_LOADING = 1
        const val STATUS_PLAYING = 2
        const val STATUS_BUFFERING = 3
        const val STATUS_PAUSING = 4
    }

    var status = STATUS_IDLE
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }

    private var bg = DEFAULT_BG
    private var color = DEFAULT_COLOR

    private val bgPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bg
        }
    }

    private val pb: ProgressBar by lazy {
        val view = View.inflate(context, R.layout.view_progress_bar_white, null) as ProgressBar
        addView(view, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        view
    }

    private val drawable: PlayPauseDrawable = PlayPauseDrawable().apply {
        callback = this@PlayView
    }

    init {
        setWillNotDraw(false)
        val ta = context.obtainStyledAttributes(attrs, R.styleable.PlayView, defStyleAttr, 0)
        ta?.let {
            if (ta.hasValue(R.styleable.PlayView_bg)) {
                bg = ta.getColor(R.styleable.PlayView_bg, DEFAULT_BG)
            }
            if (ta.hasValue(R.styleable.PlayView_color)) {
                color = ta.getColor(R.styleable.PlayView_color, DEFAULT_COLOR)
            }

            ta.recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        drawable.setBounds(0, 0, w, h)
    }

    override fun verifyDrawable(who: Drawable?): Boolean {
        return who == drawable || super.verifyDrawable(who)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height
        when (status) {
            STATUS_IDLE -> {
                drawable.isPlay = true
            }
            STATUS_LOADING -> {
                drawable.isPlay = false
                pb.visibility = VISIBLE
            }
            STATUS_PLAYING -> {
                drawable.isPlay = false
                pb.visibility = GONE
            }
            STATUS_BUFFERING -> {
                drawable.isPlay = true
                pb.visibility = View.VISIBLE
            }
            STATUS_PAUSING -> {
                drawable.isPlay = true
                pb.visibility = GONE
            }
        }
        canvas.drawCircle(w / 2f, h / 2f, w / 2f, bgPaint)
        drawable.draw(canvas)
    }
}
