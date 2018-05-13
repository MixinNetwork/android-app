package one.mixin.android.widget

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.ProgressEvent
import org.jetbrains.anko.sp

class CircleProgress @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private val bounds = RectF()
    private val fBounds = RectF()

    private var mObjectAnimatorAngle: ObjectAnimator? = null
    private val mPaint: Paint
    private val mTextPaint: Paint
    private val mBackgroundPaint: Paint
    private val mForkPath: Path
    private val mArrowPath: Path

    private var currentGlobalAngle: Float = 0f
        set(currentGlobalAngle) {
            field = currentGlobalAngle
            invalidate()
        }
    private val mBorderWidth: Int
    private var isRunning: Boolean = false

    private var mProgress = 0
    private var mMaxProgress = 100
    private var mTextSize = context.sp(12)
    private val mSize: Int
    private val mShadowColor: Int
    private val mProgressColor: Int
    private var mBindId: String? = null
    private var arcAngle = 10f
    private var mBorder = false

    private var status = STATUS_LOADING

    @Suppress("unused")
    val progress: Float
        get() = mProgress.toFloat()

    init {

        val density = context.resources.displayMetrics.density
        val a = context.obtainStyledAttributes(attrs, R.styleable.CircleProgress, defStyleAttr, 0)
        mSize = a.getDimensionPixelSize(R.styleable.CircleProgress_size, (density * 40).toInt())
        mBorderWidth = a.getDimensionPixelSize(R.styleable.CircleProgress_progressWidth,
            (DEFAULT_BORDER_WIDTH * density).toInt())
        mShadowColor = a.getColor(R.styleable.CircleProgress_shadowColor, 0x33000000)
        mProgressColor = a.getColor(R.styleable.CircleProgress_progressColor, Color.WHITE)
        mBorder = a.getBoolean(R.styleable.CircleProgress_border, false)
        mProgress = a.getInt(R.styleable.CircleProgress_progress, mProgress)
        mMaxProgress = a.getInt(R.styleable.CircleProgress_maxProgress, mMaxProgress)
        mTextSize = a.getInt(R.styleable.CircleProgress_stringSize, mTextSize)
        a.recycle()

        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeCap = Cap.ROUND
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeWidth = mBorderWidth.toFloat()
        mPaint.color = mProgressColor
        mBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mBackgroundPaint.color = mShadowColor

        mForkPath = Path()
        mArrowPath = Path()

        mTextPaint = Paint()
        mTextPaint.color = 0xFFACACAC.toInt()
        mTextPaint.textSize = mTextSize.toFloat()
        mTextPaint.textAlign = Paint.Align.CENTER
        mTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setupAnimations()
    }

    private fun start() {
        if (isRunning) {
            return
        }
        isRunning = true
        mObjectAnimatorAngle!!.start()
    }

    private fun stop() {
        if (!isRunning) {
            return
        }
        isRunning = false
        mObjectAnimatorAngle!!.cancel()
        arcAngle = 10f
        setProgress(0)
        invalidate()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (visibility == VISIBLE && !isRunning) {
            start()
        } else if (visibility == GONE && isRunning) {
            stop()
        }
    }

    private var disposable: Disposable? = null

    override fun onAttachedToWindow() {
        if (disposable == null) {
            disposable = RxBus.getInstance().toFlowable(ProgressEvent::class.java)
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.id == mBindId) {
                        if (status == STATUS_LOADING) {
                            val progress = (it.progress * mMaxProgress).toInt().let {
                                if (it >= mMaxProgress) {
                                    (mMaxProgress * 0.95).toInt()
                                } else {
                                    it
                                }
                            }
                            setProgress(progress)
                        }
                    }
                }
        }
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        disposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val centerX = w / 2
        val centerY = h / 2
        bounds.set((centerX - mSize / 2).toFloat(), (centerY - mSize / 2).toFloat(),
            (centerX + mSize / 2).toFloat(), (centerY + mSize / 2).toFloat())
        fBounds.left = bounds.left + mBorderWidth * 2
        fBounds.right = bounds.right - mBorderWidth * 2
        fBounds.top = bounds.top + mBorderWidth * 2
        fBounds.bottom = bounds.bottom - mBorderWidth * 2
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (status == STATUS_DONE) {
            mBackgroundPaint.color = 0xFFEDEEEE.toInt()
        } else {
            mBackgroundPaint.color = mShadowColor
        }
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2, mBackgroundPaint)
        when (status) {
            STATUS_DOWNLOAD -> {
                drawDownArrow(canvas)
                if (mBorder) {
                    canvas.drawArc(fBounds, 0f, 360f, false, mPaint)
                }
            }
            STATUS_UPLOAD -> {
                drawUpArrow(canvas)
                if (mBorder) {
                    canvas.drawArc(fBounds, 0f, 360f, false, mPaint)
                }
            }
            STATUS_DONE -> {
                drawDone(canvas)
            }
            else -> {
                when {
                    mProgress < 100 -> {
                        drawFork(canvas)
                        drawArc(canvas)
                    }
                    else -> {
                        drawFork(canvas)
                        canvas.drawArc(fBounds, 0f, 360f, false, mPaint)
                    }
                }
            }
        }
    }

    private fun drawFork(canvas: Canvas) {
        mForkPath.reset()
        mForkPath.moveTo(fBounds.centerX() + fBounds.width() * 0.15f, fBounds.centerY() - fBounds.height() * 0.15f)
        mForkPath.lineTo(fBounds.centerX() - fBounds.width() * 0.15f, fBounds.centerY() + fBounds.height() * 0.15f)
        mForkPath.moveTo(fBounds.centerX() - fBounds.width() * 0.15f, fBounds.centerY() - fBounds.height() * 0.15f)
        mForkPath.lineTo(fBounds.centerX() + fBounds.width() * 0.15f, fBounds.centerY() + fBounds.height() * 0.15f)
        canvas.drawPath(mForkPath, mPaint)
    }

    private fun drawDownArrow(canvas: Canvas) {
        mForkPath.reset()
        mForkPath.moveTo(fBounds.centerX(), fBounds.centerY() - fBounds.height() * 0.25f)
        mForkPath.lineTo(fBounds.centerX(), fBounds.centerY() + fBounds.height() * 0.25f)
        mForkPath.lineTo(fBounds.centerX() + fBounds.width() * 0.20f, fBounds.centerY())
        mForkPath.moveTo(fBounds.centerX(), fBounds.centerY() + fBounds.height() * 0.25f)
        mForkPath.lineTo(fBounds.centerX() - fBounds.width() * 0.20f, fBounds.centerY())
        canvas.drawPath(mForkPath, mPaint)
    }

    private fun drawUpArrow(canvas: Canvas) {
        mForkPath.reset()
        mForkPath.moveTo(fBounds.centerX(), fBounds.centerY() + fBounds.height() * 0.25f)
        mForkPath.lineTo(fBounds.centerX(), fBounds.centerY() - fBounds.height() * 0.25f)
        mForkPath.lineTo(fBounds.centerX() + fBounds.width() * 0.20f, fBounds.centerY())
        mForkPath.moveTo(fBounds.centerX(), fBounds.centerY() - fBounds.height() * 0.25f)
        mForkPath.lineTo(fBounds.centerX() - fBounds.width() * 0.20f, fBounds.centerY())
        canvas.drawPath(mForkPath, mPaint)
    }

    private fun drawDone(canvas: Canvas) {
        val fontMetrics = mTextPaint.fontMetricsInt
        val baseline = (bounds.bottom + bounds.top - fontMetrics.bottom - fontMetrics.top) / 2
        canvas.drawText("FILE", bounds.centerX(), baseline, mTextPaint)
    }

    private fun drawArc(canvas: Canvas) {
        val startAngle = currentGlobalAngle
        val endAngle = (350 * mProgress / mMaxProgress + 10)
        when {
            endAngle > arcAngle + 100 -> arcAngle += 5
            endAngle > arcAngle + 50 -> arcAngle += 2
            endAngle > arcAngle + 10 -> arcAngle += 1
            else -> arcAngle = endAngle.toFloat()
        }
        canvas.drawArc(fBounds, startAngle, arcAngle, false, mPaint)
    }

    private fun setupAnimations() {
        mObjectAnimatorAngle = ObjectAnimator.ofFloat(this, "CurrentGlobalAngle", 0f, 360f)
        mObjectAnimatorAngle!!.interpolator = ANGLE_INTERPOLATOR
        mObjectAnimatorAngle!!.duration = ANGLE_ANIMATOR_DURATION.toLong()
        mObjectAnimatorAngle!!.repeatMode = ValueAnimator.RESTART
        mObjectAnimatorAngle!!.repeatCount = ValueAnimator.INFINITE
    }

    fun setProgress(progress: Int) {
        mProgress = if (progress >= mMaxProgress) {
            mMaxProgress
        } else {
            progress
        }
    }

    fun setBindId(id: String?) {
        if (id != mBindId) {
            mBindId = id
            setProgress(0)
        }
    }

    @Suppress("unused")
    fun getBindId(): String? {
        return mBindId
    }

    private fun setStatus(status: Int) {
        this.status = status
        if (status == STATUS_UPLOAD || status == STATUS_DOWNLOAD) {
            stop()
        } else {
            start()
        }
    }

    fun enableLoading() {
        if (status != STATUS_LOADING) {
            setProgress(0)
            setStatus(STATUS_LOADING)
        }
    }

    fun enableDownload() {
        setStatus(STATUS_DOWNLOAD)
    }

    fun enableUpload() {
        setStatus(STATUS_UPLOAD)
    }

    fun setDone() {
        setStatus(STATUS_DONE)
    }

    companion object {
        private val ANGLE_INTERPOLATOR = LinearInterpolator()
        private val ANGLE_ANIMATOR_DURATION = 3000
        private val DEFAULT_BORDER_WIDTH = 3
        private val STATUS_LOADING = 0
        private val STATUS_UPLOAD = 1
        private val STATUS_DOWNLOAD = 2
        private val STATUS_DONE = 3
    }
}
