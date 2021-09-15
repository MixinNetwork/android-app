package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.ProgressEvent
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PLAY

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

    private var disposable: Disposable? = null
    private var mBindId: String? = null
    fun setBind(id: String?) {
        if (id != mBindId) {
            mBindId = id
        }
    }

    override fun onAttachedToWindow() {
        if (disposable == null) {
            disposable = RxBus.listen(ProgressEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    status = if (it.id == mBindId && it.status == STATUS_PLAY) {
                        STATUS_PLAYING
                    } else {
                        STATUS_IDLE
                    }
                }
        }
        super.onAttachedToWindow()
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
