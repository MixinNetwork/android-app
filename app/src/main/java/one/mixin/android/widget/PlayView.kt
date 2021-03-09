package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.view.updateLayoutParams
import one.mixin.android.R

class PlayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_COLOR = Color.WHITE
        val DEFAULT_BG = Color.parseColor("#B2212121")

        const val STATUS_IDLE = 0
        const val STATUS_LOADING = 1
        const val STATUS_PLAYING = 2
        const val STATUS_REFRESH = 3
        const val STATUS_PAUSE = 4
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
        addView(view)
        view
    }

    private val playDrawable = PlayPauseDrawable().apply {
        callback = this@PlayView
        setFirstTimeNotAnimated(true)
    }

    private val refreshDrawable: Drawable by lazy {
        resources.getDrawable(R.drawable.ic_refresh, context.theme).apply {
            callback = this@PlayView
        }
    }

    init {
        setWillNotDraw(false)
        val ta = context.obtainStyledAttributes(attrs, R.styleable.PlayView, defStyleAttr, 0)
        ta.let {
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
        playDrawable.setBounds(0, 0, w, h)
        playDrawable.setSize(w)
        refreshDrawable.setBounds(0, 0, w, h)
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who == playDrawable || super.verifyDrawable(who) || who == refreshDrawable
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        pb.updateLayoutParams<LayoutParams> {
            width = measuredWidth / 2
            height = measuredHeight / 2
            gravity = Gravity.CENTER
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height
        canvas.drawCircle(w / 2f, h / 2f, w / 2f, bgPaint)
        when (status) {
            STATUS_IDLE -> {
                playDrawable.setPause(false)
                pb.visibility = GONE
                playDrawable.draw(canvas)
            }
            STATUS_LOADING -> {
                pb.visibility = VISIBLE
            }
            STATUS_PAUSE -> {
                playDrawable.setPause(false)
                pb.visibility = GONE
                playDrawable.draw(canvas)
            }
            STATUS_PLAYING -> {
                playDrawable.setPause(true)
                pb.visibility = GONE
                playDrawable.draw(canvas)
            }
            STATUS_REFRESH -> {
                pb.visibility = GONE
                refreshDrawable.draw(canvas)
            }
        }
    }
}
